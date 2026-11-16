package com.example.apitest.controller;

import com.example.apitest.entity.ExecutionRun;
import com.example.apitest.repository.ExecutionRunRepository;
import com.example.apitest.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final ExecutionRunRepository runRepository;
    private final CurrentUserService currentUser;

    public RunController(ExecutionRunRepository runRepository, CurrentUserService currentUser) {
        this.runRepository = runRepository;
        this.currentUser = currentUser;
    }

    /** 当前用户的所有执行记录（含结果明细），前端轮询获取最新状态。 */
    @GetMapping
    public List<ExecutionRun> list() {
        return runRepository.findByOwnerIdOrderByIdDesc(currentUser.get().getId());
    }

    @GetMapping("/{id}")
    public ExecutionRun detail(@PathVariable Long id) {
        return runRepository.findById(id).orElse(null);
    }
}
