package com.placenext.placenext.controller;

import com.placenext.placenext.model.Application;
import com.placenext.placenext.model.Branch;
import com.placenext.placenext.model.OfferStatus;
import com.placenext.placenext.model.StudentProfile;
import com.placenext.placenext.repository.ApplicationRepository;
import com.placenext.placenext.repository.StudentProfileRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class AdminStudentController {

    private final StudentProfileRepository studentProfileRepository;
    private final ApplicationRepository applicationRepository;

    public AdminStudentController(StudentProfileRepository studentProfileRepository,
                                  ApplicationRepository applicationRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping("/admin/students")
    public String students(@RequestParam(value = "q", required = false) String q,
                           @RequestParam(value = "branch", required = false) Branch branch,
                           @RequestParam(value = "placed", required = false) String placed,
                           Model model) {

        // Ids of students who have accepted an offer
        Set<Long> placedIds = new HashSet<>();
        for (Application a : applicationRepository.findByOfferStatusOrderByRespondedAtDesc(OfferStatus.ACCEPTED)) {
            placedIds.add(a.getStudent().getId());
        }

        String needle = q == null ? "" : q.trim().toLowerCase();

        List<StudentProfile> students = studentProfileRepository.findAll().stream()
                .filter(s -> needle.isEmpty()
                        || contains(s.getUser().getFullName(), needle)
                        || contains(s.getUser().getEmail(), needle)
                        || contains(s.getRollNo(), needle))
                .filter(s -> branch == null || s.getBranch() == branch)
                .filter(s -> placed == null || placed.isBlank()
                        || ("placed".equals(placed) && placedIds.contains(s.getId()))
                        || ("unplaced".equals(placed) && !placedIds.contains(s.getId())))
                .toList();

        model.addAttribute("students", students);
        model.addAttribute("placedIds", placedIds);
        model.addAttribute("branches", Branch.values());
        model.addAttribute("q", q);
        model.addAttribute("branchFilter", branch == null ? "" : branch.name());
        model.addAttribute("placedFilter", placed == null ? "" : placed);
        return "admin/students";
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}