package com.example.employeetimetracking.service;

import com.example.employeetimetracking.dto.response.LeaveBalanceDto;
import com.example.employeetimetracking.exception.LeaveBalanceNotFoundException;
import com.example.employeetimetracking.exception.NegativeLeaveBalanceException;
import com.example.employeetimetracking.exception.UserNotFoundException;
import com.example.employeetimetracking.mapper.LeaveBalanceMapper;
import com.example.employeetimetracking.model.entities.*;
import com.example.employeetimetracking.model.enums.AccrualMethod;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class LeaveBalanceService {
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;
    private final LeaveTypeService leaveTypeService;
    private final LeaveBalanceMapper leaveBalanceMapper;
    @Autowired
    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository,
                               LeaveTypeService leaveTypeService,
                               LeaveBalanceMapper leaveBalanceMapper,
                               UserRepository userRepository
                               ){
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeService = leaveTypeService;
        this.leaveBalanceMapper = leaveBalanceMapper;
        this.userRepository = userRepository;
    }

    public LeaveBalance save(LeaveBalance balance){
        return leaveBalanceRepository.save(balance);
    }

    @Transactional
    public void initializeLeaveBalances(User user){
        List<LeaveType> leaveTypes = leaveTypeService.getAllWithPolicy();

        for(LeaveType leaveType : leaveTypes){
            LeavePolicy policy = leaveType.getLeavePolicy();
            LeaveBalance balance = new LeaveBalance();
            balance.setUser(user);
            balance.setLeaveType(leaveType);
            short year =(short) LocalDate.now().getYear();
            balance.setYear(year);

            if(policy.getAccrualMethod().equals(AccrualMethod.ANNUAL)){
                balance.setCurrentBalance(policy.getAnnualAllocation());
            }else{
                balance.setCurrentBalance(BigDecimal.ZERO);
            }
            balance.setLastAccrualDate(LocalDate.now());
            save(balance);
        }
    }
    @Transactional
    public void applyMonthlyAccrual(LeaveBalance lb) {
        LeavePolicy policy = lb.getLeaveType().getLeavePolicy();
        LocalDate today = LocalDate.now();
        if (lb.getYear() != today.getYear()) {
            return;
        }
        if (lb.getLastAccrualDate() != null) {
            LocalDate thisMonthStart = today.withDayOfMonth(1);
            if (!lb.getLastAccrualDate().isBefore(thisMonthStart)) {
                return;
            }
        }

        BigDecimal monthlyRate = policy.getAnnualAllocation()
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        lb.setCurrentBalance(lb.getCurrentBalance().add(monthlyRate));
        lb.setLastAccrualDate(today);
    }

    @Transactional
    public void rolloverBalance(LeaveBalance currentYearBalance) {
        int currentYear = LocalDate.now().getYear();
        if (currentYearBalance.getYear() != currentYear) {
            return;
        }

        int nextYear = currentYear + 1;
        boolean nextYearExists = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(
                        currentYearBalance.getUser().getId(),
                        currentYearBalance.getLeaveType().getId(),
                        nextYear
                )
                .isPresent();
        if (nextYearExists) {
            return;
        }

        LeavePolicy policy = currentYearBalance.getLeaveType().getLeavePolicy();
        BigDecimal carry = currentYearBalance.getCurrentBalance();
        if (policy.getMaxRolloverDays() != null) {
            carry = carry.min(policy.getMaxRolloverDays());
        }
        if (carry.compareTo(BigDecimal.ZERO) < 0) {
            carry = BigDecimal.ZERO;
        }

        BigDecimal nextYearBalanceValue = carry;
        if (policy.getAccrualMethod() == AccrualMethod.ANNUAL) {
            nextYearBalanceValue = nextYearBalanceValue.add(policy.getAnnualAllocation());
        }

        LeaveBalance nextYearBalance = new LeaveBalance();
        nextYearBalance.setUser(currentYearBalance.getUser());
        nextYearBalance.setLeaveType(currentYearBalance.getLeaveType());
        nextYearBalance.setYear((short) nextYear);
        nextYearBalance.setCurrentBalance(nextYearBalanceValue);
        nextYearBalance.setLastAccrualDate(LocalDate.of(nextYear, 1, 1));
        save(nextYearBalance);
    }

    private LeaveBalance getLeaveBalance(User user, LeaveRequest lr) {
        int currentYear = LocalDate.now().getYear();
        return leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(user.getId(), lr.getLeaveType().getId(), currentYear)
                .orElseThrow(() -> new LeaveBalanceNotFoundException(
                        "Leave balance not found for userId=" + user.getId() +
                                ", leaveTypeId=" + lr.getLeaveType().getId() +
                                ", year=" + currentYear
                ));
    }

    @Transactional
    public void deductLeaveBalance(LeaveRequest lr ,User user){
        LeaveBalance lb = getLeaveBalance(user, lr);
        BigDecimal newBalance = lb.getCurrentBalance().subtract(lr.getTotalDays());
        if (newBalance.compareTo(BigDecimal.ZERO) < 0 &&
                !lb.getLeaveType().getLeavePolicy().getAllowsNegativeBalance()) {
            throw new NegativeLeaveBalanceException("Insufficient leave balance");
        }

        lb.setCurrentBalance(newBalance);

    }

    @Transactional
    public void restoreLeaveBalance(LeaveRequest lr ,User user){
        LeaveBalance lb = getLeaveBalance(user, lr);
        lb.setCurrentBalance(lb.getCurrentBalance().add(lr.getTotalDays()));
    }

    @Transactional
    public void setLeaveBalance(LeaveRequest lr ,User user ,BigDecimal value){
        LeaveBalance lb = getLeaveBalance(user, lr);
        LeavePolicy policy = lb.getLeaveType().getLeavePolicy();

        if (value.compareTo(BigDecimal.ZERO) < 0 && !policy.getAllowsNegativeBalance()) {
            throw new NegativeLeaveBalanceException("Negative Balance is not allowed for this leave type");
        }
        if (value.compareTo(policy.getAnnualAllocation()) > 0) {
            lb.setCurrentBalance(policy.getAnnualAllocation());
            return;
        }
        lb.setCurrentBalance(value);
    }

    public List<LeaveBalanceDto> getByUserIdAndYear(Long userId ,int year){
        List<LeaveBalance> leaveBalances = leaveBalanceRepository.findByUserIdAndYear(userId,year);
        return leaveBalances.stream().map(leaveBalanceMapper::toDto).toList();
    }

    public List<LeaveBalanceDto> getLeaveBalanceIfAllowed(Long userId, CustomUserDetails authUser) {
        try {
            User target = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

            Long managerId = target.getManager() != null ? target.getManager().getId() : null;

            boolean isHrAdmin = authUser.hasRole("HR_ADMIN");
            boolean isManager = Objects.equals(authUser.getId(), managerId);

            if (isHrAdmin || isManager)
                return getByUserIdAndYear(userId, LocalDate.now().getYear());
            throw new AccessDeniedException("You cannot access this resource");

        } catch (UserNotFoundException | AccessDeniedException e) {
            throw new AccessDeniedException("You cannot access this resource");
        }
    }

    public LeaveBalance getByUserIdAndLeaveTypeIdAndYear(Long userId, Long leaveTypeId ,int year){
        return leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(userId, leaveTypeId, year)
                .orElseThrow(()->new LeaveBalanceNotFoundException("Leave balance not found"));
    }

}
