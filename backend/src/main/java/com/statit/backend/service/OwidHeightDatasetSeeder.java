package com.statit.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.statit.backend.model.Category;
import com.statit.backend.model.GlobalDatasetPoint;
import com.statit.backend.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(3)
public class OwidHeightDatasetSeeder implements CommandLineRunner
{
    public OwidHeightDatasetSeeder(CategoryRepository categoryRepository,
                                   JdbcTemplate jdbcTemplate,
                                   ObjectMapper objectMapper)
    {
        this.categoryRepository = categoryRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args)
    {
        Optional<Category> heightCategory = categoryRepository.findByGlobalSourceKey(HEIGHT_SOURCE_KEY);
        if(heightCategory.isEmpty()) return;

        if(hasUsableHeightSeed(heightCategory.get())) return;

        try
        {
            seedHeightPoints(heightCategory.get());
        }
        catch(RuntimeException e)
        {
            System.err.println("Failed to seed OWID height dataset: " + e.getMessage());
        }
    }

    private boolean hasUsableHeightSeed(Category category)
    {
        Long countsWithDemographics = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM global_dataset_points " +
                        "WHERE category_id = ? " +
                        "AND jsonb_exists(demographics, 'sex') " +
                        "AND jsonb_exists(demographics, 'region')",
                Long.class,
                category.getCategoryId()
        );
        return countsWithDemographics != null && countsWithDemographics > 0;
    }

    private void seedHeightPoints(Category category)
    {
        List<GlobalDatasetPoint> points = parseHeightPoints(category, fetchCsv());
        jdbcTemplate.update("DELETE FROM global_dataset_points WHERE category_id = ?", category.getCategoryId());
        batchInsert(points);
    }

    private String fetchCsv()
    {
        HttpRequest request = HttpRequest.newBuilder(URI.create(HEIGHT_CSV_URL))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Statit OWID height data seeder/1.0")
                .GET()
                .build();

        try
        {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new IllegalStateException("Failed to fetch OWID height dataset.");
            }
            return response.body();
        }
        catch(IOException e)
        {
            throw new IllegalStateException("Failed to fetch OWID height dataset.", e);
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch OWID height dataset.", e);
        }
    }

    private List<GlobalDatasetPoint> parseHeightPoints(Category category, String csv)
    {
        String[] lines = csv.split("\\R");
        if(lines.length < 2) return List.of();

        List<String> headers = parseCsvLine(lines[0]);
        int entityIndex = headers.indexOf("Entity");
        int codeIndex = headers.indexOf("Code");
        int yearIndex = headers.indexOf("Year");
        int maleHeightIndex = headers.indexOf("Mean male height (cm)");
        int femaleHeightIndex = headers.indexOf("Mean female height (cm)");
        int regionIndex = headers.indexOf("World region according to OWID");

        if(entityIndex < 0 || yearIndex < 0 || maleHeightIndex < 0 || femaleHeightIndex < 0 || regionIndex < 0)
        {
            throw new IllegalStateException("OWID height dataset is missing required columns.");
        }

        List<GlobalDatasetPoint> points = new ArrayList<>();
        for(int i = 1; i < lines.length; i++)
        {
            List<String> cells = parseCsvLine(lines[i]);
            if(cells.size() <= regionIndex) continue;

            String entity = cells.get(entityIndex).trim();
            String code = codeIndex >= 0 && codeIndex < cells.size() ? cells.get(codeIndex).trim() : "";
            String year = cells.get(yearIndex).trim();
            String region = cells.get(regionIndex).trim();
            if(entity.isEmpty() || year.isEmpty() || region.isEmpty()) continue;

            addHeightPoint(points, category, cells, maleHeightIndex, code, entity, year, region, "Male");
            addHeightPoint(points, category, cells, femaleHeightIndex, code, entity, year, region, "Female");
        }

        return points;
    }

    private void addHeightPoint(List<GlobalDatasetPoint> points,
                                Category category,
                                List<String> cells,
                                int heightIndex,
                                String code,
                                String entity,
                                String year,
                                String region,
                                String sex)
    {
        if(heightIndex >= cells.size()) return;
        Double height = parseDouble(cells.get(heightIndex));
        if(height == null) return;

        Map<String, String> demographics = new HashMap<>();
        demographics.put("sex", sex);
        demographics.put("region", region);
        demographics.put("country", entity);
        demographics.put("year", year);

        String stableEntityId = !code.isBlank() ? code : entity.replaceAll("[^A-Za-z0-9]", "");
        points.add(new GlobalDatasetPoint(
                category,
                HEIGHT_SOURCE_NAME,
                stableEntityId + "-" + year + "-" + sex,
                height,
                demographics
        ));
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
            throw new IllegalArgumentException("Failed to serialize OWID height demographics.", e);
        }
    }

    private List<String> parseCsvLine(String line)
    {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for(int i = 0; i < line.length(); i++)
        {
            char c = line.charAt(i);
            if(c == '"')
            {
                if(quoted && i + 1 < line.length() && line.charAt(i + 1) == '"')
                {
                    current.append('"');
                    i++;
                }
                else
                {
                    quoted = !quoted;
                }
            }
            else if(c == ',' && !quoted)
            {
                cells.add(current.toString());
                current.setLength(0);
            }
            else
            {
                current.append(c);
            }
        }

        cells.add(current.toString());
        return cells;
    }

    private Double parseDouble(String value)
    {
        try
        {
            String trimmed = value.trim();
            if(trimmed.isEmpty()) return null;
            return Double.parseDouble(trimmed);
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }

    public static final String HEIGHT_SOURCE_KEY = "height";
    public static final String HEIGHT_SOURCE_NAME = "Our World in Data - Human Height";
    public static final String HEIGHT_CSV_URL = "https://ourworldindata.org/grapher/mean-height-males-vs-females.csv";
    public static final String HEIGHT_DOC_URL = "https://ourworldindata.org/grapher/mean-height-males-vs-females";

    private final CategoryRepository categoryRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final int BATCH_SIZE = 500;
}
