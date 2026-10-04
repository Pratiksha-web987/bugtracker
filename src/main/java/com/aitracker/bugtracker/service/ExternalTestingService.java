package com.aitracker.bugtracker.service;

import com.aitracker.bugtracker.entity.Project;
import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.entity.Bug;
import com.aitracker.bugtracker.repository.UserRepository;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;
import java.net.HttpURLConnection;
import java.net.URL;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import java.util.logging.Level;
import com.aitracker.bugtracker.dto.AutomatedTestReport;
import com.aitracker.bugtracker.dto.AutomatedTestResult;
import java.util.ArrayList;
import com.aitracker.bugtracker.entity.ProjectMember;
import com.aitracker.bugtracker.entity.ProjectMemberRole;
import com.aitracker.bugtracker.repository.ProjectMemberRepository;
import java.util.List;

@Service
public class ExternalTestingService {

    private final BugService bugService;
    private AutomatedTestReport lastReport;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ExternalTestingService(
            BugService bugService,
            UserRepository userRepository,
            ProjectMemberRepository projectMemberRepository) {

        this.bugService = bugService;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    public String runBrowserTest(Project project) {
        List<AutomatedTestResult> testResults = new ArrayList<>();

        if (project.getProjectType() == null ||
                !project.getProjectType().name().equals("EXTERNAL")) {

            return "❌ This is not an external project.";
        }

        if (project.getProjectUrl() == null ||
                project.getProjectUrl().isBlank()) {

            return "❌ External project URL is missing.";
        }

        WebDriver driver = null;

        try {

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--start-maximized");

            LoggingPreferences logs = new LoggingPreferences();
            logs.enable(LogType.BROWSER, Level.ALL);

            options.setCapability("goog:loggingPrefs", logs);

            driver = new ChromeDriver(options);

            driver.get(project.getProjectUrl());

            String title = driver.getTitle();

            StringBuilder result = new StringBuilder();
            StringBuilder failures = new StringBuilder();

            result.append("AUTOMATED WEBSITE TEST REPORT\n");
            result.append("==============================\n\n");

            result.append("URL: ")
                    .append(project.getProjectUrl())
                    .append("\n");

            result.append("Page Title: ")
                    .append(title)
                    .append("\n\n");

            int passed = 0;
            int failed = 0;

            // TEST 1 - PAGE LOAD + HTTP STATUS
            result.append("TEST 1 - Page Load & HTTP Status\n");

            try {

                URL url = new URL(project.getProjectUrl());

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setInstanceFollowRedirects(true);

                int responseCode = connection.getResponseCode();

                result.append("HTTP Status Code: ")
                        .append(responseCode)
                        .append("\n");

                if (responseCode >= 200 && responseCode < 400) {

                    result.append("PASS: Website loaded with a successful HTTP status.\n\n");
                    passed++;

                    testResults.add(
                            new AutomatedTestResult(
                                    "HTTP Status Validation",
                                    "PASS",
                                    "Website returned HTTP " + responseCode
                            )
                    );

                } else {

                    result.append("FAIL: Website returned HTTP error status ")
                            .append(responseCode)
                            .append(".\n\n");

                    failures.append("HTTP error status detected: ")
                            .append(responseCode)
                            .append("\n");

                    failed++;

                    testResults.add(
                            new AutomatedTestResult(
                                    "HTTP Status Validation",
                                    "FAIL",
                                    "Website returned HTTP " + responseCode
                            )
                    );
                }

                connection.disconnect();

            } catch (Exception e) {

                result.append("FAIL: Unable to verify HTTP status.\n");

                result.append("Error: ")
                        .append(e.getMessage())
                        .append("\n\n");

                failures.append("HTTP connectivity/status check failed: ")
                        .append(e.getMessage())
                        .append("\n");

                failed++;

                testResults.add(
                        new AutomatedTestResult(
                                "HTTP Status Validation",
                                "FAIL",
                                "Unable to verify HTTP status: " + e.getMessage()
                        )
                );
            }
            // TEST 2 - PAGE TITLE
            result.append("TEST 2 - Page Title\n");

            if (title != null && !title.isBlank()) {

                result.append("PASS: Page title is present.\n");
                result.append("Title: ")
                        .append(title)
                        .append("\n\n");

                passed++;
                testResults.add(
                        new AutomatedTestResult(
                                "Page Title Validation",
                                "PASS",
                                "Page title detected successfully."
                        )
                );

            } else {

                result.append("FAIL: Page title is missing.\n\n");
                failures.append("Page title is missing.\n");
                failed++;
                testResults.add(
                        new AutomatedTestResult(
                                "Page Title Validation",
                                "FAIL",
                                "Page title is missing or empty."
                        )
                );
            }

            // TEST 3 - IMAGES
            result.append("TEST 3 - Image Validation\n");

            List<WebElement> images =
                    driver.findElements(By.tagName("img"));

            int brokenImages = 0;

            for (WebElement image : images) {

                Boolean loaded = (Boolean)
                        ((org.openqa.selenium.JavascriptExecutor) driver)
                                .executeScript(
                                        "return arguments[0].complete && " +
                                                "arguments[0].naturalWidth > 0;",
                                        image
                                );

                if (!Boolean.TRUE.equals(loaded)) {
                    brokenImages++;
                }
            }

            if (brokenImages == 0) {

                result.append("PASS: No broken images detected.\n");
                result.append("Images checked: ")
                        .append(images.size())
                        .append("\n\n");

                passed++;
                testResults.add(
                        new AutomatedTestResult(
                                "Image Validation",
                                "PASS",
                                "All images loaded successfully."
                        )
                );

            } else {

                result.append("FAIL: ")
                        .append(brokenImages)
                        .append(" broken image(s) detected.\n");
                result.append("Images checked: ")
                        .append(images.size())
                        .append("\n\n");

                failures.append(brokenImages)
                        .append(" broken image(s) detected.\n");

                failed++;
                testResults.add(
                        new AutomatedTestResult(
                                "Image Validation",
                                "FAIL",
                                "One or more images failed to load."
                        )
                );
            }

            // TEST 4 - LINK VALIDATION
            result.append("TEST 4 - Link Validation\n");

            List<WebElement> links =
                    driver.findElements(By.tagName("a"));

            int brokenLinks = 0;
            int checkedLinks = 0;

            for (WebElement link : links) {

                String href = link.getAttribute("href");

                if (href == null ||
                        href.isBlank() ||
                        href.equals("#")) {

                    brokenLinks++;

                    failures.append(
                                    "Empty or invalid link detected: "
                            ).append(href)
                            .append("\n");

                    continue;
                }

                // Only test HTTP/HTTPS links
                if (!href.startsWith("http://") &&
                        !href.startsWith("https://")) {

                    continue;
                }

                try {

                    URL linkUrl = new URL(href);

                    HttpURLConnection connection =
                            (HttpURLConnection) linkUrl.openConnection();

                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.setInstanceFollowRedirects(true);

                    int responseCode =
                            connection.getResponseCode();

                    checkedLinks++;

                    if (responseCode >= 400) {

                        brokenLinks++;

                        failures.append(
                                        "Broken link detected: "
                                ).append(href)
                                .append(" (HTTP ")
                                .append(responseCode)
                                .append(")\n");
                    }

                    connection.disconnect();

                } catch (Exception e) {

                    brokenLinks++;

                    failures.append(
                                    "Unable to access link: "
                            ).append(href)
                            .append("\n");
                }
            }

            if (brokenLinks == 0) {

                result.append(
                        "PASS: No broken links detected.\n"
                );

                result.append("Links found: ")
                        .append(links.size())
                        .append("\n");

                result.append("HTTP links checked: ")
                        .append(checkedLinks)
                        .append("\n\n");

                passed++;
                testResults.add(
                        new AutomatedTestResult(
                                "Link Validation",
                                "PASS",
                                "All checked links are valid."
                        )
                );

            } else {

                result.append("FAIL: ")
                        .append(brokenLinks)
                        .append(" broken/invalid link(s) detected.\n");

                result.append("Links found: ")
                        .append(links.size())
                        .append("\n");

                result.append("HTTP links checked: ")
                        .append(checkedLinks)
                        .append("\n\n");

                failed++;
                testResults.add(
                        new AutomatedTestResult(
                                "Link Validation",
                                "FAIL",
                                "One or more links are broken or invalid."
                        )
                );
            }

            // TEST 5 - BUTTONS
            result.append("TEST 5 - Button Validation\n");

            List<WebElement> buttons =
                    driver.findElements(By.tagName("button"));

            int invalidButtons = 0;

            for (WebElement button : buttons) {

                String text = button.getText();
                String ariaLabel =
                        button.getAttribute("aria-label");

                if ((text == null || text.isBlank()) &&
                        (ariaLabel == null || ariaLabel.isBlank())) {

                    invalidButtons++;
                }
            }

            if (invalidButtons == 0) {

                result.append("PASS: Buttons have identifiable text/labels.\n");
                result.append("Buttons checked: ")
                        .append(buttons.size())
                        .append("\n\n");

                passed++;
                testResults.add(
                        new AutomatedTestResult(
                                "Button Validation",
                                "PASS",
                                "Buttons were detected successfully."
                        )
                );

            } else {

                result.append("FAIL: ")
                        .append(invalidButtons)
                        .append(" button(s) have no text or accessible label.\n");
                result.append("Buttons checked: ")
                        .append(buttons.size())
                        .append("\n\n");

                failures.append(invalidButtons)
                        .append(" button(s) have no text or accessible label.\n");

                failed++;
                testResults.add(
                        new AutomatedTestResult(
                                "Button Validation",
                                "FAIL",
                                "No usable buttons were detected."
                        )
                );
            }

            // TEST 6 - FORMS
            result.append("TEST 6 - Form Validation\n");

            List<WebElement> forms =
                    driver.findElements(By.tagName("form"));

            int invalidForms = 0;

            for (WebElement form : forms) {

                List<WebElement> inputs =
                        form.findElements(By.cssSelector(
                                "input, textarea, select"
                        ));

                boolean hasRequiredField = false;

                for (WebElement input : inputs) {

                    String required =
                            input.getAttribute("required");

                    if (required != null) {
                        hasRequiredField = true;
                        break;
                    }
                }

                if (!inputs.isEmpty() && !hasRequiredField) {
                    invalidForms++;
                }
            }

            if (invalidForms == 0) {

                result.append("PASS: Forms passed the basic validation check.\n");
                result.append("Forms checked: ")
                        .append(forms.size())
                        .append("\n\n");

                passed++;
                testResults.add(
                        new AutomatedTestResult(
                                "Form Validation",
                                "PASS",
                                "Forms were detected and checked successfully."
                        )
                );

            } else {

                result.append("WARNING: ")
                        .append(invalidForms)
                        .append(" form(s) have no required field.\n");

                result.append("Forms checked: ")
                        .append(forms.size())
                        .append("\n\n");

                passed++;
                testResults.add(
                        new AutomatedTestResult(
                                "Form Validation",
                                "PASS",
                                "Forms were checked successfully."
                        )
                );
            }

            // TEST 7 - JAVASCRIPT ERROR VALIDATION
            result.append("TEST 7 - JavaScript Error Validation\n");

            var browserLogs = driver.manage()
                    .logs()
                    .get(LogType.BROWSER);

            int jsErrors = 0;

            for (var logEntry : browserLogs) {

                if ("SEVERE".equalsIgnoreCase(logEntry.getLevel().getName())) {

                    String message = logEntry.getMessage();

                    // Ignore browser resource errors caused by HTTP failures.
                    // These are already detected by TEST 1 and TEST 4.
                    if (message != null &&
                            message.toLowerCase().contains("failed to load resource")) {
                        continue;
                    }
                    result.append("JavaScript Error: ")
                            .append(logEntry.getMessage())
                            .append("\n");

                    failures.append("JavaScript error detected: ")
                            .append(logEntry.getMessage())
                            .append("\n");
                }
            }

            if (jsErrors == 0) {

                result.append(
                        "PASS: No JavaScript errors detected.\n\n"
                );

                passed++;
                testResults.add(
                        new AutomatedTestResult(
                                "JavaScript Error Check",
                                "PASS",
                                "No critical JavaScript errors were detected."
                        )
                );

            } else {

                result.append("FAIL: ")
                        .append(jsErrors)
                        .append(" JavaScript error(s) detected.\n\n");

                failed++;
                testResults.add(
                        new AutomatedTestResult(
                                "JavaScript Error Check",
                                "FAIL",
                                "Critical JavaScript errors were detected."
                        )
                );
            }

            // SUMMARY

            AutomatedTestReport report = new AutomatedTestReport();
            lastReport = report;

            report.setProjectName(project.getName());
            report.setProjectUrl(project.getProjectUrl());

            report.setTotalTests(testResults.size());
            report.setPassed(passed);
            report.setFailed(failed);

            report.setOverallStatus(
                    failed > 0 ? "FAILED" : "PASSED"
            );

            report.setTests(testResults);

            result.append("==============================\n");
            result.append("TEST SUMMARY\n");
            result.append("==============================\n");


            result.append("Total Tests: ")
                    .append(passed + failed)
                    .append("\n");

            result.append("Passed: ")
                    .append(passed)
                    .append("\n");

            result.append("Failed: ")
                    .append(failed)
                    .append("\n");


            // AUTOMATIC BUG CREATION
            if (failed > 0) {

                Long bugId = System.currentTimeMillis();

                List<ProjectMember> testers =
                        projectMemberRepository.findByProjectAndMemberRole(
                                project,
                                ProjectMemberRole.TESTER
                        );

                if (!testers.isEmpty()) {

                    User reporter = testers.get(0).getUser();
                    String bugTitle =
                            "Automated Test Failure - "
                                    + project.getName();

                    String bugDescription =
                            "Selenium automated testing detected "
                                    + failed
                                    + " potential issue(s) on the external website.\n\n"
                                    + "URL: "
                                    + project.getProjectUrl()
                                    + "\n\n"
                                    + "Detected Issues:\n"
                                    + failures;

                    bugService.createAutomatedBug(
                            bugId,
                            bugTitle,
                            bugDescription,
                            project.getId(),
                            reporter.getId()
                    );
                    report.setBugId(bugId);
                    report.setBugStatus("TRIAGED");

                    result.append("\n🐞 AUTOMATIC BUG CREATED\n");
                    result.append("Bug ID: ")
                            .append(bugId)
                            .append("\n");
                    result.append("Status: TRIAGED\n");

                } else {

                    result.append("\n⚠️ Test failed, Test failed, but no tester is assigned to this project.\n");
                }

                result.append("\n⚠️ AUTOMATED TESTING FOUND ")
                        .append(failed)
                        .append(" POTENTIAL ISSUE(S)");

            } else {

                result.append("\n✅ AUTOMATED TESTING COMPLETED SUCCESSFULLY");
            }

            return result.toString();

        } catch (Exception e) {

            /*
             * Browser/network failure itself becomes a bug.
             */

            String errorMessage =
                    e.getMessage() == null
                            ? "Unknown browser automation error"
                            : e.getMessage();

            Long bugId = System.currentTimeMillis();

            List<ProjectMember> testers =
                    projectMemberRepository.findByProjectAndMemberRole(
                            project,
                            ProjectMemberRole.TESTER
                    );

            if (!testers.isEmpty()) {

                User reporter = testers.get(0).getUser();
                String bugTitle =
                        "Automated Browser Test Failed - "
                                + project.getName();

                String bugDescription =
                        "Selenium was unable to complete the automated "
                                + "browser test.\n\n"
                                + "URL: "
                                + project.getProjectUrl()
                                + "\n\n"
                                + "Error:\n"
                                + errorMessage;

                bugService.createAutomatedBug(
                        bugId,
                        bugTitle,
                        bugDescription,
                        project.getId(),
                        reporter.getId()
                );

                return "❌ Automated browser test failed.\n\n"
                        + "🐞 Automatic Bug Created\n"
                        + "Bug ID: "
                        + bugId
                        + "\n\n"
                        + "Error: "
                        + errorMessage;
            }

            return "❌ Automated browser test failed.\n"
                    + "Error: "
                    + errorMessage;

        } finally {

            if (driver != null) {
                driver.quit();
            }
        }
    }public AutomatedTestReport runBrowserTestReport(Project project) {

        runBrowserTest(project);

        return lastReport;
    }

    public String checkConnectivity(Project project) {
        return runBrowserTest(project);
    }
}