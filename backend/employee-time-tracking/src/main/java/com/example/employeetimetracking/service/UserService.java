package com.example.employeetimetracking.service;
import com.example.employeetimetracking.dto.response.DepartmentDto;
import com.example.employeetimetracking.dto.response.UserDto;
import com.example.employeetimetracking.exception.AccountDeactivatedException;
import com.example.employeetimetracking.exception.UserNotFoundException;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    @Autowired
    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public User getByEmail(String email){
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public User getById(Long id){
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public Page<UserDto> getAll(Pageable p){
        Page<User> pages = userRepository.findAll(p);

        return pages.map(user->convertToDto(user));
    }

    public User save(User user){
        return userRepository.save(user);
    }

    public void deactivateUserById(Long id){
        User user = getById(id);
        if(!user.getIsActive()){
            throw new AccountDeactivatedException("User already deactivated");
        }
        user.setIsActive(false);
    }

    public List<User> getAllByDepartment(Long id , boolean bool){
        return userRepository.findByDepartmentIdAndIsActive(id,bool);
    }

    public UserDto getUserDetails(String email) {
        User user = getByEmail(email);
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
