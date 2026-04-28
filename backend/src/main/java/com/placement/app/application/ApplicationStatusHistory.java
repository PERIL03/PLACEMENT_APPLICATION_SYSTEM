package com.placement.app.application;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

@Entity
public class ApplicationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id", nullable = false)
    private DriveApplication application;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus newStatus;

    private String changedBy;
    private String remarks;
    private Instant changedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DriveApplication getApplication() {
        return application;
    }

    public void setApplication(DriveApplication application) {
        this.application = application;
    }

    public ApplicationStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(ApplicationStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public ApplicationStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(ApplicationStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }
}
