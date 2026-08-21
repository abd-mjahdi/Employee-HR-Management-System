package com.example.employeetimetracking.jobs;

import com.example.employeetimetracking.model.entities.LeaveBalance;
import com.example.employeetimetracking.model.enums.AccrualMethod;
import com.example.employeetimetracking.repository.LeaveBalanceRepository;
import com.example.employeetimetracking.service.LeaveBalanceService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

@Service
public class LeaveAccrualScheduler {
    private static final Logger log = LoggerFactory.getLogger(LeaveAccrualScheduler.class);

    private final LeaveBalanceService leaveBalanceService;
    private final LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    public LeaveAccrualScheduler(LeaveBalanceService leaveBalanceService,
                                 LeaveBalanceRepository leaveBalanceRepository) {
        this.leaveBalanceService = leaveBalanceService;
        this.leaveBalanceRepository = leaveBalanceRepository;
    }

    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void processMonthlyLeaveAccrual() {
        int year = LocalDate.now().getYear();
        List<LeaveBalance> balances = leaveBalanceRepository.findAccrualCandidatesForYear(year);
        for (LeaveBalance balance : balances) {
            if (!isMonthly(balance)) {
                continue;
            }
            try {
                leaveBalanceService.applyMonthlyAccrual(balance);
            } catch (RuntimeException ex) {
                log.warn("Monthly accrual failed for leaveBalanceId={}", idOf(balance), ex);
            }
        }
    }

    @Scheduled(cron = "0 59 23 31 12 *")
    @Scheduled(cron = "0 5 0 1 1 *")
    @Transactional
    public void yearEndRolloverJob() {
        int sourceYear = rolloverSourceYear(LocalDate.now());
        List<LeaveBalance> balances = leaveBalanceRepository.findAccrualCandidatesForYear(sourceYear);
        for (LeaveBalance balance : balances) {
            try {
                leaveBalanceService.rolloverBalance(balance);
            } catch (RuntimeException ex) {
                log.warn("Year-end rollover failed for leaveBalanceId={}", idOf(balance), ex);
            }
        }
    }

    public static int rolloverSourceYear(LocalDate today) {
        if (today.getMonth() == Month.JANUARY) {
            return today.getYear() - 1;
        }
        return today.getYear();
    }

    private static boolean isMonthly(LeaveBalance lb) {
        return lb != null
                && lb.getLeaveType() != null
                && lb.getLeaveType().getLeavePolicy() != null
                && lb.getLeaveType().getLeavePolicy().getAccrualMethod() == AccrualMethod.MONTHLY;
    }

    private static Long idOf(LeaveBalance balance) {
        return balance == null ? null : balance.getId();
    }
}
