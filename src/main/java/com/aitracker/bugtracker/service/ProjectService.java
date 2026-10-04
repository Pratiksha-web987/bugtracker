package com.aitracker.bugtracker.service;

import com.aitracker.bugtracker.entity.Project;
import com.aitracker.bugtracker.entity.ProjectStatus;
import com.aitracker.bugtracker.entity.ProjectType;
import com.aitracker.bugtracker.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // Create Project
    public Project createProject(String name,
                                 String description,
                                 ProjectType projectType,
                                 String projectUrl,
                                 java.time.LocalDate startDate,
                                 java.time.LocalDate endDate) {

        Project project = new Project();

        project.setName(name);
        project.setDescription(description);
        project.setProjectType(projectType);
        project.setProjectUrl(projectUrl);
        project.setStartDate(startDate);
        project.setEndDate(endDate);
        project.setStatus(ProjectStatus.PLANNING);

        return projectRepository.save(project);
    }

    // Get all projects
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // Get project by ID
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));
    }

    // Delete project
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Project not found");
        }

        projectRepository.deleteById(id);
    }
    // Update Project
    public Project updateProject(
            Long id,
            String name,
            String description,
            ProjectType projectType,
            String projectUrl,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            ProjectStatus status) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        project.setName(name);
        project.setDescription(description);
        project.setProjectType(projectType);
        project.setProjectUrl(projectUrl);
        project.setStartDate(startDate);
        project.setEndDate(endDate);
        project.setStatus(status);

        return projectRepository.save(project);
    }
}