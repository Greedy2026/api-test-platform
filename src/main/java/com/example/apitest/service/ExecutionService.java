package com.example.apitest.service;

import com.example.apitest.entity.ApiCase;
import com.example.apitest.entity.CaseResult;
import com.example.apitest.entity.ExecutionRun;
import com.example.apitest.entity.TestSuite;
import com.example.apitest.repository.CaseResultRepository;
import com.example.apitest.repository.ExecutionRunRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 执行服务：把用例集导出为 JSON -> 调用 Python 引擎执行 -> 解析结果入库。
 * Java 平台与 Python 引擎解耦，引擎可独立升级或水平扩展。
 */
@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private final CaseResultRepository caseResultRepository;
    private final ExecutionRunRepository runRepository;
    private final String pythonPath;

    public ExecutionService(CaseResultRepository caseResultRepository,
                            ExecutionRunRepository runRepository,
                            @Value("${app.engine.python-path:python}") String pythonPath) {
        this.caseResultRepository = caseResultRepository;
        this.runRepository = runRepository;
        this.pythonPath = pythonPath;
    }

    /** 异步触发一次执行，立即返回 run 记录（status=RUNNING），前端可轮询。 */
    public ExecutionRun startRun(TestSuite suite, boolean scheduled) {
        ExecutionRun run = new ExecutionRun();
        run.setSuite(suite);
        run.setOwner(suite.getOwner());
        run.setStatus("RUNNING");
        run.setScheduled(scheduled);
        run.setTotalCases(suite.getCases().size());
        run = runRepository.save(run);

        ExecutionRun finalRun = run;
        executor.submit(() -> execute(finalRun, suite));
        return run;
    }

    private void execute(ExecutionRun run, TestSuite suite) {
        Path input = null;
        Path output = null;
        try {
            input = Files.createTempFile("engine-input-" + run.getId(), ".json");
            output = Files.createTempFile("engine-output-" + run.getId(), ".json");
            writeInput(input, suite);

            ProcessBuilder pb = new ProcessBuilder(
                    pythonPath, engineScript(), input.toString(), output.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            if (!process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new IllegalStateException("引擎执行超时");
            }
            int exit = process.exitValue();
            if (exit != 0) {
                throw new IllegalStateException("引擎退出码非 0: " + exit);
            }

            ingestResults(run, output);
        } catch (Exception e) {
            log.error("执行失败 runId={}", run.getId(), e);
            run.setStatus("ERROR");
            run.setFinishedAt(LocalDateTime.now());
            runRepository.save(run);
        } finally {
            quietlyDelete(input);
            quietlyDelete(output);
        }
    }

    private void writeInput(Path input, TestSuite suite) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("runId", suite.getName()); // 引擎只透传，不依赖
        ArrayNode cases = root.putArray("cases");
        for (ApiCase c : suite.getCases()) {
            ObjectNode node = cases.addObject();
            node.put("caseId", c.getId());
            node.put("name", c.getName());
            node.put("method", c.getMethod());
            node.put("url", c.getUrl());
            node.put("headersJson", c.getHeaders() == null ? "{}" : c.getHeaders());
            node.put("body", c.getBody() == null ? "" : c.getBody());
            node.put("assertionsJson", c.getAssertions() == null ? "[]" : c.getAssertions());
            node.put("timeoutSeconds", c.getTimeoutSeconds() == null ? 10 : c.getTimeoutSeconds());
        }
        Files.writeString(input, mapper.writeValueAsString(root));
    }

    private void ingestResults(ExecutionRun run, Path output) throws IOException {
        JsonNode root = mapper.readTree(Files.readString(output));
        int passed = 0;
        int failed = 0;
        run.getResults().clear();
        for (JsonNode r : root.withArray("results")) {
            CaseResult cr = new CaseResult();
            cr.setCaseName(r.path("name").asText("(unknown)"));
            cr.setSuccess(r.path("success").asBoolean(false));
            if (r.hasNonNull("status") && r.get("status").isInt()) {
                cr.setResponseStatus(r.get("status").asInt());
            }
            if (r.hasNonNull("durationMs")) {
                cr.setDurationMs(r.get("durationMs").asLong());
            }
            cr.setResponsePreview(truncate(r.path("responsePreview").asText(""), 1800));
            cr.setMessage(truncate(r.path("message").asText(""), 1800));
            cr.setRun(run);
            caseResultRepository.save(cr);
            run.getResults().add(cr);
            if (cr.isSuccess()) {
                passed++;
            } else {
                failed++;
            }
        }
        run.setPassedCases(passed);
        run.setFailedCases(failed);
        run.setStatus(failed > 0 ? "FAILED" : "SUCCESS");
        run.setFinishedAt(LocalDateTime.now());
        runRepository.save(run);
        log.info("执行完成 runId={} passed={} failed={}", run.getId(), passed, failed);
    }

    private String engineScript() {
        // 引擎位于项目根目录 engine/runner.py；打包运行时由工作目录决定
        return System.getProperty("app.engine.script", "engine/runner.py");
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }

    private void quietlyDelete(Path p) {
        if (p == null) {
            return;
        }
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
        }
    }
}
