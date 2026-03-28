package com.example.employeetimetracking.integration;

import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.DepartmentRepository;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRespository;

    @Autowired
    protected DepartmentRepository departmentRepository;

    @Autowired
    protected BCryptPasswordEncoder encoder;

    @Autowired
    protected JwtUtil jwtUtil;

    protected String tokenFor(User user){
        return "Bearer "+ jwtUtil.generateJwtToken(user.getEmail() ,user.getId() , user.getUserRole() );
    }

    protected Department defaultDepartment;

    @BeforeEach
    void setUp() {
        defaultDepartment = createDepartment();
    }

    protected Department createDepartment(){
        Department department = new Department();
        department.setDepartmentCode("ENG");
        department.setDepartmentName("engineering");
        department.setIsActive(true);
        return departmentRepository.save(department);
    }

    protected User createHrAdmin(){
        User user =  new User();
        user.setUsername("admin1");
        user.setEmail("admin1@gmail.com");
        user.setFirstName("admin1");
        user.setLastName("admin1");
        user.setUserRole(UserRole.HR_ADMIN);
        user.setIsActive(true);
        user.setDepartment(defaultDepartment);
        user.setPasswordHash(encoder.encode("admin123"));
        return userRespository.save(user);
    }

    protected User createEmployee(){
        User user =  new User();
        user.setUsername("emp1");
        user.setEmail("emp1@gmail.com");
        user.setFirstName("emp1");
        user.setLastName("emp1");
        user.setUserRole(UserRole.EMPLOYEE);
        user.setIsActive(true);
        user.setDepartment(defaultDepartment);
        user.setPasswordHash(encoder.encode("emp123"));
        return userRespository.save(user);
    }

}
