package com.dusk.module.auth.service;


import org.apache.poi.ss.usermodel.Workbook;
import com.dusk.common.framework.service.IBaseService;
import com.dusk.module.auth.dto.edition.EditionEditDto;
import com.dusk.module.auth.dto.edition.GetEditionInput;
import com.dusk.module.auth.entity.SubscribableEdition;
import com.dusk.module.auth.repository.ISubscribableEditionRepository;
import org.springframework.data.domain.Page;

import java.io.InputStream;
import java.util.Optional;

/**
 * @author kefuming
 * @date 2020-05-08 10:14
 */
public interface ISubscribableEditionService extends IBaseService<SubscribableEdition, ISubscribableEditionRepository> {

    /**
     * 创建版本
     *
     * @param input
     * @return
     */
    SubscribableEdition createEdition(EditionEditDto input);

    /**
     * 更新版本
     *
     * @param input
     */
    void updateEdition(EditionEditDto input);

    /**
     * 查询版本列表
     *
     * @param input
     * @return
     */
    Page<SubscribableEdition> getEditions(GetEditionInput input);

    /**
     * 根据显示名称查找
     *
     * @param name
     * @return
     */
    Optional<SubscribableEdition> findByDisplayName(String name);


    void deleteEdition(Long editionId);

    Workbook export(Long id);

    void importEdition(InputStream in);
}
