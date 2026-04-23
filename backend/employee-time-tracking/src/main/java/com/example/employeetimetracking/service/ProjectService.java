package com.example.employeetimetracking.service;

import com.example.employeetimetracking.exception.ProjectNotFoundException;
import com.example.employeetimetracking.model.entities.Project;
import com.example.employeetimetracking.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    @Autowired
    public ProjectService(ProjectRepository projectRepository){
        this.projectRepository = projectRepository;
    }

    public Project getById(Long id){
        return projectRepository.findById(id).orElseThrow(()-> new ProjectNotFoundException("project not found with id: "+id));
    }
}
