package com.statit.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "global_dataset_points",
       uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "source_name", "source_participant_id"}))
public class GlobalDatasetPoint
{
    public GlobalDatasetPoint() {}

    public GlobalDatasetPoint(Category category,
                              String sourceName,
                              String sourceParticipantId,
                              Double value,
                              Map<String, String> demographics)
    {
        this.category = category;
        this.sourceName = sourceName;
        this.sourceParticipantId = sourceParticipantId;
        this.value = value;
        this.demographics = demographics != null ? demographics : new HashMap<>();
    }

    public UUID getPointId()                         { return pointId; }
    public Category getCategory()                    { return category; }
    public String getSourceName()                    { return sourceName; }
    public String getSourceParticipantId()           { return sourceParticipantId; }
    public Double getValue()                         { return value; }
    public Map<String, String> getDemographics()     { return demographics; }
    public LocalDateTime getCreatedAt()              { return createdAt; }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "point_id")
    private UUID pointId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @Column(name = "source_participant_id", nullable = false, length = 64)
    private String sourceParticipantId;

    @Column(name = "value", nullable = false)
    private Double value;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "demographics", columnDefinition = "jsonb")
    private Map<String, String> demographics = new HashMap<>();

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamp default current_timestamp")
    private LocalDateTime createdAt = LocalDateTime.now();
}
