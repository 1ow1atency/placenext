package com.placenext.placenext.controller;

import com.placenext.placenext.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
public class AdminExportController {

    private final ExportService exportService;

    public AdminExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/admin/export/students")
    public ResponseEntity<byte[]> students() {
        return csv("students", exportService.studentsCsv());
    }

    @GetMapping("/admin/export/placements")
    public ResponseEntity<byte[]> placements() {
        return csv("placements", exportService.placementsCsv());
    }

    @GetMapping("/admin/export/report")
    public ResponseEntity<byte[]> report() {
        return csv("placement-report", exportService.reportCsv());
    }

    private ResponseEntity<byte[]> csv(String name, String content) {
        // BOM so Excel opens the file as UTF-8 (keeps the ₹ sign intact)
        byte[] body = ("\uFEFF" + content).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + name + "-" + LocalDate.now() + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}