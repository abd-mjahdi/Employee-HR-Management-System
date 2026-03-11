package com.example.employeetimetracking.service;
import com.example.employeetimetracking.dto.request.UserRequestDto;
import com.example.employeetimetracking.dto.response.DepartmentDto;
import com.example.employeetimetracking.dto.response.UserResponseDto;
import com.example.employeetimetracking.exception.AccountDeactivatedException;
import com.example.employeetimetracking.exception.UserNotFoundException;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final DepartmentService departmentService;
    @Autowired
    public UserService(UserRepository userRepository ,DepartmentService departmentService){
        this.userRepository = userRepository;
        this.departmentService = departmentService;
    }

    public boolean existsByEmail(String email){
        return userRepository.existsByEmail(email);
    }

    public User getByEmail(String email){
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public User getById(Long id){
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public Page<UserResponseDto> getAll(Pageable p){
        Page<User> pages = userRepository.findAll(p);

        return pages.map(this::convertToDto);
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

    public UserResponseDto getUserDetails(String email) {
        User user = getByEmail(email);
        return convertToDto(user);
    }

    public UserResponseDto getUserDetails(Long id){
        User user = getById(id);
        return convertToDto(user);
    }

    public UserResponseDto getUserDetails(User user){
        return convertToDto(user);
    }

    public UserResponseDto getUserIfAllowed(Long id , User authenticatedUser , Collection<? extends GrantedAuthority> authorities){
        User wantedUser = getById(id);
        Long managerId = wantedUser.getManager()!=null ? wantedUser.getManager().getId() : null ;

        boolean isHrAdmin = authorities.stream().anyMatch(authority->authority.getAuthority().equals("ROLE_HR_ADMIN"));
        boolean isManager = Objects.equals(authenticatedUser.getId() , managerId);
        boolean isSameUser = Objects.equals(authenticatedUser.getId(),id);

        if(isHrAdmin || isManager || isSameUser){
            return getUserDetails(wantedUser);
        }
        throw new AccessDeniedException("You cannot access this resource");
    }

    public UserResponseDto getUserIfAllowed(User authenticatedUser){
        return getUserDetails(authenticatedUser);
    }

    public UserResponseDto updateUserIfAllowed(Long id , UserRequestDto userRequestDto, User authenticatedUser , Collection<? extends GrantedAuthority> authorities){
        User wantedUser = getById(id);
        Long managerId = wantedUser.getManager()!=null ? wantedUser.getManager().getId() : null ;

        boolean isHrAdmin = authorities.stream().anyMatch(authority->authority.getAuthority().equals("ROLE_HR_ADMIN"));
        boolean isManager = Objects.equals(authenticatedUser.getId() , managerId);
        boolean isSameUser = Objects.equals(authenticatedUser.getId(),id);

        if(isHrAdmin){
            updateAllFields(wantedUser , userRequestDto);
        }else if(isManager){
            updateManagerAllowedFields(wantedUser, userRequestDto);
        }else if(isSameUser){
            updateSelfAllowedFields(wantedUser, userRequestDto);
        } else {
            throw new AccessDeniedException("Not authorized to update this user");
        }

        userRepository.save(wantedUser);

        return getUserDetails(wantedUser);

    }

    private void updateAllFields(User wantedUser , UserRequestDto userRequestDto){
        wantedUser.setUsername(userRequestDto.getUsername());
        wantedUser.setEmail(userRequestDto.getEmail());
        wantedUser.setFirstName(userRequestDto.getFirstName());
        wantedUser.setLastName(userRequestDto.getLastName());
        wantedUser.setUserRole(userRequestDto.getUserRole());
        wantedUser.setDepartment(departmentService.getById(userRequestDto.getDepartmentId()));
        wantedUser.setIsActive(userRequestDto.getIsActive());
    }

    private void updateManagerAllowedFields(User wantedUser, UserRequestDto userRequestDto){
        wantedUser.setFirstName(userRequestDto.getFirstName());
        wantedUser.setLastName(userRequestDto.getLastName());
    }

    private void updateSelfAllowedFields(User wantedUser, UserRequestDto userRequestDto){
        wantedUser.setFirstName(userRequestDto.getFirstName());
        wantedUser.setLastName(userRequestDto.getLastName());
    }

    private UserResponseDto convertToDto(User user) {
        DepartmentDto deptDto = new DepartmentDto(
                user.getDepartment().getId(),
                user.getDepartment().getDepartmentName(),
                user.getDepartment().getDepartmentCode(),
                user.getDepartment().getIsActive()
        );

        return new UserResponseDto(
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
