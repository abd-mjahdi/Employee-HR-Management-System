package com.example.employeetimetracking.jobs;

import com.example.employeetimetracking.model.entities.LeaveBalance;
import com.example.employeetimetracking.model.entities.LeavePolicy;
import com.example.employeetimetracking.model.entities.User;
import com.example.employeetimetracking.model.enums.AccrualMethod;
import com.example.employeetimetracking.repository.UserRepository;
import com.example.employeetimetracking.service.LeaveBalanceService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveAccrualScheduler {
    private final UserRepository userRepository;
    private final LeaveBalanceService leaveBalanceService;
    @Autowired
    public LeaveAccrualScheduler(UserRepository userRepository ,LeaveBalanceService leaveBalanceService){
        this.userRepository = userRepository;
        this.leaveBalanceService = leaveBalanceService;
    }
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void processMonthlyLeaveAccrual() {

        List<User> users = userRepository.findByIsActive(true);

        for (User user : users) {
            for (LeaveBalance lb : user.getLeaveBalanceList()) {

                    LeavePolicy policy = lb.getLeaveType().getLeavePolicy();

                    if (policy.getAccrualMethod() == AccrualMethod.ANNUAL) continue;

                    leaveBalanceService.applyMonthlyAccrual(lb);
                    // log changes in future
            }

        }
    }
}
