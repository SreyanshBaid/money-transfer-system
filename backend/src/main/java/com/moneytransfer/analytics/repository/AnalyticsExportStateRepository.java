package com.moneytransfer.analytics.repository;

import com.moneytransfer.analytics.model.AnalyticsExportState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing analytics export watermark state.
 * 
 * Only one record (id=1) should exist in the table.
 */
@Repository
public interface AnalyticsExportStateRepository extends JpaRepository<AnalyticsExportState, Integer> {
}
