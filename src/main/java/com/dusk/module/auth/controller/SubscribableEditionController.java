package com.dusk.module.auth.controller;

import com.dusk.common.core.annotation.Authorize;
import com.dusk.common.core.controller.CruxBaseController;
import com.dusk.common.core.dto.EntityDto;
import com.dusk.common.core.dto.PagedResultDto;
import com.dusk.common.core.exception.BusinessException;
import com.dusk.common.core.utils.MapperUtil;
import com.dusk.module.auth.authorization.EditionAuthProvider;
import com.dusk.module.auth.dto.edition.EditionEditDto;
import com.dusk.module.auth.dto.edition.EditionListDto;
import com.dusk.module.auth.dto.edition.GetEditionInput;
import com.dusk.module.auth.dto.edition.SubscribableEditionComboboxItemDto;
import com.dusk.module.auth.entity.SubscribableEdition;
import com.dusk.module.auth.mapper.SubscribableEditionMapper;
import com.dusk.module.auth.service.ISubscribableEditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kefuming
 * @date 2020-05-08 11:38
 */
@RestController
@RequestMapping("edition")
@Tag(name = "Edition", description = "版本")
@Authorize(EditionAuthProvider.PAGES_EDITIONS)
public class SubscribableEditionController extends CruxBaseController {
    private final SubscribableEditionMapper mapper = SubscribableEditionMapper.INSTANCE;
    @Resource
    private ISubscribableEditionService editionService;

    /**
     * 查询版本列表
     *
     * @return
     */
    @GetMapping("getEditions")
    @Operation(summary = "查询版本列表")
    public PagedResultDto<EditionListDto> getEditions(GetEditionInput input) {
        Page<SubscribableEdition> page = editionService.getEditions(input);
        return MapperUtil.mapToPagedResultDto(page, mapper::toEditionListDto);
    }

    @GetMapping("export/{id}")
    @Operation(summary = "导出版本")
    public void exportEdition(@PathVariable Long id, HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("版本信息导出", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        Workbook workbook = editionService.export(id);
        workbook.write(response.getOutputStream());
    }

    @PostMapping("import")
    @Operation(summary = "导入版本")
    public void importEdition(@RequestParam MultipartFile file) {
        InputStream in = null;
        try {
            in = file.getInputStream();
            editionService.importEdition(in);
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            } else {
                throw new BusinessException("导入失败,请检查Excel是否符合导入要求", e);
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * 获取版本信息进行编辑
     *
     * @param input
     * @return
     */
    @GetMapping("getEditionForEdit")
    @Operation(summary = "获取版本信息进行编辑")
    @Authorize(EditionAuthProvider.PAGES_EDITIONS_EDIT)
    public EditionEditDto getEditionForEdit(EntityDto input) {
        EditionEditDto editionEditDto;
        if (input.getId() != null) {
            SubscribableEdition edition = editionService.findById(input.getId()).orElseThrow(() -> new BusinessException("未找到相应的版本信息"));
            editionEditDto = mapper.toEditionEditDto(edition);
        } else {
            editionEditDto = new EditionEditDto();
        }
        return editionEditDto;
    }

    @PostMapping("createOrUpdateEdition")
    @Operation(summary = "新增或编辑版本信息")
    @Authorize(EditionAuthProvider.PAGES_EDITIONS_EDIT)
    public void createOrUpdateEdition(@Valid @RequestBody EditionEditDto input) {
        if (input.getId() == null) {
            editionService.createEdition(input);
        } else {
            editionService.updateEdition(input);
        }
    }

    @DeleteMapping("deleteEdition")
    @Operation(summary = "删除版本")
    @Authorize(EditionAuthProvider.PAGES_EDITIONS_DELETE)
    public void deleteEdition(@Valid @RequestBody EntityDto input) {
        editionService.deleteEdition(input.getId());
    }

    @GetMapping("getEditionComboboxItems")
    @Operation(summary = "获取版本选择列表")
    public List<SubscribableEditionComboboxItemDto> getEditionComboboxItems(
            @RequestParam(required = false) String selectedEditionId,
            @RequestParam(defaultValue = "false") Boolean addAllItem,
            @RequestParam(defaultValue = "false") Boolean onlyFreeItems) {
        var editions = editionService.findAll();
        var subscribableEditions = editions.stream()
                .filter(e -> {
                    return !onlyFreeItems || e.isFree();
                })
                .collect(Collectors.toList());

        var editionItems = subscribableEditions.stream()
                .map(e -> new SubscribableEditionComboboxItemDto(String.valueOf(e.getId()), e.getDisplayName(), e.isFree()))
                .collect(Collectors.toList());

        var defaultItem = new SubscribableEditionComboboxItemDto("", "没有分配", null);
        editionItems.add(0, defaultItem);

        if (addAllItem) {
            editionItems.add(0, new SubscribableEditionComboboxItemDto("-1", "- " + "全部" + " -", null));
        }

        if (StringUtils.isNoneBlank(selectedEditionId)) {
            var selectedEdition = editionItems.stream().filter(e -> selectedEditionId.equals(e.getValue())).findFirst();
            selectedEdition.ifPresent(subscribableEditionComboboxItemDto -> subscribableEditionComboboxItemDto.setSelected(true));
        } else {
            editionItems.get(0).setSelected(true);
        }
        return editionItems;
    }

}
