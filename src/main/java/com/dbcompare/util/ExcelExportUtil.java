package com.dbcompare.util;

import com.dbcompare.entity.DataRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelExportUtil {

    /**
     * 将数据记录导出到Excel文件
     */
    public static String exportToExcel(List<DataRecord> records, String tableName, String taskId) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Unmatched_" + tableName);

        // 创建标题行样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        if (!records.isEmpty()) {
            // 获取所有可能的列名
            java.util.Set<String> allColumns = new java.util.HashSet<>();
            for (DataRecord record : records) {
                allColumns.addAll(record.getFields().keySet());
            }

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            java.util.List<String> columnNames = new java.util.ArrayList<>(allColumns);
            for (int i = 0; i < columnNames.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnNames.get(i));
                cell.setCellStyle(headerStyle);
            }

            // 填充数据行
            for (int rowIndex = 0; rowIndex < records.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                DataRecord record = records.get(rowIndex);
                
                for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                    String columnName = columnNames.get(colIndex);
                    Cell cell = row.createCell(colIndex);
                    
                    Object value = record.getFields().get(columnName);
                    if (value != null) {
                        if (value instanceof String) {
                            cell.setCellValue((String) value);
                        } else if (value instanceof Number) {
                            if (value instanceof Integer || value instanceof Long) {
                                cell.setCellValue(((Number) value).doubleValue());
                            } else {
                                cell.setCellValue(((Number) value).doubleValue());
                            }
                        } else if (value instanceof Boolean) {
                            cell.setCellValue((Boolean) value);
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    } else {
                        cell.setCellValue("");
                    }
                }
            }

            // 自动调整列宽
            for (int i = 0; i < columnNames.size(); i++) {
                sheet.autoSizeColumn(i);
                // 限制最大宽度
                if (sheet.getColumnWidth(i) > 256 * 50) { // 50个字符的宽度
                    sheet.setColumnWidth(i, 256 * 50);
                }
            }
        }

        // 生成文件名
        String fileName = tableName + "_unmatched_" + taskId + ".xlsx";
        String filePath = System.getProperty("user.dir") + "/" + fileName;

        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            workbook.write(outputStream);
            workbook.close();
            System.out.println("Excel文件已导出到: " + filePath);
            return filePath;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Excel导出失败: " + e.getMessage());
        }
    }
}