package com.example.employeetimetracking.service;
import com.example.employeetimetracking.dto.request.CreateUserRequestDto;
import com.example.employeetimetracking.dto.request.UserRequestDto;
import com.example.employeetimetracking.dto.response.*;
import com.example.employeetimetracking.exception.*;
import com.example.employeetimetracking.mapper.UserMapper;
import com.example.employeetimetracking.model.entities.*;
import com.example.employeetimetracking.model.enums.UserRole;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.specification.UserSpecifications;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder encoder;
    private final UserRepository userRepository;
    private final DepartmentService departmentService;
    private final LeaveBalanceService leaveBalanceService;
    @Autowired
    public UserService(UserRepository userRepository ,
                       DepartmentService departmentService,
                       BCryptPasswordEncoder encoder,
                       UserMapper userMapper,
                       LeaveBalanceService leaveBalanceService){
        this.userRepository = userRepository;
        this.departmentService = departmentService;
        this.encoder = encoder;
        this.userMapper = userMapper;
        this.leaveBalanceService = leaveBalanceService;
    }

    public User save(User user){
        return userRepository.save(user);
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().substring(0, 12);
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

        return pages.map(userMapper::toDto);
    }

    @Transactional
    public void deactivateUserById(Long id){
        User user = getById(id);
        if(!user.getIsActive()){
            throw new AccountDeactivatedException("User already deactivated");
        }
        user.setIsActive(false);
    }

    @Transactional
    public void activateUserById(Long id){
        User user = getById(id);
        if(user.getIsActive()){
            throw new AccountDeactivatedException("User already activated");
        }
        user.setIsActive(true);
    }

    public List<User> getAllByDepartment(Long id , boolean bool){
        return userRepository.findByDepartmentIdAndIsActive(id,bool);
    }

    public UserResponseDto getUserDetails(String email) {
        User user = getByEmail(email);
        return userMapper.toDto(user);
    }

    public UserResponseDto getUserDetails(Long id){
        User user = getById(id);
        return userMapper.toDto(user);
    }

    public UserResponseDto getUserDetails(User user){
        return userMapper.toDto(user);
    }

    public UserResponseDto getUserIfAllowed(Long id, User authenticatedUser, Collection<? extends GrantedAuthority> authorities) {
        try {
            User wantedUser = getById(id);

            Long managerId = wantedUser.getManager() != null ? wantedUser.getManager().getId() : null;
            boolean isHrAdmin = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_HR_ADMIN"));
            boolean isManager = Objects.equals(authenticatedUser.getId(), managerId);
            boolean isSameUser = Objects.equals(authenticatedUser.getId(), id);

            if (isHrAdmin || isManager || isSameUser) {
                return getUserDetails(wantedUser);
            }

            throw new AccessDeniedException("You cannot access this resource");

        } catch (UserNotFoundException | AccessDeniedException e) {
            throw new AccessDeniedException("You cannot access this resource");
        }
    }


    public UserResponseDto getUserIfAllowed(User authenticatedUser){
        return getUserDetails(authenticatedUser);
    }

    @Transactional
    public UserResponseDto updateUser(Long id , UserRequestDto userRequestDto){
        User wantedUser = getById(id);
        updateAllFields(wantedUser, userRequestDto);
        return getUserDetails(wantedUser);

    }

    public List<UserResponseDto> getTeamMembers(User authenticatedUser) {
        return authenticatedUser.getTeamMembers().stream()
                .map(userMapper::toDto)
                .toList();
    }

    private void updateAllFields(User wantedUser, UserRequestDto dto) {
        validateNewUserData(dto);
        User manager = getById(dto.getManagerId());
        validateManagerAssignment(wantedUser.getUserRole(),manager.getUserRole());

        if(dto.getUsername() != null) wantedUser.setUsername(dto.getUsername());
        if(dto.getEmail() != null) wantedUser.setEmail(dto.getEmail());
        if(dto.getFirstName() != null) wantedUser.setFirstName(dto.getFirstName());
        if(dto.getLastName() != null) wantedUser.setLastName(dto.getLastName());
        if(dto.getUserRole() != null) wantedUser.setUserRole(dto.getUserRole());
        if(dto.getDepartmentId() != null) wantedUser.setDepartment(departmentService.getById(dto.getDepartmentId()));
        if(dto.getManagerId() != null) {
            wantedUser.setManager(manager);
        }
    }

    @Transactional
    public UserCreatedResponse createUser(CreateUserRequestDto requestDto){

        validateNewUserData(requestDto);

        String tempPassword = generateTemporaryPassword();
        User user = createUserEntity(requestDto,tempPassword);
        User savedUser = userRepository.save(user);
        leaveBalanceService.initializeLeaveBalances(savedUser);

        return new UserCreatedResponse(userMapper.toDto(savedUser),tempPassword);

    }



    private void validateManagerAssignment(UserRole userRole , UserRole managerRole){
        boolean isIdOfManager = managerRole.equals(UserRole.MANAGER);
        boolean isIdOfHrAdmin = managerRole.equals(UserRole.HR_ADMIN);

        if (userRole == UserRole.EMPLOYEE && !isIdOfManager) {
            throw new InvalidEmployeeManagerException("Employee must have a manager with role MANAGER");
        }
        else if (userRole == UserRole.MANAGER && !isIdOfHrAdmin) {
            throw new InvalidManagerSupervisorException("Manager must have a supervisor with role HR_ADMIN");
        }
    }

    private User createUserEntity(CreateUserRequestDto requestDto,String tempPassword){
        User user = new User();
        User manager = getById(requestDto.getManagerId());

        validateManagerAssignment(requestDto.getUserRole(), manager.getUserRole());

        user.setManager(manager);

        user.setUsername(requestDto.getUsername());
        user.setEmail(requestDto.getEmail());
        user.setPasswordHash(encoder.encode(tempPassword));
        user.setFirstName(requestDto.getFirstName());
        user.setLastName(requestDto.getLastName());
        user.setUserRole(requestDto.getUserRole());
        user.setDepartment(departmentService.getById(requestDto.getDepartmentId()));

        user.setIsActive(true);
        return user;
    }

    private void validateNewUserData(CreateUserRequestDto requestDto){
        if(userRepository.existsByEmail(requestDto.getEmail())){
            throw new EmailAlreadyRegisteredException("user already exists with that email");
        }
        if(userRepository.existsByUsername(requestDto.getUsername())){
            throw new UsernameAlreadyExists("username unavailable");
        }
    }

    private void validateNewUserData(UserRequestDto requestDto){
        if(userRepository.existsByEmail(requestDto.getEmail())){
            throw new EmailAlreadyRegisteredException("user already exists with that email");
        }
        if(userRepository.existsByUsername(requestDto.getUsername())){
            throw new UsernameAlreadyExists("username unavailable");
        }
    }

    public List<UserResponseDto> searchUsers(Long departmentId , UserRole role , Boolean active , String name){
        if (name != null && name.isBlank()) {
            name = null;
        }
        Specification<User> spec = Specification
                .where(UserSpecifications.hasDepartmentId(departmentId))
                .and(UserSpecifications.hasRole(role))
                .and(UserSpecifications.isActive(active))
                .and(UserSpecifications.hasName(name));
        return userRepository.findAll(spec).stream().map(userMapper::toDto).toList();
    }








}
