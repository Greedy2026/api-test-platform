package com.example.apitest.repository;

import com.example.apitest.entity.TestSuite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TestSuiteRepository extends JpaRepository<TestSuite, Long> {
    List<TestSuite> findByOwnerIdOrderByIdDesc(Long ownerId);

    /** 查询所有开启了定时（scheduleMinutes > 0）的用例集 */
    @Query("select s from TestSuite s where s.scheduleMinutes > 0")
    List<TestSuite> findAllScheduled();
}
