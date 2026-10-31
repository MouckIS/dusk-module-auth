package com.dusk.module.auth.excel;

import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link RolePermissionExportRowWriteHandler} 单元测试，目标：100% 分支覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RolePermissionExportRowWriteHandlerTest {

    @Mock
    private WriteSheetHolder writeSheetHolder;
    @Mock
    private Sheet sheet;
    @Mock
    private Workbook workbook;
    @Mock
    private Row row;
    @Mock
    private Cell cell;
    @Mock
    private CellStyle cellStyle;
    @Mock
    private DataValidationHelper dataValidationHelper;
    @Mock
    private DataValidationConstraint constraint;
    @Mock
    private DataValidation dataValidation;

    private RolePermissionExportRowWriteHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RolePermissionExportRowWriteHandler();
        when(row.getSheet()).thenReturn(sheet);
        when(row.getRowNum()).thenReturn(3);
        when(row.createCell(anyInt())).thenReturn(cell);
        when(sheet.getWorkbook()).thenReturn(workbook);
        when(sheet.getDataValidationHelper()).thenReturn(dataValidationHelper);
        when(workbook.createCellStyle()).thenReturn(cellStyle);
        when(writeSheetHolder.getSheet()).thenReturn(sheet);
        when(dataValidationHelper.createExplicitListConstraint(any())).thenReturn(constraint);
        when(dataValidationHelper.createValidation(eq(constraint), any())).thenReturn(dataValidation);
    }

    @Test
    @DisplayName("afterRowCreate：创建 8 个带边框单元格、第 8 列加下拉并合并两段区域")
    void afterRowCreate_buildsRow() {
        handler.afterRowCreate(writeSheetHolder, null, row, 0, false);

        verify(row, times(8)).createCell(anyInt());
        verify(cell, times(8)).setCellStyle(cellStyle);
        verify(cellStyle).setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        verify(cellStyle).setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        verify(cellStyle).setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        verify(cellStyle).setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        verify(dataValidationHelper).createExplicitListConstraint(any(String[].class));
        verify(sheet).addValidationData(dataValidation);
        verify(sheet, times(2)).addMergedRegion(any(CellRangeAddress.class));
    }

    @Test
    @DisplayName("beforeRowCreate / afterRowDispose：空实现不产生交互")
    void noOpCallbacks() {
        handler.beforeRowCreate(writeSheetHolder, null, 0, 0, false);
        handler.afterRowDispose(writeSheetHolder, null, row, 0, false);

        verify(row, times(0)).createCell(anyInt());
        verify(sheet, times(0)).addMergedRegion(any());
    }

    @Test
    @DisplayName("afterRowCreate：不同行号也按行号生成校验与合并区域")
    void afterRowCreate_otherRowIndex() {
        when(row.getRowNum()).thenReturn(9);
        Sheet otherSheet = mock(Sheet.class);
        when(row.getSheet()).thenReturn(otherSheet);
        when(otherSheet.getWorkbook()).thenReturn(workbook);
        when(otherSheet.getDataValidationHelper()).thenReturn(dataValidationHelper);
        when(writeSheetHolder.getSheet()).thenReturn(otherSheet);

        handler.afterRowCreate(writeSheetHolder, null, row, 1, false);

        verify(otherSheet).addValidationData(dataValidation);
        verify(otherSheet, times(2)).addMergedRegion(any(CellRangeAddress.class));
    }
}
