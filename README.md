# Statit

A platform for submitting, ranking, and analyzing personal metrics across user-defined categories with real-time statistical processing. 

## Getting Started

### Prerequisites
- Java 25
- PostgreSQL 15+
- Gradle

### Database Setup
```sql
CREATE DATABASE statit_db;
CREATE USER ranking_admin WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE statit_db TO ranking_admin;
```

### Run Locally
```bash
git clone https://github.com/Charblez/Global-Ranking-System.git
cd Global-Ranking-System/backend
./gradlew bootRun
```

The API will be available at `http://localhost:8080`

### Run Tests
```bash
./gradlew test
```

## API Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/users | Create a user |
| GET | /api/v1/users/{username} | Get user by username |
| POST | /api/v1/categories | Create a category |
| GET | /api/v1/categories | List all categories (paginated) |
| POST | /api/v1/scores | Submit a score |
| GET | /api/v1/leaderboards/{categoryId}/top | Get ranked leaderboard (paginated) |
| POST | /api/v1/leaderboards/{categoryId}/filtered | Get filtered leaderboard (paginated) |

## Architecture
```
com.statit.backend
├── config/         # Spring configuration
├── controller/     # REST API endpoints
├── dto/            # Request/response objects
├── exception/      # Error handling
├── model/          # JPA entities
├── repository/     # Database queries
├── service/        # Business logic
```

## Tech Stack
- **Backend:** Java 25, Spring Boot 4.0, Hibernate
- **Database:** PostgreSql 15 with JSONB for dynamic filtering

## Features

## Contributing
1. Create a branch: `user/<name>/backend/<feature>`
2. PR into 'dev' for code review
3. PRs into 'main' require passing tests

## Team
- Charles Bassani
- Wilson Jimenez
- Kenneth Chan
- Derek Ly

# Statit
Statit is a platform that allows users to submit their personal metrics (e.g., height, weight, etc.) across various categories and see how they compare to the rest of the world. The system tracks individual user histories while dynamically maintaining global and community baselines, using statistical outlier rejection to ensure data integrity.

## Core Features
### 1. User Accounts & Authentication
- **Account Requirements:** Users must create an account to upload scores or propose new categories. This acts as the primary rate-limiting and anti-spam measure.
- **Verification:** Currently, there is no email or identity verification required for account creation (similar to when2meet). Future scope is to require email registration and potentially other anti-spam measures.
- **Profile Demographics:** Users can save default demographics (Date of Birth, Sex, Region) to their profile to enable some auto-filling for score submissions.

### 2. Category Management
- **Admin Approval:** Users can propose new measurement categories, but all categories require Admin approval before going live to prevent duplicates, maintain appropriateness, and enforce data standards.
- **Granular Configurations:** When creating a category, the following parameters are defined.
    - **Units:** The specific unit of measurement (e.g., cm, lbs, kg).
    - **Separation Rules:** Options for whether the category requires separation by Region, Sex, and/or Age Group.

### 3. Score Tracking & History
- **Personal Records:** The system highlights a user's highest (or best) score in a given category.
- **Score History:** A complete, time-stamped history of all uploads is retained so users can track their progress over time. Only 1 record (the highest value, or less often, the most recent value) is kept in the community table. 

### 4. Global Baselines & Statistics
- **External APIs vs. Internal Logic:** If highly reliable, static data exists (e.g., WHO or CDC LMS parameters), the system will use a seeded table to calculate z-scores.
    - For all other custom categories, the application dynamically calculates community baselines (Mean, Median, Standard Deviation) from aggregated user submissions.
- **Outlier Rejection:** To handle malicious inputs, the system calculates a z-score for every incoming submission. Scores that fall beyond a realistic threshold are saved to the user's history but are flagged as invalid and are excluded from community tables.

### 5. Advanced Analytics (Not yet implemented)
- **Distribution Shape Analaysis:** For internally tracked categories, the system will eventually calculate the shape of the data curve (skewness and kurtosis) to move beyond simple Gaussian distributions and better model real-world data clustering.
- **Dataset Correlation:** Identifying statistical relationships between different categories

<img width="1860" height="384" alt="image" src="https://github.com/user-attachments/assets/0ee75289-b4b3-47b7-bd6e-5c6f82af4993" />

