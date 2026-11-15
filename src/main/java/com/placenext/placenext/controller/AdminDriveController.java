package com.placenext.placenext.controller;

import com.placenext.placenext.service.DriveService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/drives")
public class AdminDriveController {

    private final DriveService driveService;

    public AdminDriveController(DriveService driveService) {
        this.driveService = driveService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pending", driveService.pending());
        model.addAttribute("drives", driveService.all());
        return "admin/drives";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable("id") Long id, RedirectAttributes redirect) {
        driveService.approve(id);
        redirect.addFlashAttribute("message", "Drive approved. Eligible students can now see it.");
        return "redirect:/admin/drives";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable("id") Long id,
                         @RequestParam(value = "reason", required = false) String reason,
                         RedirectAttributes redirect) {
        driveService.reject(id, reason);
        redirect.addFlashAttribute("message", "Drive rejected. The recruiter will see your reason.");
        return "redirect:/admin/drives";
    }
}