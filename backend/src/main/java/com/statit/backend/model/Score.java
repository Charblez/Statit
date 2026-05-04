/**
 * Filename: Score.java
 * Author: Charles Bassani
 * Description: Score DTO and model, utilizing JSONB for dynamic tags.
 */

//----------------------------------------------------------------------------------------------------
// Package
//----------------------------------------------------------------------------------------------------
package com.statit.backend.model;

//----------------------------------------------------------------------------------------------------
// Imports
//----------------------------------------------------------------------------------------------------
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

//----------------------------------------------------------------------------------------------------
// Class Definition
//----------------------------------------------------------------------------------------------------
@Entity
@Table(name = "scores")
public class Score
{
    //------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------
    public Score() {}

    public Score(Category category,
                 User user,
                 Float score,
                 Map<String, String> tags,
                 Boolean anonymous)
    {
        this(category, user, score != null ? score.doubleValue() : null, tags, anonymous);
    }

    public Score(Category category,
                 User user,
                 Double score,
                 Map<String, String> tags,
                 Boolean anonymous)
    {
        this.category = category;
        this.user = user;
        this.score = score;
        this.tags = tags != null ? tags : new HashMap<>();
        this.anonymous = anonymous;
        this.rejected = false;
    }

    //------------------------------------------------------------------------------------------------
    // Public Functions
    //------------------------------------------------------------------------------------------------
    public void update(Category category,
                       User user,
                       Float score,
                       Map<String, String> tags,
                       Boolean anonymous,
                       Boolean rejected)
    {
        update(category, user, score != null ? score.doubleValue() : null, tags, anonymous, rejected);
    }

    public void update(Category category,
                       User user,
                       Double score,
                       Map<String, String> tags,
                       Boolean anonymous,
                       Boolean rejected)
    {
        this.category = category;
        this.user = user;
        this.score = score;
        this.tags = tags != null ? tags : new HashMap<>();
        this.anonymous = anonymous;
        this.rejected = rejected;
    }

    //Getters
    public UUID getScoreId()               { return scoreId; }
    public Category getCategory()          { return category; }
    public User getUser()                  { return user; }
    public Double getScore()                { return score; }
    public Map<String, String> getTags()   { return tags; }
    public Boolean getAnonymous()          { return anonymous; }
    public LocalDateTime getSubmittedAt()  { return submittedAt; }

    //Setters
    public void setRejected(Boolean rejected)            { this.rejected = rejected; }
    public void setTags(Map<String, String> tags)        { this.tags = tags; }

    //------------------------------------------------------------------------------------------------
    // Private Variables
    //------------------------------------------------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "score_id")
    private UUID scoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "score_value", nullable = false)
    private Double score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private Map<String, String> tags = new HashMap<>();

    @Column(name = "is_anonymous", nullable = false)
    private Boolean anonymous;

    @Column(name = "rejected", nullable = false)
    private Boolean rejected;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
};


