package com.example.employeetimetracking.service;
import com.example.employeetimetracking.dto.response.DepartmentDto;
import com.example.employeetimetracking.dto.response.UserDto;
import com.example.employeetimetracking.exception.UserNotFoundException;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    @Autowired
    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public User findByEmail(String email){
        return userRepository.findByEmail(email);
    }

    public UserDto getCurrentUserDetails(String email) {
        User user = userRepository.findByEmail(email);

        if(user == null) {
            throw new UserNotFoundException("User not found");
        }

        return convertToDto(user);
    }

    private UserDto convertToDto(User user) {
        DepartmentDto deptDto = new DepartmentDto(
                user.getDepartment().getId(),
                user.getDepartment().getDepartmentName(),
                user.getDepartment().getDepartmentCode(),
                user.getDepartment().getIsActive()
        );

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getUserRole(),
                deptDto,
                user.getIsActive()
        );
    }


}
