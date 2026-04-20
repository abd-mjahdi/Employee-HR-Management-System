package com.example.employeetimetracking.jobs;

import com.example.employeetimetracking.model.entities.LeaveBalance;
import com.example.employeetimetracking.model.enums.AccrualMethod;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.service.LeaveBalanceService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveAccrualScheduler {
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveBalanceRepository leaveBalanceRepository;
    @Autowired
    public LeaveAccrualScheduler( LeaveBalanceService leaveBalanceService , LeaveBalanceRepository leaveBalanceRepository){
        this.leaveBalanceService = leaveBalanceService;
        this.leaveBalanceRepository = leaveBalanceRepository;
    }
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void processMonthlyLeaveAccrual() {
        int currentYear = LocalDate.now().getYear();
        List<LeaveBalance> lbs = leaveBalanceRepository.findByYear(currentYear);
        lbs.stream()
                .filter(lb->lb.getLeaveType().getLeavePolicy().getAccrualMethod()==AccrualMethod.MONTHLY)
                .forEach(leaveBalanceService::applyMonthlyAccrual);
    }

    @Scheduled(cron = "0 59 23 31 12 *")
    @Transactional
    public void yearEndRolloverJob() {
        int currentYear = LocalDate.now().getYear();
        List<LeaveBalance> lbs = leaveBalanceRepository.findByYear(currentYear);
        lbs.forEach(leaveBalanceService::rolloverBalance);

    }

}
