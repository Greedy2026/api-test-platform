package com.example.apitest.controller;

import com.example.apitest.entity.ApiCase;
import com.example.apitest.entity.User;
import com.example.apitest.repository.ApiCaseRepository;
import com.example.apitest.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final ApiCaseRepository caseRepository;
    private final CurrentUserService currentUser;

    public CaseController(ApiCaseRepository caseRepository, CurrentUserService currentUser) {
        this.caseRepository = caseRepository;
        this.currentUser = currentUser;
    }

    public record CaseDto(String name, String method, String url,
                          String headers, String body, String assertions, Integer timeoutSeconds) {}

    @GetMapping
    public List<ApiCase> list() {
        User user = currentUser.get();
        return caseRepository.findByOwnerIdOrderByIdDesc(user.getId());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CaseDto dto) {
        String error = validate(dto);
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", error));
        }
        ApiCase c = new ApiCase();
        apply(c, dto);
        c.setOwner(currentUser.get());
        return ResponseEntity.ok(caseRepository.save(c));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CaseDto dto) {
        ApiCase c = caseRepository.findById(id).orElse(null);
        if (c == null || !c.getOwner().getId().equals(currentUser.get().getId())) {
            return ResponseEntity.notFound().build();
        }
        String error = validate(dto);
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", error));
        }
        apply(c, dto);
        c.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(caseRepository.save(c));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        ApiCase c = caseRepository.findById(id).orElse(null);
        if (c == null || !c.getOwner().getId().equals(currentUser.get().getId())) {
            return ResponseEntity.notFound().build();
        }
        caseRepository.delete(c);
        return ResponseEntity.noContent().build();
    }

    private String validate(CaseDto dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            return "用例名称不能为空";
        }
        String m = dto.method() == null ? "" : dto.method().toUpperCase();
        if (!List.of("GET", "POST", "PUT", "DELETE", "PATCH").contains(m)) {
            return "不支持的 HTTP 方法";
        }
        if (dto.url() == null || !(dto.url().startsWith("http://") || dto.url().startsWith("https://"))) {
            return "URL 必须以 http:// 或 https:// 开头";
        }
        return null;
    }

    private void apply(ApiCase c, CaseDto dto) {
        c.setName(dto.name());
        c.setMethod(dto.method().toUpperCase());
        c.setUrl(dto.url());
        c.setHeaders(dto.headers() == null || dto.headers().isBlank() ? "{}" : dto.headers());
        c.setBody(dto.body() == null ? "" : dto.body());
        c.setAssertions(dto.assertions() == null || dto.assertions().isBlank() ? "[]" : dto.assertions());
        if (dto.timeoutSeconds() != null && dto.timeoutSeconds() > 0 && dto.timeoutSeconds() <= 60) {
            c.setTimeoutSeconds(dto.timeoutSeconds());
        }
    }
}
