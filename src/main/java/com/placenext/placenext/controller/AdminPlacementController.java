package com.placenext.placenext.controller;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.OfferStatus;
import com.placenext.placenext.repository.ApplicationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AdminPlacementController {

    private final ApplicationRepository applicationRepository;

    public AdminPlacementController(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @GetMapping("/admin/placements")
    public String placements(Model model) {
        List<Application> placed =
                applicationRepository.findByOfferStatusOrderByRespondedAtDesc(OfferStatus.ACCEPTED);
        model.addAttribute("placements", placed);
        return "admin/placements";
    }
}