package com.dusk.module.auth.service;

import cn.hutool.core.text.CharSequenceUtil;
import com.dusk.common.core.annotation.DisableGlobalFilter;
import com.dusk.common.core.constant.EntityConstant;
import com.dusk.common.core.constant.TreeConstant;
import com.dusk.common.core.dto.TreeCreateOrUpdateInputDto;
import com.dusk.common.core.dto.TreeFullPathNameDto;
import com.dusk.common.core.dto.TreeMoveInputDto;
import com.dusk.common.core.dto.TreePagedAndSortedInputDto;
import com.dusk.common.core.entity.CreationEntity;
import com.dusk.common.core.entity.FullAuditedEntity;
import com.dusk.common.core.entity.TreeEntity;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.repository.IBaseRepository;
import com.dusk.common.core.service.ITreeService;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.common.core.tenant.TenantContextHolder;
import com.dusk.module.auth.enums.EnumResetType;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2021-04-22 9:40
 */
@Slf4j
public class TreeService<T extends TreeEntity, K extends IBaseRepository<T>> extends BaseService<T, K> implements ITreeService<T, K> {
    @DubboReference
    protected ISerialNoRpcService serialNoRpcService;
    
    private final String noFoundRecoredMsgTemplate = "未找到相应的记录：id=";

    @Override
    @Transactional
    public T createOrUpdate(TreeCreateOrUpdateInputDto input) {
        T entity;
        if (input.getId() == null) {
            entity = BeanUtils.instantiateClass(getEntityClass());
            BeanUtils.copyProperties(input, entity);
        } else {
            entity = findById(input.getId()).orElseThrow(() -> new BusinessException(noFoundRecoredMsgTemplate + input.getId()));
            BeanUtils.copyProperties(input, entity);
        }
        save(entity);
        return entity;
    }

    @Override
    public <S extends T> S save(S s) {
        if (isCreate(s)) {
            s.setSerialNo(getSerialNo());
            if (s.getParentId() != null) {
                T parent = findById(s.getParentId()).orElseThrow(() -> new BusinessException("未找到父节点：parentId=" + s.getParentId()));
                s.setPath(parent.getPath() + TreeConstant.PATH_DELIMITER + s.getSerialNo());
            } else {
                s.setPath(s.getSerialNo());
            }
        } else {//update
            if (s.getId().equals(s.getParentId())) {
                throw new BusinessException("不能以自身为父节点！");
            }
            updateDescendantsPath(s);
        }
        return super.save(s);
    }

    @Override
    public <S extends T> List<S> saveAll(Iterable<S> iterable) {
        List<T> newList = new ArrayList<>();
        List<T> updateList = new ArrayList<>();

        iterable.forEach(e -> {
            //序列号为空， 认为是新增
            if (CharSequenceUtil.isBlank(e.getSerialNo())) {
                newList.add(e);
            } else {
                updateList.add(e);
            }
        });

        //1.先保存新增的
        //填充序列号
        genSerialNos(newList);
        //保存, 填充id
        super.saveAll(newList);
        //2. 更新path
        List<T> allRelativeEntities = new ArrayList<>();
        allRelativeEntities.addAll(newList);
        allRelativeEntities.addAll(updateList);
        addParents(allRelativeEntities);
        iterable.forEach(e -> e.setPath(getPath(e, allRelativeEntities)));
        List<S> result = super.saveAll(iterable);
        //更新所有节点的路径
        updateAllNodesPath();

        return result;
    }

    @Override
    public void batchSave(List<T> data, int batchSize) {
        //批量保存暂不支持
        throw new BusinessException("暂不支持该方法！");
        //        //todo:
        //        super.batchSave(data, batchSize);
    }

    /**
     * 更新所有节点的路径
     */
    private void updateAllNodesPath() {
        List<T> all = findAll();
        all.forEach(e -> e.setPath(getPath(e, all)));
        super.saveAll(all);
    }

    /**
     * 添加list相关联的父节点
     *
     * @param list
     * @return
     */
    private void addParents(List<T> list) {
        List<Long> ids = list.stream().map(TreeEntity::getId).collect(Collectors.toList());
        List<Long> parentIds = list.stream().filter(e -> e.getParentId() != null && !ids.contains(e.getParentId())).map(TreeEntity::getParentId).distinct().collect(Collectors.toList());
        while (!parentIds.isEmpty()) {
            List<T> parents = findAllById(parentIds);
            list.addAll(parents);
            ids.addAll(parents.stream().map(TreeEntity::getId).toList());
            parentIds = parents.stream().filter(e -> e.getParentId() != null && !ids.contains(e.getParentId())).map(TreeEntity::getParentId).distinct().collect(Collectors.toList());
        }
    }

