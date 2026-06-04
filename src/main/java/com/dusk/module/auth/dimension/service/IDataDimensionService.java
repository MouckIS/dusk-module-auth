package com.dusk.module.auth.dimension.service;

import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.service.IBaseService;
import com.dusk.module.auth.dimension.dto.DataDimensionCreateDto;
import com.dusk.module.auth.dimension.dto.DataDimensionDto;
import com.dusk.module.auth.dimension.dto.DataDimensionPagedInputDto;
import com.dusk.module.auth.dimension.dto.DataDimensionUpdateDto;
import com.dusk.module.auth.dimension.entity.DataDimension;
import com.dusk.module.auth.dimension.repository.IDataDimensionRepository;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.util.List;

/**
 * 数据维度服务接口
 *
 * @author dusk
 */
public interface IDataDimensionService extends IBaseService<DataDimension, IDataDimensionRepository> {

    /**
     * 分页查询数据维度
     *
     * @param input 分页查询参数
     * @return 分页结果
     */
    PagedResultDto<DataDimensionDto> getPage(DataDimensionPagedInputDto input);

    /**
     * 根据ID查询数据维度
     *
     * @param id 维度ID
     * @return 数据维度DTO
     */
    DataDimensionDto getById(Long id);

    /**
     * 根据维度编码查询数据维度
     *
     * @param code 维度编码
     * @return 数据维度DTO
     */
    DataDimensionDto getByCode(String code);

    /**
     * 创建数据维度
     *
     * @param dto 创建请求
     * @return 创建后的数据维度DTO
     */
    DataDimensionDto create(DataDimensionCreateDto dto);

    /**
     * 更新数据维度
     *
     * @param dto 更新请求
     * @return 更新后的数据维度DTO
     */
    DataDimensionDto update(DataDimensionUpdateDto dto);

    /**
     * 删除数据维度
     *
     * @param id 维度ID
     */
    void deleteById(Long id);

    /**
     * 导出数据维度为CSV
     *
     * @param response HTTP响应
     */
    void exportCsv(HttpServletResponse response);

    /**
     * 从CSV导入数据维度
     *
     * @param inputStream CSV文件输入流
     * @return 导入结果消息
     */
    String importCsv(InputStream inputStream);

    /**
     * 获取所有启用的数据维度
     *
     * @return 数据维度列表
     */
    List<DataDimensionDto> getAllEnabled();
}
