package com.example.apitest.controller;

import com.example.apitest.entity.ApiCase;
import com.example.apitest.entity.ExecutionRun;
import com.example.apitest.entity.TestSuite;
import com.example.apitest.entity.User;
import com.example.apitest.repository.ApiCaseRepository;
import com.example.apitest.repository.ExecutionRunRepository;
import com.example.apitest.repository.TestSuiteRepository;
import com.example.apitest.service.CurrentUserService;
import com.example.apitest.service.ExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/suites")
public class SuiteController {

    private final TestSuiteRepository suiteRepository;
    private final ApiCaseRepository caseRepository;
    private final ExecutionRunRepository runRepository;
    private final ExecutionService executionService;
    private final CurrentUserService currentUser;

    public SuiteController(TestSuiteRepository suiteRepository,
                           ApiCaseRepository caseRepository,
                           ExecutionRunRepository runRepository,
                           ExecutionService executionService,
                           CurrentUserService currentUser) {
        this.suiteRepository = suiteRepository;
        this.caseRepository = caseRepository;
        this.runRepository = runRepository;
        this.executionService = executionService;
        this.currentUser = currentUser;
    }

    public record SuiteDto(String name, String description, List<Long> caseIds, Integer scheduleMinutes) {}

    @GetMapping
    public List<TestSuite> list() {
        return suiteRepository.findByOwnerIdOrderByIdDesc(currentUser.get().getId());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody SuiteDto dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "用例集名称不能为空"));
        }
        User user = currentUser.get();
        Set<ApiCase> cases = loadOwnedCases(dto.caseIds(), user.getId());
        if (cases.size() != (dto.caseIds() == null ? 0 : dto.caseIds().size())) {
            return ResponseEntity.badRequest().body(Map.of("error", "包含不存在的用例"));
        }
        TestSuite suite = new TestSuite();
        suite.setName(dto.name());
        suite.setDescription(dto.description());
        suite.setCases(cases);
        suite.setScheduleMinutes(normalizeSchedule(dto.scheduleMinutes()));
        suite.setOwner(user);
        return ResponseEntity.ok(suiteRepository.save(suite));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody SuiteDto dto) {
        TestSuite suite = ownedSuite(id);
        if (suite == null) {
            return ResponseEntity.notFound().build();
        }
        User user = currentUser.get();
        Set<ApiCase> cases = loadOwnedCases(dto.caseIds(), user.getId());
        suite.setName(dto.name());
        suite.setDescription(dto.description());
        suite.setCases(cases);
        suite.setScheduleMinutes(normalizeSchedule(dto.scheduleMinutes()));
        return ResponseEntity.ok(suiteRepository.save(suite));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        TestSuite suite = ownedSuite(id);
        if (suite == null) {
            return ResponseEntity.notFound().build();
        }
        suiteRepository.delete(suite);
        return ResponseEntity.noContent().build();
    }

    /** 手动触发执行 */
    @PostMapping("/{id}/run")
    public ResponseEntity<?> run(@PathVariable Long id) {
        TestSuite suite = ownedSuite(id);
        if (suite == null) {
            return ResponseEntity.notFound().build();
        }
        if (suite.getCases().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "用例集为空，请先添加用例"));
        }
        suite.setLastRunAt(java.time.LocalDateTime.now());
        suiteRepository.save(suite);
        ExecutionRun run = executionService.startRun(suite, false);
        return ResponseEntity.ok(Map.of("runId", run.getId(), "status", run.getStatus()));
    }

    /** 某用例集的历史执行记录 */
    @GetMapping("/{id}/runs")
    public List<ExecutionRun> runs(@PathVariable Long id) {
        return runRepository.findBySuiteIdOrderByIdDesc(id);
    }

    private TestSuite ownedSuite(Long id) {
        TestSuite suite = suiteRepository.findById(id).orElse(null);
        if (suite == null || !suite.getOwner().getId().equals(currentUser.get().getId())) {
            return null;
        }
        return suite;
    }

    private Set<ApiCase> loadOwnedCases(List<Long> ids, Long ownerId) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        List<ApiCase> cases = caseRepository.findAllById(ids);
        return cases.stream()
                .filter(c -> c.getOwner().getId().equals(ownerId))
                .collect(java.util.stream.Collectors.toSet());
    }

    private int normalizeSchedule(Integer m) {
        if (m == null || m < 0) {
            return 0;
        }
        return Math.min(m, 1440);
    }
}
