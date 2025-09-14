package com.dusk.module.auth.excel;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.dusk.common.framework.auth.permission.Permission;
import com.dusk.common.framework.feature.ui.*;
import com.dusk.module.auth.entity.SubscribableEdition;
import org.springframework.cglib.beans.BeanCopier;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @Author: pengmengjiang
 * @Date: 2021/9/28 10:47
 */
@Slf4j
public class EditionExcelExportWriter {

    private final SubscribableEdition edition;

    @Setter
    private List<String> editionPermissions;

    @Setter
    private List<Permission> definitionPermissions;

    @Setter
    private List<TenantFeature> editionFeatures;

    @Getter
    private final Workbook workbook;

    boolean hasWrite = false;

    int maxPermissionDepth ;

    Sheet sheet ;
    Row row;
    Cell cell;
    int rowIdx = 0;

    public final static int FEATURE_START_INDEX = 5;

    public final static String SEPARATOR = "->";

    DataValidationHelper validationHelper;


    public EditionExcelExportWriter(SubscribableEdition edition) {
        workbook = new XSSFWorkbook();
        this.edition = edition;
        sheet = workbook.createSheet();
        validationHelper = sheet.getDataValidationHelper();
    }

    public void write(){
        if(hasWrite){
            return;
        }
        List<InnerPermission> permissions = convertPermission();

        int colIdx = 0;
        row = sheet.createRow(rowIdx++);
        cell = row.createCell(colIdx++);
        sheet.setColumnHidden(0,true);
        cell = row.createCell(colIdx++);
        cell.setCellValue("版本名称");
        cell = row.createCell(colIdx++);
        cell.setCellValue(edition.getDisplayName());
        maxPermissionDepth = maxPermissionDepth==0?1:maxPermissionDepth;
        colIdx = 1;
        row = sheet.createRow(rowIdx++);
        cell = row.createCell(colIdx++);
        cell.setCellValue("权限名");
        cell = row.createCell(colIdx++);
        cell.setCellValue("是否授予");

        writePermission(permissions);
        if(rowIdx>2){
            DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(new String[] {"是", "否"});
            DataValidation dataValidation = validationHelper.createValidation(constraint, new CellRangeAddressList(2,rowIdx-1,2,2));
            sheet.addValidationData(dataValidation);
        }

        List<InnerFeature> features = convertFeature();
        rowIdx = 1;
        row = sheet.getRow(rowIdx++);
        colIdx = FEATURE_START_INDEX;
        cell = row.createCell(colIdx++);
        cell = row.createCell(colIdx++);
        cell.setCellValue("特性");
        cell = row.createCell(colIdx++);
        cell.setCellValue("特性值");
        writeFeature(features);

        sheet.setColumnHidden(FEATURE_START_INDEX,true);
        row = sheet.getRow(1);
        for (int i = 0; i < row.getLastCellNum(); i++) {
            sheet.autoSizeColumn(i,true);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) * 17 / 10);
        }
    }

    List<InnerPermission> convertPermission(){
        List<InnerPermission> list = new ArrayList<>();
        definitionPermissions.forEach(p->{
            InnerPermission permission = new InnerPermission();
            permission.setName(p.getName());
            permission.setDisplayName(p.getDisplayName());
            if(p.getParent()!=null){
                permission.setParentName(p.getParent().getName());
            }
            list.add(permission);
            Iterator<String> it = editionPermissions.iterator();
            while (it.hasNext()){
                String next = it.next();
                if(StringUtils.equals(next,permission.getName())){
                    permission.setGranted(true);
                    it.remove();
                    break;
                }
            }
        });
        List<InnerPermission> clone = new ArrayList<>();
        clone.addAll(list);
        Iterator<InnerPermission> it = list.iterator();
        while (it.hasNext()){
            InnerPermission parent = it.next();
            Iterator<InnerPermission> cloneIterator = clone.iterator();
            while (cloneIterator.hasNext()){
                InnerPermission child = cloneIterator.next();
                if(StringUtils.equals(parent.getName(),child.getParentName())){
                    parent.addChild(child);
                    child.setParent(parent);
                    cloneIterator.remove();
                }
            }
        }
        it = list.iterator();
        while (it.hasNext()){
            InnerPermission next = it.next();
            if(next.getParent()!=null){
                it.remove();
                if(next.getChildren().isEmpty()){
                    setParentPermissionDepth(next.getParent(),1);
                }
            }
        }

        return list;
    }

    void setParentPermissionDepth(InnerPermission parent, int depth){
        if(parent.getDepth()<depth){
            parent.setDepth(depth);
            maxPermissionDepth = maxPermissionDepth<depth?depth:maxPermissionDepth;
            if(parent.getParent()!=null){
                setParentPermissionDepth(parent.getParent(),parent.getDepth()+1);
            }
        }
    }

    void writePermission(List<InnerPermission> permissions){
        for (InnerPermission permission : permissions) {
            int colIdx = 0;
            row = sheet.createRow(rowIdx++);
            cell = row.createCell(colIdx++);
            cell.setCellValue(permission.getName());

            cell = row.createCell(colIdx++);
            cell.setCellValue(permission.getFullPathName());

            cell = row.createCell(colIdx++);
            cell.setCellValue(permission.isGranted()?"是":"否");
            if(!permission.getChildren().isEmpty()){
                writePermission(permission.getChildren());
            }
        }
    }

    List<InnerFeature> convertFeature(){
        List<InnerFeature> list = new ArrayList<>();
        BeanCopier copier = BeanCopier.create(TenantFeature.class,InnerFeature.class,false);
        for (TenantFeature editionFeature : editionFeatures) {
            InnerFeature feature = new InnerFeature();
            copier.copy(editionFeature,feature,null);
            list.add(feature);
        }
        List<InnerFeature> clone = new ArrayList<>();
        clone.addAll(list);
        Iterator<InnerFeature> iterator = list.iterator();
        while (iterator.hasNext()) {
            InnerFeature feature = iterator.next();
            Iterator<InnerFeature> it = clone.iterator();
            while (it.hasNext()){
                InnerFeature child = it.next();
                if(StringUtils.equals(child.getParentName(),feature.getName())){
                    child.setParent(feature);
                    feature.addChild(child);
                    it.remove();
                }
            }
            if(StringUtils.isNotBlank(feature.getParentName())){
                iterator.remove();
            }
        }
        return list;
    }

    void writeFeature(List<InnerFeature> features){
        int rows = sheet.getPhysicalNumberOfRows();
        for (InnerFeature feature : features) {
            int colIdx = FEATURE_START_INDEX;
            if(rowIdx<rows){
                row = sheet.getRow(rowIdx++);
            }else{
                row = sheet.createRow(rowIdx++);
            }
            cell = row.createCell(colIdx++);
            cell.setCellValue(feature.getName());
            cell = row.createCell(colIdx++);
            cell.setCellValue(feature.getFullPathName());
            cell = row.createCell(colIdx++);
            InputType inputType = feature.getInputType();
            if(inputType instanceof ComboBox){
                ItemSource source = inputType.getItemSource();
                String value = feature.getFeatureValue();
                String[] values = new String[source.getItems().size()];
                int i = 0;
                for (Item item : source.getItems()) {
                    values[i++] = item.getValue()+SEPARATOR+item.getDisplayText();
                    if(StringUtils.equals(item.getValue(),value)){
                        value = item.getValue()+SEPARATOR+item.getDisplayText();
                    }
                }
                cell.setCellValue(value);
                DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(values);
                DataValidation dataValidation = validationHelper.createValidation(constraint, new CellRangeAddressList(cell.getRowIndex(),cell.getRowIndex(),cell.getColumnIndex(),cell.getColumnIndex()));
                sheet.addValidationData(dataValidation);
            }else if(inputType instanceof CheckBox){
                DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(new String[]{"true","false"});
                DataValidation dataValidation = validationHelper.createValidation(constraint, new CellRangeAddressList(cell.getRowIndex(),cell.getRowIndex(),cell.getColumnIndex(),cell.getColumnIndex()));
                sheet.addValidationData(dataValidation);
                cell.setCellValue(feature.getFeatureValue());
            }else{
                cell.setCellValue(feature.getFeatureValue());
            }
            if(!feature.getChildren().isEmpty()){
                writeFeature(feature.getChildren());
            }
        }
    }

    @Setter
    @Getter
    class InnerPermission implements Serializable {
        private InnerPermission parent;
        private String parentName;
        private String name;
        private String displayName;
        private boolean granted ;
        private int depth;
        private List<InnerPermission> children;

        public InnerPermission() {
            this.children = new ArrayList<>();
        }
        public void addChild(InnerPermission child){
            children.add(child);
        }

        public void setParent(InnerPermission parent) {
            this.parent = parent;
        }

        public String getFullPathName(){
            if(parent==null){
                return displayName;
            }else{
                return parent.getFullPathName()+SEPARATOR+displayName;
            }
        }
    }

    @Setter
    @Getter
    class InnerFeature implements Serializable{
        InnerFeature parent;
        String parentName;
        String name;
        String featureValue;
        String displayName;
        String description;
        String defaultValue;
        InputType inputType;
        List<InnerFeature> children;

        public InnerFeature() {
            children = new ArrayList<>();
        }

        public void addChild(InnerFeature child){
            children.add(child);
        }

        public String getFullPathName(){
            if(parent==null){
                return displayName;
            }else{
                return parent.getFullPathName()+SEPARATOR+displayName;
            }
        }
    }
}
