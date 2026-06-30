package com.example.logistics.controller;

import com.example.logistics.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.YearMonth;

@Controller
public class ReportController {
    private final ReportService reportService;
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }
    @GetMapping("/reports/monthly")
    public String monthly(@RequestParam(required = false) String month, Model model) {
        String targetMonth = month == null ? YearMonth.now().toString() : month;
        model.addAttribute("month", targetMonth);
        model.addAttribute("rows", reportService.monthly(targetMonth));
        return "monthly-report";
    }
    @GetMapping("/reports/monthly/export")
    public void export(@RequestParam String month, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=monthly-freight-" + month + ".xlsx");
        reportService.exportMonthly(month, response.getOutputStream());
    }
}
