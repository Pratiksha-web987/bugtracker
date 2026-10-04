package com.aitracker.bugtracker.service;

import org.springframework.ai.chat.client.ChatClient;
import com.aitracker.bugtracker.entity.Bug;
import org.springframework.stereotype.Service;
import com.aitracker.bugtracker.entity.User;
import com.aitracker.bugtracker.entity.Role;
import com.aitracker.bugtracker.entity.BugPriority;
import com.aitracker.bugtracker.entity.BugSeverity;


@Service
public class AIService {

    private final ChatClient chatClient;

    public AIService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String analyzeBug(String title, String description) {

        // Local fallback/demo AI analysis
        String text = (title + " " + description).toLowerCase();

        String category;
        String severity;
        String priority;
        String fix;

        if (text.contains("login") || text.contains("password")
                || text.contains("authentication")) {

            category = "Authentication";
            severity = "MAJOR";
            priority = "HIGH";
            fix = "Check login validation, password handling, session management and authentication configuration.";

        } else if (text.contains("database") || text.contains("sql")
                || text.contains("data")) {

            category = "Database";
            severity = "MAJOR";
            priority = "HIGH";
            fix = "Check database connection, SQL queries, entity mappings and transaction handling.";

        } else if (text.contains("404")
                || text.contains("403")
                || text.contains("500")
                || text.contains("http")
                || text.contains("web")
                || text.contains("broken link")
                || text.contains("page not found")) {

            category = "Web / HTTP Error";
            severity = "MAJOR";
            priority = "HIGH";
            fix = "Check the requested URL, server routing, linked resources and HTTP response configuration.";

        } else if (text.contains("crash") || text.contains("error")
                || text.contains("exception")) {

            category = "Runtime Error";
            severity = "CRITICAL";
            priority = "HIGH";
            fix = "Check application logs, exception stack trace and the code path causing the runtime failure.";

        } else if (text.contains("ui") || text.contains("button")
                || text.contains("design") || text.contains("display")) {

            category = "UI/UX";
            severity = "MINOR";
            priority = "MEDIUM";
            fix = "Check frontend HTML, CSS, JavaScript and browser-side validation.";

        } else {

            category = "Functional";
            severity = "MAJOR";
            priority = "MEDIUM";
            fix = "Review the reported behavior, reproduce the issue and validate the affected business logic.";

        }

        return """
                AI Bug Analysis (Demo Mode)

                Category: %s
                Severity: %s
                Priority: %s

                Suggested Fix:
                %s
                """.formatted(
                category,
                severity,
                priority,
                fix
        );
    }
    public String detectDuplicateBug(
            String newTitle,
            String newDescription,
            java.util.List<Bug> existingBugs) {

        String newText = (newTitle + " " + newDescription)
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ");

        String[] newWords = newText.split("\\s+");

        Bug bestMatch = null;
        double bestScore = 0.0;

        for (Bug existingBug : existingBugs) {

            String existingText =
                    (existingBug.getTitle() + " "
                            + existingBug.getDescription())
                            .toLowerCase()
                            .replaceAll("[^a-z0-9 ]", " ");

            String[] existingWords = existingText.split("\\s+");

            int matchingWords = 0;

            for (String newWord : newWords) {

                if (newWord.length() < 3) {
                    continue;
                }

                for (String existingWord : existingWords) {

                    if (newWord.equals(existingWord)) {
                        matchingWords++;
                        break;
                    }
                }
            }

            if (newWords.length > 0) {

                double score =
                        ((double) matchingWords / newWords.length) * 100;

                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = existingBug;
                }
            }
        }

        if (bestMatch == null || bestScore < 40) {

            return """
                AI Duplicate Bug Detection

                Result: No strong duplicate found.

                Similarity: %.0f%%
                """.formatted(bestScore);
        }