    /**
     * 判断是否为新增
     * 序列号为空表示新增
     *
     * @param t
     * @return
     */
    private boolean isCreate(T t) {
        return CharSequenceUtil.isBlank(t.getSerialNo());
    }

    @Override
    @Transactional
    public T move(TreeMoveInputDto input) {
        T entity = findById(input.getId()).orElseThrow(() -> new BusinessException(noFoundRecoredMsgTemplate + input.getId()));
        entity.setParentId(input.getParentId());
        save(entity);
        return entity;
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        T entity = findById(id).orElseThrow(() -> new BusinessException(noFoundRecoredMsgTemplate + id));
        delete(entity);
    }

    @Override
    public void delete(T t) {
        //同时删除所有子节点
        Query query = em.createQuery("DELETE FROM " + getEntityClass().getName() + " t WHERE t.path LIKE :likePath");
        query.setParameter("likePath", t.getPath() + TreeConstant.PATH_DELIMITER + "%");
        query.executeUpdate();
        super.delete(t);
    }

    @Override
    public List<T> findChildren(Long id) {
        Specification<T> query = Specifications.where(e -> e.eq(TreeEntity.Fields.parentId, id));
        return findAll(query);
    }

    @Override
    public List<T> findDescendants(Long id) {
        T entity = findById(id).orElse(null);
        if (entity == null) {
            return new ArrayList<>();
        }
        Specification<T> query = Specifications.where(e -> e.startingWith(TreeEntity.Fields.path, entity.getPath() + TreeConstant.PATH_DELIMITER));
        return findAll(query);
    }

    @Override
    public List<T> getAncestors(Long id) {
        T entity = findById(id).orElse(null);
        if (entity == null || entity.getParentId() == null) {
            return new ArrayList<>();
        }
        String path = entity.getPath();
        String parentPath = path.substring(0, path.length() - entity.getSerialNo().length() - 1);
        String[] serialNos = parentPath.split(TreeConstant.PATH_DELIMITER);
        Specification<T> query = Specifications.where(e -> e.in(TreeEntity.Fields.serialNo, serialNos));
        return findAll(query);
    }

    @Override
    public Page<T> getPage(TreePagedAndSortedInputDto input) {
        return findAll(input.getPageable());
    }

    @Override
    public String getPath(Long id) {
        return getPath(id, false);
    }

    @Override
    public String getPathEndWidthPathDelimiter(Long id) {
        return getPath(id, true);
    }

    @Override
    public List<Long> getAllDescendantIds(Long id, boolean includeSelf) {
        List<Long> result = findDescendants(id).stream().map(TreeEntity::getId).collect(Collectors.toList());
        if (includeSelf) {
            result.add(id);
        }
        return result;
    }

    @Override
    @DisableGlobalFilter
    public List<T> getNullSerialNoRecords() {
        Specification<T> query = Specifications.where(e -> {
            e.isNull(TreeEntity.Fields.serialNo);
            e.eq(FullAuditedEntity.Fields.dr, EntityConstant.LOGIC_UN_DELETE_VALUE);
        });
        return findAll(query, Sort.by(Sort.Order.asc(CreationEntity.Fields.createTime), Sort.Order.asc(FullAuditedEntity.Fields.lastModifyTime)));
    }

