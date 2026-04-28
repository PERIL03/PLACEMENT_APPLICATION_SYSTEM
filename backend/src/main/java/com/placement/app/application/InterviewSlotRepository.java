package com.placement.app.application;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSlotRepository extends JpaRepository<InterviewSlot, Long> {

    Optional<InterviewSlot> findByApplicationId(Long applicationId);
}