        return """
            AI Duplicate Bug Detection

            Result: Possible Duplicate Found

            Existing Bug ID: %d
            Existing Bug Title: %s

            Similarity: %.0f%%

            Recommendation:
            Review the existing bug before creating a new bug.
            """.formatted(
                bestMatch.getId(),
                bestMatch.getTitle(),
                bestScore
        );
    }
    public String analyzeRootCause(String title, String description) {

        String text = (title + " " + description).toLowerCase();

        String rootCause;
        String reason;

        if (text.contains("login")
                || text.contains("password")
                || text.contains("authentication")
                || text.contains("session")) {

            rootCause =
                    "Authentication or session management issue";

            reason =
                    "The reported behavior is related to login validation, "
                            + "password handling, authentication configuration or session management.";

        } else if (text.contains("database")
                || text.contains("sql")
                || text.contains("query")
                || text.contains("data")) {

            rootCause =
                    "Database or data access issue";

            reason =
                    "The issue may be caused by database connectivity, "
                            + "SQL queries, entity mapping or incorrect data handling.";

        } else if (text.contains("404")
            || text.contains("403")
            || text.contains("500")
            || text.contains("http")
            || text.contains("web")
            || text.contains("broken link")
            || text.contains("page not found")) {

        rootCause =
                "Web page or HTTP response error";

        reason =
                "The external website returned an unsuccessful HTTP response "
                        + "or the requested web resource could not be accessed successfully.";

    } else if (text.contains("null")
            || text.contains("exception")
            || text.contains("crash")
            || text.contains("error")) {

            rootCause =
                    "Runtime exception or invalid application state";

            reason =
                    "The application may be receiving an unexpected value "
                            + "or executing a code path without proper error handling.";

        } else if (text.contains("button")
                || text.contains("ui")
                || text.contains("display")
                || text.contains("css")
                || text.contains("javascript")) {

            rootCause =
                    "Frontend UI or client-side logic issue";

            reason =
                    "The problem is likely related to HTML, CSS, JavaScript "
                            + "event handling or browser-side validation.";

        } else {

            rootCause =
                    "Business logic or functional implementation issue";

            reason =
                    "The reported behavior may be caused by incorrect "
                            + "application logic or an incomplete implementation of the expected workflow.";
        }

        return """
            AI Root-Cause Analysis (Demo Mode)

            Likely Root Cause: %s

            Analysis:
            %s

            Recommendation:
            Review the affected code path, reproduce the issue and verify the related application logic.
            """.formatted(rootCause, reason);
    }

    public String suggestFixAndDebuggingSteps(String title, String description) {

        String text = (title + " " + description).toLowerCase();

        String suggestedFix;
        String debuggingSteps;
        String components;
        String risk;

        if (text.contains("login")
                || text.contains("password")
                || text.contains("authentication")
                || text.contains("session")) {

            suggestedFix =
                    "Review authentication validation, password handling and session configuration.";

            debuggingSteps =
                    "1. Reproduce the login issue.\n"
                            + "2. Check authentication logs.\n"
                            + "3. Verify user credentials and validation logic.\n"
                            + "4. Check session/security configuration.\n"
                            + "5. Test login with valid and invalid credentials.";

            components =
                    "SecurityConfig, UserService, authentication logic and login frontend";

            risk = "MEDIUM";

        } else if (text.contains("database")
                || text.contains("sql")
                || text.contains("query")
                || text.contains("data")) {

            suggestedFix =
                    "Review database connectivity, SQL queries, entity mappings and transaction handling.";

            debuggingSteps =
                    "1. Reproduce the database issue.\n"
                            + "2. Check application and database logs.\n"
                            + "3. Verify database connection settings.\n"
                            + "4. Inspect repository queries and entity mappings.\n"
                            + "5. Test the affected database operation.";

            components =
                    "Repository, Entity, database configuration and Service layer";

            risk = "HIGH";

        } else if (text.contains("404")
                || text.contains("403")
                || text.contains("500")
                || text.contains("http")
                || text.contains("web")
                || text.contains("broken link")
                || text.contains("page not found")) {

            suggestedFix =
                    "Check the requested URL, server routing, linked resources and HTTP response configuration.";

            debuggingSteps =
                    "1. Open the reported URL directly in the browser.\n"
                            + "2. Verify the HTTP response status code.\n"
                            + "3. Check whether the requested page or resource exists.\n"
                            + "4. Inspect website routing, links and server configuration.\n"
                            + "5. Correct the URL or server configuration and retest.";

            components =
                    "Website URL, routing, links, server configuration and HTTP response handling";

            risk = "MEDIUM";

        } else if (text.contains("button")
                || text.contains("ui")
                || text.contains("css")
                || text.contains("javascript")
                || text.contains("display")) {

            suggestedFix =
                    "Review HTML structure, CSS rules and JavaScript event handling.";

            debuggingSteps =
                    "1. Reproduce the UI issue in the browser.\n"
                            + "2. Open browser developer tools.\n"
                            + "3. Inspect the affected HTML element.\n"
                            + "4. Check CSS and JavaScript errors.\n"
                            + "5. Test the corrected UI behavior.";

            components =
                    "HTML templates, CSS, JavaScript and browser-side logic";

            risk = "LOW";

        } else if (text.contains("crash")
                || text.contains("exception")
                || text.contains("error")
                || text.contains("null")) {

            suggestedFix =
                    "Inspect the exception stack trace and add proper validation and error handling.";

            debuggingSteps =
                    "1. Reproduce the application error.\n"
                            + "2. Check the complete stack trace.\n"
                            + "3. Identify the failing method or class.\n"
                            + "4. Validate input values and null handling.\n"
                            + "5. Retest the complete workflow.";

            components =
                    "Controller, Service layer, exception handling and application logs";

            risk = "HIGH";

        } else {

            suggestedFix =
                    "Reproduce the reported behavior and review the affected business logic.";

            debuggingSteps =
                    "1. Reproduce the reported bug.\n"
                            + "2. Identify the affected workflow.\n"
                            + "3. Check application logs.\n"
                            + "4. Inspect the related code path.\n"
                            + "5. Apply the fix and perform regression testing.";

            components =
                    "Controller, Service, Repository and related frontend components";

            risk = "MEDIUM";
        }

        return """
                AI Fix Suggestion & Debugging Guide (Demo Mode)

                Suggested Fix:
                %s

                Debugging Steps:
                %s

                Components to Inspect:
                %s

                Potential Risk:
                %s
                """.formatted(
                suggestedFix,
                debuggingSteps,
                components,
                risk
        );
    }
    public String recommendDeveloper(
            String title,
            String description,
            java.util.List<User> developers) {

        String text = (title + " " + description).toLowerCase();

        User recommended = null;

        for (User developer : developers) {

            if (developer.getRole() != Role.DEVELOPER) {
                continue;
            }

            // Simple demo AI matching
            if (text.contains("login")
                    || text.contains("password")
                    || text.contains("authentication")
                    || text.contains("security")) {

                recommended = developer;
                break;
            }

            if (text.contains("database")
                    || text.contains("sql")
                    || text.contains("query")) {

                recommended = developer;
                break;
            }

            if (text.contains("ui")
                    || text.contains("button")
                    || text.contains("css")
                    || text.contains("javascript")) {

                recommended = developer;
                break;
            }
        }

        // Fallback: first developer
        if (recommended == null) {
            recommended = developers.stream()
                    .filter(user -> user.getRole() == Role.DEVELOPER)
                    .findFirst()
                    .orElse(null);
        }

        if (recommended == null) {
            return """
                AI Developer Recommendation

                Result: No developer available.

                Please add at least one user with DEVELOPER role.
                """;
        }

        return """
            AI Developer Recommendation (Demo Mode)

            Recommended Developer: %s
            Developer Email: %s

            Reason:
            Based on the bug title and description, this developer is a suitable candidate for handling the reported issue.

            Recommendation:
            Project Manager should review the suggestion before assigning the bug.
            """.formatted(
                recommended.getName(),
                recommended.getEmail()
        );
    }
    public String autoTriageBug(String title, String description) {

        String text = (title + " " + description).toLowerCase();

        String category;
        BugSeverity severity;
        BugPriority priority;

        // HTTP / Website errors
        if (text.contains("404")
                || text.contains("not found")
                || text.contains("http error")
                || text.contains("status code")
                || text.contains("500")
                || text.contains("502")
                || text.contains("503")
                || text.contains("504")) {

            category = "Web / HTTP Error";
            severity = BugSeverity.MAJOR;
            priority = BugPriority.HIGH;

            // Authentication / Login
        } else if (text.contains("login")
                || text.contains("password")
                || text.contains("authentication")
                || text.contains("session")) {

            category = "Authentication";
            severity = BugSeverity.MAJOR;
            priority = BugPriority.HIGH;

            // Database
        } else if (text.contains("database")
                || text.contains("sql")
                || text.contains("query")
                || text.contains("data")) {

            category = "Database";
            severity = BugSeverity.MAJOR;
            priority = BugPriority.HIGH;

            // Runtime errors
        } else if (text.contains("404")
                || text.contains("403")
                || text.contains("500")
                || text.contains("http")
                || text.contains("web")
                || text.contains("broken link")
                || text.contains("page not found")) {

            category = "Web / HTTP Error";
            severity = BugSeverity.MAJOR;
            priority = BugPriority.HIGH;

        } else if (text.contains("crash")
                || text.contains("exception")
                || text.contains("fatal")
                || text.contains("system down")) {

            category = "Runtime Error";
            severity = BugSeverity.CRITICAL;
            priority = BugPriority.HIGH;

            // UI
        } else if (text.contains("button")
                || text.contains("ui")
                || text.contains("css")
                || text.contains("javascript")
                || text.contains("display")
                || text.contains("layout")) {

            category = "UI/UX";
            severity = BugSeverity.MINOR;
            priority = BugPriority.MEDIUM;

            // Default
        } else {

            category = "Functional";
            severity = BugSeverity.MAJOR;
            priority = BugPriority.MEDIUM;
        }

        return category + "|" + severity.name() + "|" + priority.name();
    }
}