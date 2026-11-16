package com.example.apitest.service;

import com.example.apitest.entity.TestSuite;
import com.example.apitest.repository.TestSuiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** 定时巡检：每 30 秒扫描一次，对到期的用例集自动触发执行。 */
@Component
public class ScheduleScanner {

    private static final Logger log = LoggerFactory.getLogger(ScheduleScanner.class);

    private final TestSuiteRepository suiteRepository;
    private final ExecutionService executionService;

    public ScheduleScanner(TestSuiteRepository suiteRepository, ExecutionService executionService) {
        this.suiteRepository = suiteRepository;
        this.executionService = executionService;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void scan() {
        List<TestSuite> suites = suiteRepository.findAllScheduled();
        LocalDateTime now = LocalDateTime.now();
        for (TestSuite suite : suites) {
            if (suite.getCases().isEmpty()) {
                continue;
            }
            LocalDateTime last = suite.getLastRunAt();
            boolean due = last == null
                    || Duration.between(last, now).toMinutes() >= suite.getScheduleMinutes();
            if (due) {
                log.info("定时触发用例集: {}", suite.getName());
                suite.setLastRunAt(now);
                suiteRepository.save(suite);
                executionService.startRun(suite, true);
            }
        }
    }
}
