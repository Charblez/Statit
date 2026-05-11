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
                    User foundingUser)
    {
        this(categoryName, description, units, tags, sortOrder, foundingUser, 0.0, 100.0);
    }

    public Category(String categoryName,
                    String description,
                    String units,
                    List<String> tags,
                    Boolean sortOrder,
                    User foundingUser,
                    Double lowerLimit,
                    Double upperLimit)
    {
        this(categoryName, description, units, tags, sortOrder, foundingUser, lowerLimit, upperLimit, null);
    }

    public Category(String categoryName,
                    String description,
                    String units,
                    List<String> tags,
                    Boolean sortOrder,
                    User foundingUser,
                    Double lowerLimit,
                    Double upperLimit,
                    String imageData)
    {
        this.categoryName = categoryName;
        this.description = description;
        this.units = units;
        if(tags != null) addTags(tags);
        this.sortOrder = sortOrder;
        this.foundingUser = foundingUser;
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
        this.imageData = imageData;
        this.categoryScope = CategoryScope.LOCAL;
    }

    //------------------------------------------------------------------------------------------------
    // Public Methods
    //------------------------------------------------------------------------------------------------
    public void update(String categoryName,
                       String description,
                       String units,
                       List<String> tags,
                       Boolean sortOrder)
    {
        update(categoryName, description, units, tags, sortOrder, lowerLimit, upperLimit);
    }

    public void update(String categoryName,
                       String description,
                       String units,
                       List<String> tags,
                       Boolean sortOrder,
                       Double lowerLimit,
                       Double upperLimit)
    {
        update(categoryName, description, units, tags, sortOrder, lowerLimit, upperLimit, imageData);
    }

    public void update(String categoryName,
                       String description,
                       String units,
                       List<String> tags,
                       Boolean sortOrder,
                       Double lowerLimit,
                       Double upperLimit,
                       String imageData)
    {
        this.categoryName = categoryName;
        this.description = description;
        this.units = units;
        if(tags != null) addTags(tags);
        this.sortOrder = sortOrder;
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
        this.imageData = imageData;
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
    public String getImageData()                   { return imageData; }
    public CategoryScope getCategoryScope()        { return categoryScope; }
    public String getGlobalSourceKey()             { return globalSourceKey; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public Boolean getLive()                       { return live != null && live; }
    public boolean isGlobal()                      { return categoryScope == CategoryScope.GLOBAL; }

    //Setters
    public void setName(String name)                { this.categoryName = name; }
    public void setDescription(String description)  { this.description = description; }
    public void setTags(List<String> tags)          { this.tags = tags; }
    public void setLowerLimit(Double lowerLimit)    { this.lowerLimit = lowerLimit; }
    public void setUpperLimit(Double upperLimit)    { this.upperLimit = upperLimit; }
    public void setImageData(String imageData)      { this.imageData = imageData; }
    public void setCategoryScope(CategoryScope categoryScope)
    {
        this.categoryScope = categoryScope != null ? categoryScope : CategoryScope.LOCAL;
    }
    public void setGlobalSourceKey(String globalSourceKey) { this.globalSourceKey = globalSourceKey; }
    public void setLive(Boolean live)               { this.live = live != null && live; }

    @PrePersist
    @PreUpdate
    private void applyCategoryDefaults()
    {
        if(categoryScope == null)
        {
            categoryScope = CategoryScope.LOCAL;
        }
        if(globalSourceKey == null || globalSourceKey.isBlank())
        {
            globalSourceKey = null;
            categoryScope = CategoryScope.LOCAL;
        }
    }

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

    @Column(name = "lower_limit", nullable = false, columnDefinition = "double precision default 0.0")
    private Double lowerLimit = 0.0;

    @Column(name = "upper_limit", nullable = false, columnDefinition = "double precision default 100.0")
    private Double upperLimit = 100.0;

    @Column(name = "image_data", columnDefinition = "text")
    private String imageData;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_scope", nullable = false, length = 16, columnDefinition = "varchar(16) default 'LOCAL'")
    private CategoryScope categoryScope = CategoryScope.LOCAL;

    @Column(name = "global_source_key")
    private String globalSourceKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "founding_user_id")
    private User foundingUser;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "live", nullable = false, columnDefinition = "boolean default false")
    private Boolean live = false;
};
