package com.mentalhealth.mhbackend.repository;

import com.mentalhealth.mhbackend.model.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    Optional<SyncLog> findTopByOrderBySyncedAtDesc();
}
