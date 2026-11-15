package com.placenext.placenext.controller;

import com.placenext.placenext.dto.CreateRecruiterForm;
import com.placenext.placenext.repository.RecruiterProfileRepository;
import com.placenext.placenext.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/recruiters")
public class AdminRecruiterController {

    private final UserService userService;
    private final RecruiterProfileRepository recruiterProfileRepository;

    public AdminRecruiterController(UserService userService, RecruiterProfileRepository recruiterProfileRepository) {
        this.userService = userService;
        this.recruiterProfileRepository = recruiterProfileRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("form", new CreateRecruiterForm());
        model.addAttribute("recruiters", recruiterProfileRepository.findAllByOrderByIdDesc());
        return "admin/recruiters";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CreateRecruiterForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirect) {
        if (!result.hasFieldErrors("email") && userService.emailExists(form.getEmail())) {
            result.rejectValue("email", "duplicate", "An account with this email already exists");
        }
        if (result.hasErrors()) {
            model.addAttribute("recruiters", recruiterProfileRepository.findAllByOrderByIdDesc());
            return "admin/recruiters";
        }
        userService.createRecruiter(form);
        redirect.addFlashAttribute("created", form.getEmail().toLowerCase().trim());
        return "redirect:/admin/recruiters";
    }
}