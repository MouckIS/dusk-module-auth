package com.dusk.module.auth.dimension.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.exception.ResourceNotFoundException;
import com.dusk.common.core.jpa.Specifications;
import com.dusk.common.core.service.impl.BaseService;
import com.dusk.module.auth.dimension.dto.*;
import com.dusk.module.auth.dimension.entity.DataDimension;
import com.dusk.module.auth.dimension.enums.DimensionOperationType;
import com.dusk.module.auth.dimension.enums.DimensionTargetType;
import com.dusk.module.auth.dimension.repository.IDataDimensionRepository;
import com.dusk.module.auth.dimension.repository.IDimensionValueRepository;
import com.dusk.module.auth.dimension.repository.IUserDimensionPermissionRepository;
import com.dusk.module.auth.dimension.service.IDataDimensionService;
import com.dusk.module.auth.dimension.service.IDimensionOperationLogService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据维度服务实现
 *
 * @author dusk
 */
@Service
public class DataDimensionServiceImpl extends BaseService<DataDimension, IDataDimensionRepository>
        implements IDataDimensionService {

    @Autowired
    private IDimensionValueRepository dimensionValueRepository;

    @Autowired
    private IUserDimensionPermissionRepository userDimensionPermissionRepository;

    @Autowired
    private IDimensionOperationLogService operationLogService;

    @Override
    public PagedResultDto<DataDimensionDto> getPage(DataDimensionPagedInputDto input) {
        Page<DataDimension> page = repository.findAll(
                Specifications.where(w -> {
                    w.contains(StrUtil.isNotBlank(input.getDimensionName()), DataDimension.Fields.dimensionName, input.getDimensionName());
                    w.contains(StrUtil.isNotBlank(input.getDimensionCode()), DataDimension.Fields.dimensionCode, input.getDimensionCode());
                    w.contains(StrUtil.isNotBlank(input.getDimensionDesc()), DataDimension.Fields.dimensionDesc, input.getDimensionDesc());
                    w.eq(input.getEnabled() != null, DataDimension.Fields.enabled, input.getEnabled());

                    // 关键字搜索（名称或描述）
                    if (StrUtil.isNotBlank(input.getKeyword())) {
                        w.or(or -> {
                            or.contains(true, DataDimension.Fields.dimensionName, input.getKeyword());
                            or.contains(true, DataDimension.Fields.dimensionDesc, input.getKeyword());
                        });
                    }
                }),
                input.getPageable()
        );

        List<DataDimensionDto> items = page.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new PagedResultDto<>(page.getTotalElements(), items);
    }

    @Override
    public DataDimensionDto getById(Long id) {
        DataDimension entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("数据维度不存在，ID: " + id));
        return toDto(entity);
    }

    @Override
    public DataDimensionDto getByCode(String code) {
        DataDimension entity = repository.findByDimensionCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("数据维度不存在，编码: " + code));
        return toDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDimensionDto create(DataDimensionCreateDto dto) {
        // 检查编码唯一性
        if (repository.existsByDimensionCode(dto.getDimensionCode())) {
            throw new BusinessException("维度编码已存在: " + dto.getDimensionCode());
        }

        DataDimension entity = new DataDimension();
        BeanUtil.copyProperties(dto, entity);
        entity.setEnabled(true);
        entity = repository.save(entity);

        operationLogService.log(DimensionOperationType.ADD, DimensionTargetType.DIMENSION,
                entity.getId(), entity.getDimensionName(),
                "创建数据维度: " + entity.getDimensionName() + " (" + entity.getDimensionCode() + ")");

        return toDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDimensionDto update(DataDimensionUpdateDto dto) {
        DataDimension entity = repository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("数据维度不存在，ID: " + dto.getId()));

        entity.setDimensionName(dto.getDimensionName());
        entity.setDimensionDesc(dto.getDimensionDesc());
        if (dto.getEnabled() != null) {
            entity.setEnabled(dto.getEnabled());
        }
        entity.setVersion(dto.getVersion());
        entity = repository.save(entity);

        operationLogService.log(DimensionOperationType.UPDATE, DimensionTargetType.DIMENSION,
                entity.getId(), entity.getDimensionName(),
                "更新数据维度: " + entity.getDimensionName());

        return toDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        DataDimension entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("数据维度不存在，ID: " + id));

        // 删除关联的权限
        userDimensionPermissionRepository.deleteByDimensionId(id);
        // 删除关联的维度值
        dimensionValueRepository.deleteByDimensionId(id);
        // 删除维度
        repository.deleteById(id);

        operationLogService.log(DimensionOperationType.DELETE, DimensionTargetType.DIMENSION,
                entity.getId(), entity.getDimensionName(),
                "删除数据维度: " + entity.getDimensionName() + " (" + entity.getDimensionCode() + ")");
    }

    @Override
    public void exportCsv(HttpServletResponse response) {
        List<DataDimension> list = repository.findAll();

        try {
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode("数据维度.csv", StandardCharsets.UTF_8));

            // 写入BOM，以便Excel正确识别UTF-8编码
            OutputStream os = response.getOutputStream();
            os.write(0xEF);
            os.write(0xBB);
            os.write(0xBF);

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
            // 写入CSV头
            writer.println("维度名称,维度编码,维度描述,是否启用");

            for (DataDimension entity : list) {
                writer.println(String.format("%s,%s,%s,%s",
                        escapeCsv(entity.getDimensionName()),
                        escapeCsv(entity.getDimensionCode()),
                        escapeCsv(entity.getDimensionDesc()),
                        entity.getEnabled() != null && entity.getEnabled() ? "true" : "false"));
            }
            writer.flush();

            operationLogService.log(DimensionOperationType.EXPORT, DimensionTargetType.DIMENSION,
                    null, null, "导出数据维度CSV，共 " + list.size() + " 条");
        } catch (IOException e) {
            throw new BusinessException("导出CSV文件失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importCsv(InputStream inputStream) {
        List<DataDimensionCsvDto> csvList = parseCsv(inputStream);
        int successCount = 0;
        int skipCount = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < csvList.size(); i++) {
            DataDimensionCsvDto csv = csvList.get(i);
            int lineNo = i + 2; // CSV第一行是标题

            if (StrUtil.isBlank(csv.getDimensionName()) || StrUtil.isBlank(csv.getDimensionCode())) {
                errors.add("第" + lineNo + "行：维度名称和维度编码不能为空");
                continue;
            }

            if (repository.existsByDimensionCode(csv.getDimensionCode())) {
                skipCount++;
                continue;
            }

            DataDimension entity = new DataDimension();
            entity.setDimensionName(csv.getDimensionName());
            entity.setDimensionCode(csv.getDimensionCode());
            entity.setDimensionDesc(csv.getDimensionDesc());
            entity.setEnabled(!"false".equalsIgnoreCase(csv.getEnabled()));
            repository.save(entity);
            successCount++;
        }

        String result = String.format("导入完成：成功 %d 条，跳过 %d 条（编码已存在），失败 %d 条",
                successCount, skipCount, errors.size());
        if (!errors.isEmpty()) {
            result += "。错误详情：" + String.join("；", errors);
        }

        operationLogService.log(DimensionOperationType.IMPORT, DimensionTargetType.DIMENSION,
                null, null, result);

        return result;
    }

    @Override
    public List<DataDimensionDto> getAllEnabled() {
        List<DataDimension> list = repository.findAll(
                Specifications.where(w -> w.eq(true, DataDimension.Fields.enabled, true))
        );
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    private DataDimensionDto toDto(DataDimension entity) {
        DataDimensionDto dto = new DataDimensionDto();
        BeanUtil.copyProperties(entity, dto);
        return dto;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private List<DataDimensionCsvDto> parseCsv(InputStream inputStream) {
        List<DataDimensionCsvDto> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // 跳过标题行
                }
                String[] fields = parseCsvLine(line);
                if (fields.length >= 2) {
                    DataDimensionCsvDto csv = new DataDimensionCsvDto();
                    csv.setDimensionName(fields[0].trim());
                    csv.setDimensionCode(fields[1].trim());
                    csv.setDimensionDesc(fields.length > 2 ? fields[2].trim() : "");
                    csv.setEnabled(fields.length > 3 ? fields[3].trim() : "true");
                    list.add(csv);
                }
            }
        } catch (IOException e) {
            throw new BusinessException("解析CSV文件失败: " + e.getMessage());
        }
        return list;
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
