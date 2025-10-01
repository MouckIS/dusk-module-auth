package com.dusk.module.auth.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.dusk.common.rpc.auth.enums.EnumResetType;
import com.dusk.common.rpc.auth.service.ISerialNoRpcService;
import com.github.dozermapper.core.Mapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.lock.annotation.Lock4j;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.module.auth.dto.sysno.GetSerialNoInput;
import com.dusk.module.auth.dto.sysno.SerialNoEditInput;
import com.dusk.module.auth.entity.QSerialNo;
import com.dusk.module.auth.entity.SerialNo;
import com.dusk.module.auth.repository.ISerialNoRepository;
import com.dusk.module.auth.service.ISerialNoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-09-22 15:50
 */
@Service(retries = 0, timeout = 2000)
@Transactional
@Slf4j
public class SerialNoServiceImpl extends BaseService<SerialNo, ISerialNoRepository> implements ISerialNoRpcService, ISerialNoService {
    @Autowired
    JPAQueryFactory queryFactory;
    @Autowired
    Mapper dozerMapper;

    @Lock4j(keys = "#billType")
    @Override
    public String[] getSerialNos(String billType, EnumResetType resetType, String dateFormat, int serialLength, int count) {
        return getSerialNos(billType, resetType, dateFormat, serialLength, count, false);
    }

    @Lock4j(keys = "#billType")
    @Override
    public String[] getSerialNos(String billType, EnumResetType resetType, String dateFormat, int serialLength, int count, boolean codeFirst) {
        SerialNo serialNo = queryFactory.selectFrom(QSerialNo.serialNo).where(QSerialNo.serialNo.billType.eq(billType)).fetchFirst();
        long currentNo = 0;
        String[] bill_nos = new String[count];
        LocalDateTime now = LocalDateTime.now();
        if (serialNo != null) {
            currentNo = serialNo.getCurrentNo();
            if (!codeFirst) {
                dateFormat = serialNo.getDateFormat();
                serialLength = serialNo.getNoLength();
                resetType = serialNo.getResetType();
            } else {
                serialNo.setDateFormat(dateFormat);
                serialNo.setNoLength(serialLength);
                serialNo.setResetType(resetType);
            }

            LocalDateTime lastUpdateTime = serialNo.getLastUpdateTime();
            switch (resetType) {
                case Day:
                    if (now.getDayOfMonth() != lastUpdateTime.getDayOfMonth()) {
                        currentNo = 0;
                    }
                case Month:
                    if (now.getMonthValue() != lastUpdateTime.getMonthValue()) {
                        currentNo = 0;
                    }
                case Year:
                    if (now.getYear() != lastUpdateTime.getYear()) {
                        currentNo = 0;
                    }
                    break;
                default:
                    break;
            }
        } else {
            serialNo = new SerialNo();
            serialNo.setBillType(billType);
            serialNo.setDateFormat(dateFormat);
            serialNo.setNoLength(serialLength);
            serialNo.setResetType(resetType);
        }
        for (int i = 0; i < count; i++) {
            long nextNo = currentNo + i + 1;
            if (nextNo >= Math.pow(10, serialLength)) {
                throw new BusinessException("流水号超过最大限度：" + serialLength);
            }
            bill_nos[i] = getCurrentNo(now, dateFormat, nextNo, serialLength);
            serialNo.setCurrentNo(nextNo);
            serialNo.setLastNo(bill_nos[i]);
        }
        serialNo.setLastUpdateTime(LocalDateTime.now());

        save(serialNo);
        return bill_nos;
    }

    @Lock4j(keys = "#billType")
    @Override
    public String getSerialNo(String billType, EnumResetType resetType, String dateFormat, int serialLength) {
        return getSerialNos(billType, resetType, dateFormat, serialLength, 1)[0];
    }


    @Lock4j(keys = "#billType")
    @Override
    public String getSerialNo(String billType, EnumResetType resetType, String dateFormat, int serialLength, boolean codeFirst) {
        return getSerialNos(billType, resetType, dateFormat, serialLength, 1, codeFirst)[0];
    }

    @Override
    public Page<SerialNo> getSerialNos(GetSerialNoInput input) {
        Specification<SerialNo> specification = Specifications.where(e -> {
            if (StringUtils.isNotBlank(input.getBillType())) {
                e.contains(SerialNo.Fields.billType, input.getBillType());
            }
        });
        return repository.findAll(specification, input.getPageable());
    }

    @Override
    public String getCurrentNo(LocalDateTime now, String dateFormat, long nextNo, int serialLength) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(dateFormat)) {
            sb.append(DateUtil.format(now, dateFormat));
        }
        sb.append(StrUtil.padPre(String.valueOf(nextNo), serialLength, '0'));
        return sb.toString();
    }

    @Override
    public void update(SerialNoEditInput input) {
        SerialNo entity = repository.findById(input.getId()).orElseThrow(() -> new BusinessException("数据不存在"));
        dozerMapper.map(input, entity);
        save(entity);
    }

    @Override
    public SerialNo getOneById(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException("数据不存在"));
    }
}
