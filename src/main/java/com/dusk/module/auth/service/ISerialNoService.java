package com.dusk.module.auth.service;

import com.dusk.common.module.auth.service.ISerialNoRpcService;
import com.dusk.module.auth.dto.sysno.GetSerialNoInput;
import com.dusk.module.auth.dto.sysno.SerialNoEditInput;
import com.dusk.module.auth.entity.SerialNo;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

/**
 * @author kefuming
 * @date 2020-09-22 15:42
 */
public interface ISerialNoService extends ISerialNoRpcService {
    Page<SerialNo> getSerialNos(GetSerialNoInput input);

    /**
     * 根据条件获取当前的序列号
     * @param now
     * @param dateFormat
     * @param nextNo
     * @param serialLength
     * @return
     */
    String getCurrentNo(LocalDateTime now, String dateFormat, long nextNo, int serialLength);

    /**
     * 更新
     * @param input
     */
    void update(SerialNoEditInput input);

    SerialNo getOneById(Long id);
}
