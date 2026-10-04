package com.aitracker.bugtracker.controller;

import com.aitracker.bugtracker.entity.Project;
import com.aitracker.bugtracker.repository.ProjectRepository;
import com.aitracker.bugtracker.service.ExternalTestingService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.aitracker.bugtracker.dto.AutomatedTestReport;

@Controller
@RequestMapping("/external-testing")
public class ExternalTestingController {

    private final ExternalTestingService externalTestingService;
    private final ProjectRepository projectRepository;

    public ExternalTestingController(
            ExternalTestingService externalTestingService,
            ProjectRepository projectRepository) {

        this.externalTestingService = externalTestingService;
        this.projectRepository = projectRepository;
    }

    @GetMapping("/{projectId}")
    public String testingPage(
            @PathVariable Long projectId,
            org.springframework.ui.Model model) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        model.addAttribute("project", project);

        return "external-testing";
    }

    @RequestMapping(
            value = "/run/{projectId}",
            method = {RequestMethod.GET, RequestMethod.POST}
    )
    @ResponseBody
    public String runTest(@PathVariable Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        return externalTestingService.runBrowserTest(project);
    }
    @GetMapping("/run-report/{projectId}")
    @ResponseBody
    public AutomatedTestReport runTestReport(@PathVariable Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        return externalTestingService.runBrowserTestReport(project);
    }
}