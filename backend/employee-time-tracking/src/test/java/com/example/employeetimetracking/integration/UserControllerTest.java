package com.example.employeetimetracking.integration;

import com.example.employeetimetracking.model.entities.User;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerTest extends BaseIntegrationTest {
    @Test
    void getUsers_asHrAdmin_returns200WithList() throws Exception {
        User admin = createHrAdmin();
        createEmployee();

        mockMvc.perform(get("/users")
                        .header("Authorization", tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getUsers_asEmployee_returns403() throws Exception {
        User employee = createEmployee();

        mockMvc.perform(get("/users")
                        .header("Authorization", tokenFor(employee)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMe_returnsCurrentUserProfile() throws Exception {
        User employee = createEmployee();

        mockMvc.perform(get("/users/me")
                        .header("Authorization", tokenFor(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(employee.getEmail()))
                .andExpect(jsonPath("$.password").doesNotExist()); // dont expose password!
    }

    @Test
    void createUser_asHrAdmin_savesToDatabase() throws Exception {
        User admin = createHrAdmin();
        User manager = createManager();
        Long deptId = defaultDepartment.getId();
        Long managerId = manager.getId();
        mockMvc.perform(post("/users")
                        .header("Authorization", tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                {
                  "username": "omarali",
                  "email": "newemployee@test.com",
                  "firstName": "ali",
                  "lastName": "omar",
                  "userRole": "EMPLOYEE",
                  "departmentId": %d,
                  "managerId": %d
                }
            """, deptId ,managerId)))
                .andExpect(status().isCreated()).andDo(print());

        assertTrue(userRepository.findByEmail("newemployee@test.com").isPresent());
    }

    @Test
    void deactivateUser_asHrAdmin_setsIsActiveToFalse() throws Exception {
        User hrAdmin = createHrAdmin();
        User employee = createEmployee();
        mockMvc.perform(patch("/users/"+employee.getId()+"/deactivate")
                .header("Authorization",tokenFor(hrAdmin))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.findByEmail(employee.getEmail()).get().getIsActive());
    }
    @Test
    void deactivateUser_asHrAdmin_doesNotChangeAlreadyInactiveUser() throws Exception {
        User hrAdmin = createHrAdmin();
        User employee = createEmployee();
        employee.setIsActive(false);
        userRepository.save(employee);
        mockMvc.perform(patch("/users/"+employee.getId()+"/deactivate")
                        .header("Authorization",tokenFor(hrAdmin))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        assertFalse(userRepository.findByEmail(employee.getEmail()).get().getIsActive());
    }
}