    /**
     * 修复序列号和路径
     *
     * @param list
     */
    @Override
    @Transactional
    public void fixNullSerialNoRecords(List<T> list) {
        if (list.isEmpty()) {
            return;
        }
        String[] serialNos = getSerialNos(list.size());
        //设置serialNo
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setSerialNo(serialNos[i]);
        }
        for (T t : list) {
            t.setPath(getPath(t, list));
        }
        super.batchSave(list, 1000);
    }

    @Override
    public List<TreeFullPathNameDto> getTreeFullPathNameList(List<Long> ids) {
        List<T> list;
        Map<String, String> map; //SerialNo, DisplayName

        if (ids == null || ids.isEmpty()) {
            list = findAll();
            if (list.isEmpty()) {
                return new ArrayList<>();
            }
            map = toSerialNoNameMap(list);
        } else {
            list = findAllById(ids);
            if (list.isEmpty()) {
                return new ArrayList<>();
            }
            Set<String> serialNos = new HashSet<>();
            for (T t : list) {
                serialNos.addAll(Arrays.asList(t.getPath().split(TreeConstant.PATH_DELIMITER)));
            }
            Specification<T> query = Specifications.where(e -> e.in(TreeEntity.Fields.serialNo, serialNos));

            List<T> allList = findAll(query);
            map = toSerialNoNameMap(allList);
        }
        return null;
        //return DozerUtils.mapList(dozerMapper, list, TreeFullPathNameDto.class, (s, t) -> {
        //    List<String> names = new ArrayList<>();
        //    String[] pathItems = t.getPath().split(TreeConstant.PATH_DELIMITER);
        //    for (String serialNo : pathItems) {
        //        names.add(map.get(serialNo));
        //    }
        //    t.setPathName(String.join("/", names));
        //});
    }

    /**
     * 转成(SerialNo, name)map, name默认用的DisplayName, 需要修改的话， 请覆盖这个方法
     *
     * @param list
     * @return
     */
    protected Map<String, String> toSerialNoNameMap(List<T> list) {
        return list.stream().collect(Collectors.toMap(TreeEntity::getSerialNo, TreeEntity::getDisplayName));
    }


    /**
     * 获取当前节点的路径
     *
     * @param t    当前节点
     * @param list 所有节点
     * @return
     */
    private String getPath(T t, List<T> list) {
        T parent = t.getParentId() == null ? null : list.stream().filter(e -> e.getId().equals(t.getParentId())).findFirst().orElse(null);
        if (parent == null) {
            return t.getSerialNo();
        } else {
            return getPath(parent, list) + TreeConstant.PATH_DELIMITER + t.getSerialNo();
        }
    }


    private void genSerialNos(List<T> list) {
        if (list.isEmpty()) {
            return;
        }
        String[] serialNos = getSerialNos(list.size());
        //设置serialNo
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setSerialNo(serialNos[i]);
        }
    }


    /**
     * 生成编码
     *
     * @return
     */
    private String getSerialNo() {
        return getSerialNos(1)[0];
    }


    /**
     * 生成编码
     *
     * @return
     */
    protected String[] getSerialNos(int count) {
        String[] serialNos = serialNoRpcService.getSerialNos(getEntityClass().getName(), EnumResetType.Never, "", 12, count);
        String[] result = new String[count];
        for (int i = 0; i < serialNos.length; i++) {
            result[i] = Integer.parseInt(serialNos[i]) + "";
            //去除前导0
        }
        return result;
    }

    /**
     * 更新所有后代的路径
     *
     * @param entity
     * @return 当前节点的新路径
     */
    private void updateDescendantsPath(T entity) {
        String currPath = entity.getPath(); //当前的路径
        String newPath = entity.getSerialNo();
        Long newParentId = entity.getParentId();
        if (newParentId != null) {
            T parent = findById(newParentId).orElseThrow(() -> new BusinessException("未找到父节点：parentId=" + newParentId));
            String parentPath = parent.getPath();
            if ((parentPath).startsWith(currPath + TreeConstant.PATH_DELIMITER)) {
                throw new BusinessException("不能以孩子节点作为父节点！");
            }
            newPath = parentPath + TreeConstant.PATH_DELIMITER + entity.getSerialNo();
        }
        //更新所有后代的路径
        if (!currPath.equals(newPath)) {
            updateDescendantsPath(currPath, newPath);
        }
        entity.setPath(newPath);
    }

    /**
     * 更新所有后代的路径
     *
     * @param currParentPath
     * @param newParentPath
     */
    private void updateDescendantsPath(String currParentPath, String newParentPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ")
                .append(getEntityClass().getName())
                .append(" t SET t.path = CONCAT(:newParentPath, SUBSTRING(t.path, :currParentPathLength)) ")
                .append("WHERE t.path LIKE :likePath  and (t.dr = :dr)");
        if (TenantContextHolder.getTenantId() == null) {
            sb.append(" and t.tenantId is null ");
        } else {
            sb.append(" and  ( t.tenantId = :tenantId )");
        }
        Query query = em.createQuery(sb.toString());
        query.setParameter("likePath", currParentPath + TreeConstant.PATH_DELIMITER + "%");
        query.setParameter("newParentPath", newParentPath);
        query.setParameter("currParentPathLength", currParentPath.length() + 1);
        //这里给多参数也不影响
        if (TenantContextHolder.getTenantId() != null) {
            query.setParameter("tenantId", TenantContextHolder.getTenantId());
        }
        query.setParameter("dr", EntityConstant.LOGIC_UN_DELETE_VALUE);
        int i = query.executeUpdate();
        log.debug("updateDescendantsPath update records:{}", i);
    }

    private String getPath(Long id, boolean endWidthPathDelimiter) {
        T entity = findById(id).orElseThrow(() -> new BusinessException(noFoundRecoredMsgTemplate + id));
        return endWidthPathDelimiter ? entity.getPath() + TreeConstant.PATH_DELIMITER : entity.getPath();
    }
}
