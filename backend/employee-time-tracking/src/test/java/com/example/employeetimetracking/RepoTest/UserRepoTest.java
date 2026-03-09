package com.example.employeetimetracking.RepoTest;


import com.example.employeetimetracking.exception.UserNotFoundException;
import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.DepartmentRepository;
import com.example.employeetimetracking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class UserRepoTest {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Autowired
    public UserRepoTest(UserRepository userRepository , DepartmentRepository departmentRepository){
        this.userRepository =  userRepository;
        this.departmentRepository = departmentRepository;

    }



    @Test
    void testSave(){

        User saved = userRepository.findByUsername("bob.dev").orElseThrow(() -> new UserNotFoundException("User not found"));

        System.out.println(saved.getEmail());
    }



}
