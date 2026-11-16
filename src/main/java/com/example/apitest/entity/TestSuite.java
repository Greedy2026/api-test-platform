package com.example.apitest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/** 用例集：一组用例的集合，可配置定时执行间隔（分钟，0 表示不自动执行）。 */
@Entity
@Table(name = "test_suites")
public class TestSuite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /** 自动执行间隔（分钟），0 = 关闭定时 */
    private Integer scheduleMinutes = 0;

    private LocalDateTime lastRunAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "suite_cases",
            joinColumns = @JoinColumn(name = "suite_id"),
            inverseJoinColumns = @JoinColumn(name = "case_id"))
    private Set<ApiCase> cases = new HashSet<>();

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getScheduleMinutes() { return scheduleMinutes; }
    public void setScheduleMinutes(Integer scheduleMinutes) { this.scheduleMinutes = scheduleMinutes; }
    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(LocalDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public Set<ApiCase> getCases() { return cases; }
    public void setCases(Set<ApiCase> cases) { this.cases = cases; }
}
