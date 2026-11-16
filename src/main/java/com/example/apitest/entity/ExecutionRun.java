package com.example.apitest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 一次执行：包含若干用例结果。 */
@Entity
@Table(name = "execution_runs")
public class ExecutionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** RUNNING / SUCCESS / FAILED / ERROR */
    @Column(nullable = false, length = 20)
    private String status;

    private int totalCases;
    private int passedCases;
    private int failedCases;

    private boolean scheduled;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "suite_id")
    private TestSuite suite;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "run", fetch = FetchType.EAGER)
    private List<CaseResult> results = new ArrayList<>();

    private LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime finishedAt;

    public Long getId() { return id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalCases() { return totalCases; }
    public void setTotalCases(int totalCases) { this.totalCases = totalCases; }
    public int getPassedCases() { return passedCases; }
    public void setPassedCases(int passedCases) { this.passedCases = passedCases; }
    public int getFailedCases() { return failedCases; }
    public void setFailedCases(int failedCases) { this.failedCases = failedCases; }
    public boolean isScheduled() { return scheduled; }
    public void setScheduled(boolean scheduled) { this.scheduled = scheduled; }
    public TestSuite getSuite() { return suite; }
    public void setSuite(TestSuite suite) { this.suite = suite; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public List<CaseResult> getResults() { return results; }
    public void setResults(List<CaseResult> results) { this.results = results; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
