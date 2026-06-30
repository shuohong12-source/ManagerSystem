package com.example.logistics.service;

import com.example.logistics.mapper.ReportMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {
    private final ReportMapper reportMapper;

    public ReportService(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    public List<Map<String, Object>> monthly(String month) {
        return reportMapper.monthly(month);
    }

    public void exportMonthly(String month, OutputStream outputStream) throws IOException {
        List<Map<String, Object>> rows = monthly(month);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("月度物流费用");
            Row header = sheet.createRow(0);
            String[] titles = {"月份", "目的枢纽", "目的城市", "状态", "运单数", "商品数量", "总运费"};
            for (int i = 0; i < titles.length; i++) {
                header.createCell(i).setCellValue(titles[i]);
            }
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> data = rows.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(String.valueOf(data.get("month_value")));
                row.createCell(1).setCellValue(String.valueOf(data.get("destination_region")));
                row.createCell(2).setCellValue(String.valueOf(data.get("destination_city")));
                row.createCell(3).setCellValue(String.valueOf(data.get("status")));
                row.createCell(4).setCellValue(((Number) data.get("waybill_count")).doubleValue());
                row.createCell(5).setCellValue(((Number) data.get("goods_quantity")).doubleValue());
                Object freight = data.get("freight_amount");
                row.createCell(6).setCellValue(freight instanceof BigDecimal bd ? bd.doubleValue() : 0D);
            }
            for (int i = 0; i < titles.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
        }
    }
}
