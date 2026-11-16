package com.example.apitest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 单条用例的执行结果。 */
@Entity
@Table(name = "case_results")
public class CaseResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用例名快照（防止用例改名后报告对不上） */
    @Column(nullable = false, length = 100)
    private String caseName;

    private boolean success;

    private Integer responseStatus;
    private Long durationMs;

    @Column(length = 2000)
    private String responsePreview;

    /** 失败原因 / 断言差异 */
    @Column(length = 2000)
    private String message;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id")
    private ExecutionRun run;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getCaseName() { return caseName; }
    public void setCaseName(String caseName) { this.caseName = caseName; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getResponsePreview() { return responsePreview; }
    public void setResponsePreview(String responsePreview) { this.responsePreview = responsePreview; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public ExecutionRun getRun() { return run; }
    public void setRun(ExecutionRun run) { this.run = run; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
