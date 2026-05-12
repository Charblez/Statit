# Statit

The app supports user-created local categories, seeded global categories backed by public health datasets, leaderboards, percentile/rank calculations, category images, admin moderation, profile score history, and statistical visualizations.

- **Frontend:** React, Vite, React Router
- **Backend:** Java 17, Spring Boot 4, Spring Data JPA, Hibernate
- **Database:** PostgreSQL with JSONB support for flexible tags and demographics
- **Deployment:** Azure Container Apps with Azure Container Registry

Local categories are created by Statit users and automatically default to `LOCAL` on the backend. User submissions are saved to the database and used for leaderboards, statistics, score distributions, percentile graphs, and correlation graphs.

Global categories are seeded by the backend and use external reference datasets. Height, Weight, and BMI use CDC NHANES row-level body-measures data. The app stores those external data points in `global_dataset_points` and uses them to render reference distributions.

- The leaderboard shows Statit user submissions.
- The score distribution graph uses the external reference dataset.
- User percentile and estimated rank are calculated against the external dataset.
- Demographic filters, such as sex and age group, filter the external dataset before statistics are calculated.
- User submissions are still saved, but only the user's best score is kept for ranking/profile behavior.

Statit keeps the best score per user/category for leaderboard and profile views. Tied scores share the same placement, using competition-style ranking. For example, if three users are tied for first, the next user is ranked fourth.

Anonymous submissions are displayed as separate anonymous entries instead of being collapsed into one shared anonymous user.

- Passwords are hashed on the backend and verified during login.
- User profile data includes demographics used by score submissions and filtering.

- Category images are used as blurred card backgrounds.
- Uploaded category images are auto-cropped on the frontend to a square using the Canvas API.
- Search is available from the navigation bar and filters categories by ordered letters in the category name.
- Empty search restores the full category list.

- Statistics box with mean, median, standard deviation, percentile, and rank when available.
- Filters that apply only after the user presses **Apply Filters**.
- Score distribution histogram.
- Percentile distribution graph.
- "This is you" indicators on distribution/percentile visualizations when the current user has a submitted score.

For local categories, the page also includes a correlation graph that compares the active category with another local category.

- **Score Distribution Graph:** Displays score bins for the active category. Local categories use Statit scores; global categories use the seeded external dataset.
- **Percentile Distribution Graph:** Shows where users fall by percentile and is available for all categories.
- **Correlation Graph:** Available only for local categories. It compares paired user submissions across two local categories, computes the Pearson correlation coefficient, highlights the current user's point in red, and renders a line of best fit with its equation.

- Shows the user's best score per category.
- Includes the user's rank for each category.

- Edit category details and category images.
- Search users.
- Grant admin permissions.

Destructive or permission-changing actions include a confirmation prompt before the request is sent to the backend.

The global Height, Weight, and BMI categories use CDC NHANES body-measures data.

- **CDC NHANES:** https://wwwn.cdc.gov/nchs/nhanes/

The backend seeds row-level external records into the database on startup. Each point stores the source participant id, numeric value, source name, and any supported demographics.

- Category lower and upper score limits.
- Category images stored as text/base64 data.
- Category scope through `category_scope`, defaulting to `LOCAL`.
- Global source keys for seeded external datasets.
- External global dataset points in `global_dataset_points`.
- JSONB demographics for flexible filtering.

## Code Coverage 
<img width="2169" height="448" alt="image" src="https://github.com/user-attachments/assets/154fb293-319a-4e31-b682-9fde83fb53a0" />

