package com.example.apitest.repository;

import com.example.apitest.entity.ExecutionRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionRunRepository extends JpaRepository<ExecutionRun, Long> {
    List<ExecutionRun> findByOwnerIdOrderByIdDesc(Long ownerId);
    List<ExecutionRun> findBySuiteIdOrderByIdDesc(Long suiteId);
}
