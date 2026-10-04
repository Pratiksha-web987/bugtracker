package com.aitracker.bugtracker.dto;

import java.util.List;

public class AutomatedTestReport {

    private String projectName;
    private String projectUrl;

    private int totalTests;
    private int passed;
    private int failed;

    private String overallStatus;

    private Long bugId;
    private String bugStatus;

    private List<AutomatedTestResult> tests;

    public AutomatedTestReport() {
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
    }

    public int getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }

    public int getPassed() {
        return passed;
    }

    public void setPassed(int passed) {
        this.passed = passed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public Long getBugId() {
        return bugId;
    }

    public void setBugId(Long bugId) {
        this.bugId = bugId;
    }

    public String getBugStatus() {
        return bugStatus;
    }

    public void setBugStatus(String bugStatus) {
        this.bugStatus = bugStatus;
    }

    public List<AutomatedTestResult> getTests() {
        return tests;
    }

    public void setTests(List<AutomatedTestResult> tests) {
        this.tests = tests;
    }
}