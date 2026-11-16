package com.example.apitest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 接口用例：一条待执行的 HTTP 请求及其断言规则。
 * 断言以 JSON 字符串存储，格式：
 * [{"type":"status","op":"eq","value":200},
 *  {"type":"body_contains","value":"ok"},
 *  {"type":"json_field","path":"data.name","op":"eq","value":"alice"}]
 */
@Entity
@Table(name = "api_cases")
public class ApiCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** GET / POST / PUT / DELETE */
    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 500)
    private String url;

    /** 请求头，JSON 对象字符串 */
    @Column(length = 2000)
    private String headers;

    /** 请求体（原始字符串，通常为 JSON） */
    @Column(length = 10000)
    private String body;

    /** 断言规则列表，JSON 数组字符串 */
    @Column(nullable = false, length = 4000)
    private String assertions;

    /** 超时时间（秒） */
    private Integer timeoutSeconds = 10;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getHeaders() { return headers; }
    public void setHeaders(String headers) { this.headers = headers; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getAssertions() { return assertions; }
    public void setAssertions(String assertions) { this.assertions = assertions; }
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
