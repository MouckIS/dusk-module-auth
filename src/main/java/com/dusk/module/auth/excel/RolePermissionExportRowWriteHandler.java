package com.dusk.module.auth.excel;

import com.alibaba.excel.write.handler.RowWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;

/**
 * @author: pengmengjiang
 * @date: 2021/2/5 13:58
 */
public class RolePermissionExportRowWriteHandler implements RowWriteHandler {
    private boolean isNewRow = false;
    private Integer newRowIndex ;


    @Override
    public void beforeRowCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Integer rowIndex, Integer relativeRowIndex, Boolean isHead) {

    }

    @Override
    public void afterRowCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Row row, Integer relativeRowIndex, Boolean isHead) {
        Sheet sheet = row.getSheet();
        isNewRow = true;
        newRowIndex = row.getRowNum();
        CellStyle cellStyle = row.getSheet().getWorkbook().createCellStyle();
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        for (int i=0; i<8; i++){
            Cell cell = row.createCell(i);
            if(i==7){
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(row.getRowNum(), row.getRowNum(), 7, 7);
                DataValidationHelper helper = writeSheetHolder.getSheet().getDataValidationHelper();
                DataValidationConstraint constraint = helper.createExplicitListConstraint(new String[] {"是", "否"});
                DataValidation dataValidation = helper.createValidation(constraint, cellRangeAddressList);
                writeSheetHolder.getSheet().addValidationData(dataValidation);
            }
            cell.setCellStyle(cellStyle);
        }
        CellRangeAddress rangeAddress = new CellRangeAddress(row.getRowNum(),row.getRowNum(),0,3);
        sheet.addMergedRegion(rangeAddress);
        rangeAddress = new CellRangeAddress(row.getRowNum(),row.getRowNum(),4,6);
        sheet.addMergedRegion(rangeAddress);
    }

    @Override
    public void afterRowDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Row row, Integer relativeRowIndex, Boolean isHead) {

    }

}
