package com.statit.backend.service;

import com.statit.backend.model.Category;
import com.statit.backend.model.GlobalDatasetPoint;
import com.statit.backend.repository.CategoryRepository;
import com.statit.backend.repository.GlobalDatasetPointRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.boot.CommandLineRunner;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(2)
public class NhanesDatasetSeeder implements CommandLineRunner
{
    public NhanesDatasetSeeder(CategoryRepository categoryRepository,
                               GlobalDatasetPointRepository globalDatasetPointRepository,
                               SasXportParser sasXportParser,
                               JdbcTemplate jdbcTemplate,
                               ObjectMapper objectMapper)
    {
        this.categoryRepository = categoryRepository;
        this.globalDatasetPointRepository = globalDatasetPointRepository;
        this.sasXportParser = sasXportParser;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args)
    {
        Optional<Category> heightCategory = categoryRepository.findByGlobalSourceKey(HEIGHT_SOURCE_KEY);
        Optional<Category> weightCategory = categoryRepository.findByGlobalSourceKey(WEIGHT_SOURCE_KEY);
        Optional<Category> bmiCategory = categoryRepository.findByGlobalSourceKey(BMI_SOURCE_KEY);
        if(heightCategory.isEmpty() || weightCategory.isEmpty() || bmiCategory.isEmpty()) return;

        if(hasUsableNhanesSeed(heightCategory.get())
                && hasUsableNhanesSeed(weightCategory.get())
                && hasUsableNhanesSeed(bmiCategory.get()))
        {
            return;
        }

        try
        {
            seedNhanesBodyMeasures(heightCategory.get(), weightCategory.get(), bmiCategory.get());
        }
        catch(RuntimeException e)
        {
            System.err.println("Failed to seed CDC NHANES global datasets: " + e.getMessage());
        }
    }

    private void seedNhanesBodyMeasures(Category heightCategory, Category weightCategory, Category bmiCategory)
    {
        List<Map<String, Object>> bodyRows = sasXportParser.fetchRows(BODY_MEASURES_XPT_URL);
        List<Map<String, Object>> demographicRows = sasXportParser.fetchRows(DEMOGRAPHICS_XPT_URL);
        Map<Integer, Map<String, String>> demographicsBySeqn = buildDemographicsBySeqn(demographicRows);

        List<GlobalDatasetPoint> heightPoints = new ArrayList<>();
        List<GlobalDatasetPoint> weightPoints = new ArrayList<>();
        List<GlobalDatasetPoint> bmiPoints = new ArrayList<>();

        for(Map<String, Object> row : bodyRows)
        {
            Integer seqn = toInteger(row.get("SEQN"));
            if(seqn == null) continue;

            Map<String, String> demographics = demographicsBySeqn.getOrDefault(seqn, Map.of());
            Double height = toDouble(row.get("BMXHT"));
            Double weight = toDouble(row.get("BMXWT"));
            Double bmi = toDouble(row.get("BMXBMI"));

            if(height != null)
            {
                heightPoints.add(new GlobalDatasetPoint(
                        heightCategory,
                        NHANES_SOURCE_NAME,
                        String.valueOf(seqn),
                        height,
                        demographics
                ));
            }

            if(weight != null)
            {
                weightPoints.add(new GlobalDatasetPoint(
                        weightCategory,
                        NHANES_SOURCE_NAME,
                        String.valueOf(seqn),
                        weight,
                        demographics
                ));
            }

            if(bmi != null)
            {
                bmiPoints.add(new GlobalDatasetPoint(
                        bmiCategory,
                        NHANES_SOURCE_NAME,
                        String.valueOf(seqn),
                        bmi,
                        demographics
                ));
            }
        }

        deletePointsForCategory(heightCategory);
        deletePointsForCategory(weightCategory);
        deletePointsForCategory(bmiCategory);
        batchInsert(heightPoints);
        batchInsert(weightPoints);
        batchInsert(bmiPoints);
    }

    private void deletePointsForCategory(Category category)
    {
        jdbcTemplate.update(
                "DELETE FROM global_dataset_points WHERE category_id = ?",
                category.getCategoryId()
        );
    }

