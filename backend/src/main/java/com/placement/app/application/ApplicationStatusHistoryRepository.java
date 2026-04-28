package com.placement.app.application;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {

    List<ApplicationStatusHistory> findByApplicationIdOrderByChangedAtDesc(Long applicationId);
}
