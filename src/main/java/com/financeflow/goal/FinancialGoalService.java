package com.financeflow.goal;

import com.financeflow.common.exception.BusinessException;
import com.financeflow.common.exception.NotFoundException;
import com.financeflow.goal.dto.*;
import com.financeflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FinancialGoalService {

    private final FinancialGoalRepository goals;
    private final UserRepository users;

    public FinancialGoalService(FinancialGoalRepository goals, UserRepository users) {
        this.goals = goals;
        this.users = users;
    }

    @Transactional
    public GoalResponse create(Long userId, CreateGoalRequest req) {
        var user = users.findById(userId).orElseThrow();
        var g = FinancialGoal.builder()
                .user(user).name(req.name().trim())
                .targetAmount(req.targetAmount())
                .currentAmount(java.math.BigDecimal.ZERO)
                .targetDate(req.targetDate())
                .completed(false).build();
        goals.save(g);
        return GoalResponse.from(g);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(Long userId) {
        return goals.findAllByUserIdOrderByCompletedAscTargetDateAsc(userId).stream()
                .map(GoalResponse::from).toList();
    }

    @Transactional
    public GoalResponse contribute(Long userId, Long goalId, ContributeRequest req) {
        var g = goals.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new NotFoundException("Meta não encontrada"));
        if (Boolean.TRUE.equals(g.getCompleted())) {
            throw new BusinessException("Meta já concluída.");
        }
        g.setCurrentAmount(g.getCurrentAmount().add(req.amount()));
        if (g.getCurrentAmount().compareTo(g.getTargetAmount()) >= 0) {
            g.setCompleted(true);
        }
        return GoalResponse.from(g);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        var g = goals.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Meta não encontrada"));
        goals.delete(g);
    }
}