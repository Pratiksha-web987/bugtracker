package com.aitracker.bugtracker.controller;

import com.aitracker.bugtracker.entity.Bug;
import com.aitracker.bugtracker.entity.BugPriority;
import com.aitracker.bugtracker.entity.BugSeverity;
import com.aitracker.bugtracker.entity.BugStatus;
import com.aitracker.bugtracker.service.BugService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.util.List;

@Controller
public class ReportController {

    private final BugService bugService;

    public ReportController(BugService bugService) {
        this.bugService = bugService;
    }

    @GetMapping("/reports")
    public String reports(Model model) {

        List<Bug> bugs = bugService.getAllBugs();

        // Total
        model.addAttribute("totalBugs", bugs.size());

        // Status counts
        model.addAttribute("openBugs",
                bugService.getBugsByStatus(BugStatus.OPEN));

        model.addAttribute("triagedBugs",
                bugService.getBugsByStatus(BugStatus.TRIAGED));

        model.addAttribute("assignedBugs",
                bugService.getBugsByStatus(BugStatus.ASSIGNED));

        model.addAttribute("inProgressBugs",
                bugService.getBugsByStatus(BugStatus.IN_PROGRESS));

        model.addAttribute("fixedBugs",
                bugService.getBugsByStatus(BugStatus.FIXED));

        model.addAttribute("retestingBugs",
                bugService.getBugsByStatus(BugStatus.RETESTING));

        model.addAttribute("closedBugs",
                bugService.getBugsByStatus(BugStatus.CLOSED));

        model.addAttribute("reopenedBugs",
                bugService.getBugsByStatus(BugStatus.REOPENED));

        // Priority counts
        model.addAttribute(
                "lowPriority",
                bugs.stream()
                        .filter(b -> b.getPriority() == BugPriority.LOW)
                        .count()
        );

        model.addAttribute(
                "mediumPriority",
                bugs.stream()
                        .filter(b -> b.getPriority() == BugPriority.MEDIUM)
                        .count()
        );

        model.addAttribute(
                "highPriority",
                bugs.stream()
                        .filter(b -> b.getPriority() == BugPriority.HIGH)
                        .count()
        );

        // Severity counts
        model.addAttribute(
                "minorSeverity",
                bugs.stream()
                        .filter(b -> b.getSeverity() == BugSeverity.MINOR)
                        .count()
        );

        model.addAttribute(
                "majorSeverity",
                bugs.stream()
                        .filter(b -> b.getSeverity() == BugSeverity.MAJOR)
                        .count()
        );

        model.addAttribute(
                "criticalSeverity",
                bugs.stream()
                        .filter(b -> b.getSeverity() == BugSeverity.CRITICAL)
                        .count()
        );

        // Complete bug list
        model.addAttribute("bugs", bugs);

        return "reports";

    }
    @GetMapping("/reports/export")
    public void exportCsv(HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=bug-report.csv"
        );

        List<Bug> bugs = bugService.getAllBugs();

        var writer = response.getWriter();

        writer.println(
                "Bug ID,Title,Project,Reported By,Assigned To,Priority,Severity,Status,Category,Created At"
        );

        for (Bug bug : bugs) {

            String projectName =
                    bug.getProject() != null
                            ? bug.getProject().getName()
                            : "";

            String reportedBy =
                    bug.getReportedBy() != null
                            ? bug.getReportedBy().getName()
                            : "";

            String assignedTo =
                    bug.getAssignedTo() != null
                            ? bug.getAssignedTo().getName()
                            : "";

            writer.println(
                    csv(bug.getId()) + "," +
                            csv(bug.getTitle()) + "," +
                            csv(projectName) + "," +
                            csv(reportedBy) + "," +
                            csv(assignedTo) + "," +
                            csv(bug.getPriority()) + "," +
                            csv(bug.getSeverity()) + "," +
                            csv(bug.getStatus()) + "," +
                            csv(bug.getCategory()) + "," +
                            csv(bug.getCreatedAt())
            );
        }

        writer.flush();
    }

    private String csv(Object value) {

        if (value == null) {
            return "\"\"";
        }

        String text = String.valueOf(value)
                .replace("\"", "\"\"");

        return "\"" + text + "\"";
    }
}