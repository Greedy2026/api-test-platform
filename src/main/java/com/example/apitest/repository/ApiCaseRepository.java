package com.example.apitest.repository;

import com.example.apitest.entity.ApiCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiCaseRepository extends JpaRepository<ApiCase, Long> {
    List<ApiCase> findByOwnerIdOrderByIdDesc(Long ownerId);
}
