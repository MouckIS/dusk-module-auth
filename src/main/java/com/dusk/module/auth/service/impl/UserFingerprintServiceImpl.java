package com.dusk.module.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.dusk.module.auth.dto.fingerprint.*;
import com.github.dozermapper.core.Mapper;
import org.apache.dubbo.config.annotation.Reference;
import com.dusk.common.framework.entity.BaseEntity;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.jpa.Specifications;
import com.dusk.common.framework.service.impl.BaseService;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.common.framework.utils.MqttUtils;
import com.dusk.common.framework.utils.SecurityUtils;
import com.dusk.common.framework.utils.UtBeanUtils;
import com.dusk.common.module.auth.dto.fingerprint.GetAllInputDto;
import com.dusk.common.module.auth.dto.fingerprint.UserFingerprintDto;
import com.dusk.common.module.auth.enums.EnumResetType;
import com.dusk.common.module.auth.service.ISerialNoRpcService;
import com.dusk.module.auth.cache.IUserFingerprintCacheService;
import com.dusk.module.auth.dto.fingerprint.*;
import com.dusk.module.auth.entity.UserFingerprint;
import com.dusk.module.auth.repository.IUserFingerprintRepository;
import com.dusk.module.auth.service.IUserFingerprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author panyanlin1
 * @date 2021-05-11 17:10:02
 */
@Service
@Transactional
public class UserFingerprintServiceImpl extends BaseService<UserFingerprint, IUserFingerprintRepository> implements IUserFingerprintService {
    @Autowired
    IUserFingerprintCacheService userFingerprintCacheService;
    @Reference
    ISerialNoRpcService serialNoRpcService;
    @Autowired
    MqttUtils mqttUtils;
    @Autowired
    Mapper mapper;
    @Autowired
    SecurityUtils securityUtils;

    //注册指纹开始{指纹仪序列号}
    private final String TOPIC_REGISTER_START = "Fingerprint/{}/Register/Start";
    //验证指纹开始{指纹仪序列号}
    private final String TOPIC_IDENTIFY_START = "Fingerprint/{}/Identify/Start";

    @Override
    public void registerFingerprint(RegisterFingerprintInputDto inputDto) {
        mqttUtils.publishMsgAsync(StrUtil.format(TOPIC_REGISTER_START, inputDto.getDeviceNo())
                , new RegisterFingerprintStartPayload(getUserSeq(inputDto.getUserId())));
    }

    @Override
    public Long saveFingerprint(SaveFingerprintInputDto inputDto) {
        //校验指纹命名是否重复
        long sameFingerCount = count(Specifications.where(e -> {
            e.eq(UserFingerprint.Fields.userId, inputDto.getUserId());
            e.eq(UserFingerprint.Fields.fromEnum, inputDto.getFromEnum());
            e.eq(UserFingerprint.Fields.name, inputDto.getName());
            e.ne(inputDto.getId() != null, BaseEntity.Fields.id, inputDto.getId());
        }));
        if(sameFingerCount > 0){
            throw new BusinessException(StrUtil.format("已存在【{}】指纹！", inputDto.getName()));
        }

        if (inputDto.getId() == null) {
            long count = count(Specifications.where(e -> e.eq(UserFingerprint.Fields.userId, inputDto.getUserId())));
            if (count >= 10) {
                throw new BusinessException("每个用户最多只能录入10个指纹");
            }
            return save(mapper.map(inputDto, UserFingerprint.class)).getId();
        } else {
            UserFingerprint fingerprint = findById(inputDto.getId()).orElseThrow(() -> new BusinessException("指纹记录不存在"));
            UtBeanUtils.copyNotNullProperties(inputDto, fingerprint);
            return save(fingerprint).getId();
        }
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        repository.deleteByIdIn(ids);
    }

    @Override
    public List<UserFingerprintDto> getAll(GetAllInputDto inputDto) {
        List<UserFingerprint> all = findAll(Specifications.where(e -> {
            e.in(inputDto.getUserIds().size() > 0, UserFingerprint.Fields.userId, inputDto.getUserIds());
            e.contains(StrUtil.isNotBlank(inputDto.getFilter()), UserFingerprint.Fields.name, inputDto.getFilter());
            e.eq(inputDto.getFingerprintId() != null, BaseEntity.Fields.id, inputDto.getFingerprintId());
        }), Sort.by(BaseEntity.Fields.id));

        return DozerUtils.mapList(mapper, all, UserFingerprintDto.class);
    }

    @Override
    public void identify(IdentifyInputDto inputDto) {
        List<UserFingerprint> fingerprintList = findAll(Specifications.where(e -> {
            e.eq(UserFingerprint.Fields.userId, inputDto.getUserId());
            e.eq(inputDto.getFingerprintId() != null, BaseEntity.Fields.id, inputDto.getFingerprintId());
        }));

        if (fingerprintList.isEmpty()) {
            throw new BusinessException("未找到用户指纹数据");
        }

        List<String> dataList = fingerprintList.stream().map(UserFingerprint::getData).collect(Collectors.toList());
        String[] dataArr = dataList.toArray(new String[]{});

        mqttUtils.publishMsgAsync(StrUtil.format(TOPIC_IDENTIFY_START, inputDto.getDeviceNo())
                , new IdentifyFingerprintStartPayload(fingerprintList.get(0).getUserSeq(), dataArr));
    }

    @Override
    public Long saveFingerprintPrivate(SaveFingerprintInputDto inputDto) {
        if (inputDto.getId() != null) {
            UserFingerprint fingerprint = findById(inputDto.getId()).orElseThrow(() -> new BusinessException("指纹记录不存在"));
            if (!fingerprint.getCreateId().equals(securityUtils.getCurrentUser().getId())) {
                throw new BusinessException("不允许新增/保存他人指纹记录");
            }
        } else{
            if(!securityUtils.getCurrentUser().getId().equals(inputDto.getUserId())) {
                throw new BusinessException("不允许新增/保存他人指纹记录");
            }
        }
        return saveFingerprint(inputDto);
    }

    @Override
    public void deleteByIdsPrivate(List<Long> ids) {
        List<UserFingerprint> userFingerprintList = findAllById(ids);
        if (userFingerprintList.stream().anyMatch(userFingerprint ->
                !userFingerprint.getUserId().equals(securityUtils.getCurrentUser().getId()))) {
            throw new BusinessException("不允许删除他人指纹记录");
        }

        deleteByIds(ids);
    }


    private int getUserSeq(Long userId) {
        Integer userSeq = userFingerprintCacheService.getUserSeq(userId);
        if (userSeq == null) {
            synchronized (this) {
                userSeq = userFingerprintCacheService.getUserSeq(userId);
                if (userSeq != null) {
                    return userSeq;
                }

                List<UserFingerprint> userFingerprintList = findAll(Specifications.where(e -> {
                    e.eq(UserFingerprint.Fields.userId, userId);
                    e.isNotNull(UserFingerprint.Fields.userSeq);
                }));

                if (userFingerprintList.size() > 0) {
                    userSeq = userFingerprintList.get(0).getUserSeq();
                } else {
                    userSeq = Integer.parseInt(serialNoRpcService.getSerialNo("userSeq", EnumResetType.Never,
                            null, 5));
                }

                userFingerprintCacheService.saveUserSeq(userId, userSeq);
            }
        }

        return userSeq;
    }


}
