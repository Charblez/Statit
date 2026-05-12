# Statit

Statit is a full-stack ranking and statistics application for submitting personal metrics, comparing scores against other users, and visualizing distributions across local and global categories.

The app supports user-created local categories, seeded global categories backed by public health datasets, leaderboards, percentile/rank calculations, category images, admin moderation, profile score history, and statistical visualizations.

## Tech Stack

- **Frontend:** React, Vite, React Router
- **Backend:** Java 17, Spring Boot 4, Spring Data JPA, Hibernate
- **Database:** PostgreSQL with JSONB support for flexible tags and demographics
- **Deployment:** Azure Container Apps with Azure Container Registry

## Core Concepts

### Local Categories

Local categories are created by Statit users and automatically default to `LOCAL` on the backend. User submissions are saved to the database and used for leaderboards, statistics, score distributions, percentile graphs, and correlation graphs.

### Global Categories

Global categories are seeded by the backend and use external reference datasets. Height, Weight, and BMI use CDC NHANES row-level body-measures data. The app stores those external data points in `global_dataset_points` and uses them to render reference distributions.

For global categories:

- The leaderboard shows Statit user submissions.
- The score distribution graph uses the external reference dataset.
- User percentile and estimated rank are calculated against the external dataset.
- Demographic filters, such as sex and age group, filter the external dataset before statistics are calculated.
- User submissions are still saved, but only the user's best score is kept for ranking/profile behavior.

### Best Scores and Ranking

Statit keeps the best score per user/category for leaderboard and profile views. Tied scores share the same placement, using competition-style ranking. For example, if three users are tied for first, the next user is ranked fourth.

Anonymous submissions are displayed as separate anonymous entries instead of being collapsed into one shared anonymous user.

## Features

### Accounts and Authentication

- Users can create accounts and log in.
- Passwords are hashed on the backend and verified during login.
- User profile data includes demographics used by score submissions and filtering.

### Category Gallery

- Approved categories are shown as fixed-size cards.
- Category images are used as blurred card backgrounds.
- Uploaded category images are auto-cropped on the frontend to a square using the Canvas API.
- Search is available from the navigation bar and filters categories by ordered letters in the category name.
- Empty search restores the full category list.

### Category Pages

Each category page includes:

- Category title, description, image icon, units, and rules.
- Statistics box with mean, median, standard deviation, percentile, and rank when available.
- Score submission form.
- Filters that apply only after the user presses **Apply Filters**.
- Leaderboard populated by saved Statit submissions.
- Score distribution histogram.
- Percentile distribution graph.
- "This is you" indicators on distribution/percentile visualizations when the current user has a submitted score.

For local categories, the page also includes a correlation graph that compares the active category with another local category.

### Data Visualization

- **Score Distribution Graph:** Displays score bins for the active category. Local categories use Statit scores; global categories use the seeded external dataset.
- **Percentile Distribution Graph:** Shows where users fall by percentile and is available for all categories.
- **Correlation Graph:** Available only for local categories. It compares paired user submissions across two local categories, computes the Pearson correlation coefficient, highlights the current user's point in red, and renders a line of best fit with its equation.

### Profile Page

- Shows the user's best score per category.
- Includes the user's rank for each category.
- Supports paginated score display.

### Admin Tools

Admins can:

- View pending categories.
- Approve or delete submitted categories.
- Edit category details and category images.
- Delete scores.
- Search users.
- Grant admin permissions.

Destructive or permission-changing actions include a confirmation prompt before the request is sent to the backend.

## Data Sources

The global Height, Weight, and BMI categories use CDC NHANES body-measures data.

- **CDC NHANES:** https://wwwn.cdc.gov/nchs/nhanes/

The backend seeds row-level external records into the database on startup. Each point stores the source participant id, numeric value, source name, and any supported demographics.

## Local Development

### Prerequisites

- Java 17
- Node.js and npm
- Docker
- PostgreSQL, or the provided Docker Compose database service

### Start the Database

From the backend directory:

```bash
cd backend
POSTGRES_DB=statit_db POSTGRES_USER=ranking_admin POSTGRES_PASSWORD=your_password docker compose up db
```

### Start the Backend

In another terminal:

```bash
cd backend
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
DB_URL=jdbc:postgresql://localhost:5432/statit_db \
DB_USERNAME=ranking_admin \
DB_PASSWORD=your_password \
./gradlew bootRun
```

The backend API runs at:

```text
http://localhost:8080/api/v1
```

### Start the Frontend

In another terminal:

```bash
cd frontend
npm install
VITE_API_BASE_URL=http://localhost:8080/api/v1 npm run dev
```

The Vite dev server prints the local frontend URL, usually:

```text
http://localhost:5173
```

### Run Tests

Backend:

```bash
cd backend
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew test
```

Frontend build check:

```bash
cd frontend
npm run build
```

## Database Notes

The schema includes support for:

- Category lower and upper score limits.
- Category images stored as text/base64 data.
- Category scope through `category_scope`, defaulting to `LOCAL`.
- Global source keys for seeded external datasets.
- External global dataset points in `global_dataset_points`.

## Team

- Charles Bassani
- Wilson Jimenez
- Kenneth Chan
- Derek Ly

## Code Coverage
<img width="2169" height="448" alt="image" src="https://github.com/user-attachments/assets/182f6b8d-a640-4d3d-8208-3fc5b5c07468" />

