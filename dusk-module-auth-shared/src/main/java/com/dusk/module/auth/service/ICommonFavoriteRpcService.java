package com.dusk.module.auth.service;

import com.dusk.common.core.dto.CommonFavoriteDto;
import com.dusk.common.core.dto.PagedAndSortedInputDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.service.IFavoriteService;

/**
 * 不建议使用此rpc接口进行收藏操作，请使用 {@link IFavoriteService IFavoriteService } 进行操作
 *
 * @author chenzhi1
 * @date 2021/5/25 11:27
 */
public interface ICommonFavoriteRpcService {
    /**
     * 添加或更新
     *
     * @param dto
     * @return
     */
    CommonFavoriteDto save(CommonFavoriteDto dto);

    void deleteById(Long id, String type);

    void setPublic(Long id, boolean isPublic);

    CommonFavoriteDto get(Long id, String type);

    /**
     * 通过类型获取收藏列表
     *
     */
    PagedResultDto<CommonFavoriteDto> list(boolean onlyMine, PagedAndSortedInputDto pageDto, String... types);
}