    private boolean hasUsableNhanesSeed(Category category)
    {
        Long countsWithDemographics = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM global_dataset_points " +
                        "WHERE category_id = ? " +
                        "AND jsonb_exists(demographics, 'sex') " +
                        "AND jsonb_exists(demographics, 'age_group')",
                Long.class,
                category.getCategoryId()
        );
        return countsWithDemographics != null && countsWithDemographics > 0;
    }

    private void batchInsert(List<GlobalDatasetPoint> points)
    {
        jdbcTemplate.batchUpdate(
                "INSERT INTO global_dataset_points " +
                        "(point_id, category_id, source_name, source_participant_id, value, demographics, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), current_timestamp) " +
                        "ON CONFLICT (category_id, source_name, source_participant_id) DO NOTHING",
                points,
                BATCH_SIZE,
                this::setBatchValues
        );
    }

    private void setBatchValues(PreparedStatement statement, GlobalDatasetPoint point) throws SQLException
    {
        statement.setObject(1, UUID.randomUUID());
        statement.setObject(2, point.getCategory().getCategoryId());
        statement.setString(3, point.getSourceName());
        statement.setString(4, point.getSourceParticipantId());
        statement.setDouble(5, point.getValue());
        statement.setString(6, toJson(point.getDemographics()));
    }

    private String toJson(Map<String, String> demographics)
    {
        try
        {
            return objectMapper.writeValueAsString(demographics != null ? demographics : Map.of());
        }
        catch(JsonProcessingException e)
        {
            throw new IllegalArgumentException("Failed to serialize NHANES demographics.", e);
        }
    }

    private Map<Integer, Map<String, String>> buildDemographicsBySeqn(List<Map<String, Object>> rows)
    {
        Map<Integer, Map<String, String>> demographicsBySeqn = new HashMap<>();
        for(Map<String, Object> row : rows)
        {
            Integer seqn = toInteger(row.get("SEQN"));
            if(seqn == null) continue;

            Map<String, String> demographics = new HashMap<>();
            String sex = mapSex(toInteger(row.get("RIAGENDR")));
            Integer ageYears = toInteger(row.get("RIDAGEYR"));
            String raceEthnicity = mapRaceEthnicity(toInteger(row.get("RIDRETH3")));

            if(sex != null) demographics.put("sex", sex);
            if(ageYears != null)
            {
                demographics.put("age_years", String.valueOf(ageYears));
                demographics.put("age_group", mapAgeGroup(ageYears));
            }
            if(raceEthnicity != null) demographics.put("race_ethnicity", raceEthnicity);

            demographicsBySeqn.put(seqn, demographics);
        }
        return demographicsBySeqn;
    }

    private String mapSex(Integer code)
    {
        if(code == null) return null;
        if(code == 1) return "Male";
        if(code == 2) return "Female";
        return null;
    }

    private String mapAgeGroup(Integer ageYears)
    {
        if(ageYears < 18) return "Under 18";
        if(ageYears < 40) return "18-39";
        if(ageYears < 60) return "40-59";
        return "60+";
    }

    private String mapRaceEthnicity(Integer code)
    {
        if(code == null) return null;
        return switch(code)
        {
            case 1 -> "Mexican American";
            case 2 -> "Other Hispanic";
            case 3 -> "Non-Hispanic White";
            case 4 -> "Non-Hispanic Black";
            case 6 -> "Non-Hispanic Asian";
            case 7 -> "Other Race";
            default -> null;
        };
    }

    private Integer toInteger(Object value)
    {
        Double doubleValue = toDouble(value);
        return doubleValue != null ? (int)Math.round(doubleValue) : null;
    }

    private Double toDouble(Object value)
    {
        if(value instanceof Number number)
        {
            double doubleValue = number.doubleValue();
            return Double.isFinite(doubleValue) ? doubleValue : null;
        }
        if(value instanceof String text)
        {
            try
            {
                String trimmed = text.trim();
                if(trimmed.isEmpty()) return null;
                return Double.parseDouble(trimmed);
            }
            catch(NumberFormatException e)
            {
                return null;
            }
        }
        return null;
    }

    public static final String HEIGHT_SOURCE_KEY = "nhanes_height";
    public static final String WEIGHT_SOURCE_KEY = "nhanes_weight";
    public static final String BMI_SOURCE_KEY = "nhanes_bmi";
    public static final String NHANES_SOURCE_NAME = "CDC NHANES August 2021-August 2023";
    public static final String BODY_MEASURES_XPT_URL = "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public/2021/DataFiles/BMX_L.xpt";
    public static final String DEMOGRAPHICS_XPT_URL = "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public/2021/DataFiles/DEMO_L.xpt";
    public static final String BODY_MEASURES_DOC_URL = "https://wwwn.cdc.gov/Nchs/Data/Nhanes/Public/2021/DataFiles/BMX_L.htm";

    private final CategoryRepository categoryRepository;
    private final GlobalDatasetPointRepository globalDatasetPointRepository;
    private final SasXportParser sasXportParser;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private static final int BATCH_SIZE = 500;
}
