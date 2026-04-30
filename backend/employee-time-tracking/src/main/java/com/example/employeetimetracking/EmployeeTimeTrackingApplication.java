package com.example.employeetimetracking;

import com.example.employeetimetracking.model.entities.Department;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.DepartmentRepository;
import com.example.employeetimetracking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authorization.method.AuthorizeReturnObject;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

@SpringBootApplication
public class EmployeeTimeTrackingApplication {
	private final UserRepository userRepository;
	private final BCryptPasswordEncoder encoder;
	private final DepartmentRepository departmentRepository;
	@Autowired
	public EmployeeTimeTrackingApplication(UserRepository userRepository ,BCryptPasswordEncoder encoder ,DepartmentRepository departmentRepository){
		this.userRepository = userRepository;
		this.encoder = encoder;
		this.departmentRepository = departmentRepository;
	}

	public static void main(String[] args) {
		SpringApplication.run(EmployeeTimeTrackingApplication.class, args);
	}

	//@Bean
	//CommandLineRunner myInitializer() {
	//	System.out.println(encoder.encode("password"));
	//	return args -> {
	//		if(userRepository.count() == 0) {
	//			Department defaultDepartment = new Department();
	//			defaultDepartment.setDepartmentName("Human Resources");
	//			defaultDepartment.setDepartmentCode("HR");
	//			departmentRepository.save(defaultDepartment);

	//			User adminUser = new User();
	//			adminUser.setUsername("adminadmin");
	//			adminUser.setEmail("adminadmin@example.com");
	//			adminUser.setPasswordHash(encoder.encode("password"));
	//			adminUser.setFirstName("Admin");
	//			adminUser.setLastName("User");
	//			adminUser.setUserRole(UserRole.HR_ADMIN);
	//			adminUser.setDepartment(defaultDepartment);
	//			adminUser.setIsActive(true);
	//			adminUser.setManager(null);

	//			userRepository.save(adminUser);
	//		}
	//	};
	//}
}
