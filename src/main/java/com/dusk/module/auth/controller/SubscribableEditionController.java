package com.dusk.module.auth.controller;

import com.github.dozermapper.core.Mapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import com.dusk.common.framework.annotation.Authorize;
import com.dusk.common.framework.controller.CruxBaseController;
import com.dusk.common.framework.dto.EntityDto;
import com.dusk.common.framework.dto.PagedResultDto;
import com.dusk.common.framework.exception.BusinessException;
import com.dusk.common.framework.utils.DozerUtils;
import com.dusk.module.auth.authorization.EditionAuthProvider;
import com.dusk.module.auth.dto.edition.EditionEditDto;
import com.dusk.module.auth.dto.edition.EditionListDto;
import com.dusk.module.auth.dto.edition.GetEditionInput;
import com.dusk.module.auth.dto.edition.SubscribableEditionComboboxItemDto;
import com.dusk.module.auth.entity.SubscribableEdition;
import com.dusk.module.auth.service.IFeatureService;
import com.dusk.module.auth.service.ISubscribableEditionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
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
@Api(description = "版本", tags = "Edition")
@Authorize(EditionAuthProvider.PAGES_EDITIONS)
public class SubscribableEditionController  extends CruxBaseController {
    @Autowired
    private ISubscribableEditionService editionService;
    @Autowired
    private Mapper dozerMapper;
    @Autowired
    IFeatureService featureService;

    /**
     * 查询版本列表
     * @return
     */
    @GetMapping("getEditions")
    @ApiOperation(value = "查询版本列表")
    public PagedResultDto<EditionListDto> getEditions(GetEditionInput input){
        Page<SubscribableEdition> page = editionService.getEditions(input);
        List<EditionListDto> list = DozerUtils.mapList(dozerMapper, page.getContent(), EditionListDto.class);
        return new PagedResultDto<>(page.getTotalElements(), list);
    }

    @GetMapping("export/{id}")
    @ApiOperation("导出版本")
    public void exportEdition(@PathVariable Long id, HttpServletResponse response) throws Exception{
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("版本信息导出", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        Workbook workbook = editionService.export(id);
        workbook.write(response.getOutputStream());
    }

    @PostMapping("import")
    @ApiOperation("导入版本")
    public void importEdition(@RequestParam MultipartFile file){
        InputStream in = null;
        try{
            in = file.getInputStream();
            editionService.importEdition(in);
        }catch (Exception e){
            if(e instanceof BusinessException){
                throw (BusinessException)e;
            }else {
                throw new BusinessException("导入失败,请检查Excel是否符合导入要求",e);
            }
        }finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * 获取版本信息进行编辑
     * @param input
     * @return
     */
    @GetMapping("getEditionForEdit")
    @ApiOperation(value = "获取版本信息进行编辑")
    @Authorize(EditionAuthProvider.PAGES_EDITIONS_EDIT)
    public EditionEditDto getEditionForEdit(EntityDto input){
        EditionEditDto editionEditDto;
        if (input.getId()!=null)
        {
            var edition = editionService.findById(input.getId()).orElseThrow(() -> new BusinessException("未找到相应的版本信息"));
            editionEditDto = dozerMapper.map(edition, EditionEditDto.class);
        }
        else
        {
            editionEditDto = new EditionEditDto();
        }
        return editionEditDto;
    }

    @PostMapping("createOrUpdateEdition")
    @ApiOperation(value = "新增或编辑版本信息")
    @Authorize(EditionAuthProvider.PAGES_EDITIONS_EDIT)
    public void createOrUpdateEdition(@Valid @RequestBody EditionEditDto input){
        if (input.getId()==null){
            editionService.createEdition(input);
        }else{
            editionService.updateEdition(input);
        }
    }

    @DeleteMapping("deleteEdition")
    @ApiOperation(value = "删除版本")
    @Authorize(EditionAuthProvider.PAGES_EDITIONS_DELETE)
    public void deleteEdition(@Valid @RequestBody EntityDto input){
        editionService.deleteEdition(input.getId());
    }

    @GetMapping("getEditionComboboxItems")
    @ApiOperation(value = "获取版本选择列表")
    public List<SubscribableEditionComboboxItemDto> getEditionComboboxItems(
            @RequestParam(required = false) String selectedEditionId,
            @RequestParam(defaultValue = "false")Boolean addAllItem,
            @RequestParam(defaultValue = "false") Boolean onlyFreeItems){
        var editions = editionService.findAll();
        var subscribableEditions = editions.stream()
                .filter(e -> {
                    return !onlyFreeItems || e.isFree();
                })
                .collect(Collectors.toList());

        var editionItems = subscribableEditions.stream()
                .map(e -> new SubscribableEditionComboboxItemDto(String.valueOf( e.getId()), e.getDisplayName(), e.isFree()))
                .collect(Collectors.toList());

        var defaultItem = new SubscribableEditionComboboxItemDto("", "没有分配", null);
        editionItems.add(0,defaultItem);

        if (addAllItem){
            editionItems.add(0, new SubscribableEditionComboboxItemDto("-1", "- " + "全部" + " -", null));
        }

        if (StringUtils.isNoneBlank(selectedEditionId)){
            var selectedEdition = editionItems.stream().filter(e -> selectedEditionId.equals(e.getValue())).findFirst();
            selectedEdition.ifPresent(subscribableEditionComboboxItemDto -> subscribableEditionComboboxItemDto.setSelected(true));
        }else{
            editionItems.get(0).setSelected(true);
        }
        return editionItems;
    }

}
