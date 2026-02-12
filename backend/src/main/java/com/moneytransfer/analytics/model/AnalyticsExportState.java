package com.moneytransfer.analytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity to track the watermark for incremental analytics export.
 * 
 * Stores the timestamp of the last successfully exported transaction.
 * Only one row exists in the table (id=1).
 */
@Entity
@Table(name = "analytics_export_state")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsExportState {

    @Id
    @Column(nullable = false)
    private Integer id;

    @Column(name = "last_exported_timestamp")
    private Instant lastExportedTimestamp;

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Instant getLastExportedTimestamp() {
        return lastExportedTimestamp;
    }

    public void setLastExportedTimestamp(Instant lastExportedTimestamp) {
        this.lastExportedTimestamp = lastExportedTimestamp;
    }
}
