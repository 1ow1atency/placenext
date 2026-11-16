package com.placenext.placenext.controller;

import com.placenext.placenext.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        model.addAttribute("report", reportService.build());
        return "admin/reports";
    }
}