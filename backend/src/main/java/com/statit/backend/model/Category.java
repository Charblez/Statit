/**
 * Filename: Category.java
 * Author: Charles Bassani
 * Description: Category DTO and model, utilizing JSONB for allowed tags.
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

//----------------------------------------------------------------------------------------------------
// Class Definition
//----------------------------------------------------------------------------------------------------
@Entity
@Table(name = "categories")
public class Category
{
    //------------------------------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------------------------------
    public Category() {}

    public Category(String categoryName,
                    String description,
                    String units,
                    List<String> tags,
                    Boolean sortOrder,
                    User foundingUser,
                    Double lowerLimit,
                    Double upperLimit)
    {
        this.categoryName = categoryName;
        this.description = description;
        this.units = units;
        if(tags != null) addTags(tags);
        this.sortOrder = sortOrder;
        this.foundingUser = foundingUser;
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
    }

    //------------------------------------------------------------------------------------------------
    // Public Methods
    //------------------------------------------------------------------------------------------------
    public void update(String categoryName,
                       String description,
                       String units,
                       List<String> tags,
                       Boolean sortOrder,
                       Double lowerLimit,
                       Double upperLimit)
    {
        this.categoryName = categoryName;
        this.description = description;
        this.units = units;
        if(tags != null) addTags(tags);
        this.sortOrder = sortOrder;
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
    }

    public void addTag(String tag)
    {
        if(tags.contains(tag.toLowerCase())) return;
        else tags.add(tag.toLowerCase());
    }

    public void addTags(List<String> tags)
    {
        for(String tag : tags) addTag(tag);
    }

    public void removeTag(String tag)
    {
        if(!tags.contains(tag.toLowerCase())) return;
        else tags.remove(tag.toLowerCase());
    }

    //Getters
    public UUID getCategoryId()                    { return categoryId; }
    public String getName()                        { return categoryName; }
    public String getDescription()                 { return description; }
    public List<String> getTags()                  { return tags; }
    public String getUnits()                       { return units; }
    public Boolean getSortOrder()                  { return sortOrder; }
    public User getFoundingUser()                  { return foundingUser; }
    public Double getLowerLimit()                  { return lowerLimit; }
    public Double getUpperLimit()                  { return upperLimit; }
    public LocalDateTime getCreatedAt()            { return createdAt; }

    //Setters
    public void setName(String name)                { this.categoryName = name; }
    public void setDescription(String description)  { this.description = description; }
    public void setTags(List<String> tags)          { this.tags = tags; }
    public void setLowerLimit(Double lowerLimit)    { this.lowerLimit = lowerLimit; }
    public void setUpperLimit(Double upperLimit)    { this.upperLimit = upperLimit; }

    //------------------------------------------------------------------------------------------------
    // Private Variables
    //------------------------------------------------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "name", unique = true, nullable = false)
    private String categoryName;

    @Column(name = "description")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private List<String> tags = new ArrayList<>();

    @Column(name = "units_of_measurement", nullable = false, length = 20)
    private String units;

    @Column(name = "sort_order", nullable = false)
    private Boolean sortOrder;

    @Column(name = "lower_limit", nullable = false)
    private Double lowerLimit;

    @Column(name = "upper_limit", nullable = false)
    private Double upperLimit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "founding_user_id", nullable = false)
    private User foundingUser;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
};