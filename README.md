# SpringCrawler Platform

Modern Spring Boot application that crawls news sources, stores normalized posts, and serves both a public-facing portal and a full-featured admin console (users, categories, sources, crawling queue). Built on Spring Boot 3.5, Thymeleaf, MySQL (Cloud SQL), and Tailwind/Bootstrap templates.

---

## 1. Tech Stack

| Layer            | Technology                                                                                     |
|------------------|------------------------------------------------------------------------------------------------|
| Runtime          | Java 17, Spring Boot 3.5                                                                       |
| Persistence      | Spring Data JPA, MySQL (Cloud SQL in production), Hibernate                                    |
| View             | Thymeleaf templates, Tailwind/Bootstrap-based layouts, custom CSS                              |
| Crawling         | Jsoup HTML parser, custom selectors per source                                                  |
| Auth/Security    | Spring Security (form login, remember-me, logout, CSRF)                                         |
| Messaging        | SMTP via `spring-boot-starter-mail` (OTP + reset emails)                                        |

---

## 2. Application Flow

1. **Source Configuration**
   - Admin defines sources with category association and CSS selectors (title, content, description, image, removal) via `/admin/sources`.

2. **Link Discovery**
   - `SourceCrawlService.queueArticlesFromSourcesBatch()` visits each source URL, scrapes anchor tags, filters for in-domain `.html` links, and queues new `Post` rows with `status=UNCRAWL`.

3. **Content Extraction**
   - `SourceCrawlService.crawlUnCrawlPost()` fetches each queued link, applies selectors/removal rules, resolves lazy-loaded images, populates `Post` fields, and marks the post `CRAWLED`.

4. **Publishing**
   - Admin uses `/admin/posts` to edit content, set SEO metadata, switch status to `PUBLISHED`, and optionally flag “unique content.” Public `/` shows only `CRAWLED` or `PUBLISHED` posts with keyword/category filters.

5. **User Authentication**
   - Registration (OTP-based), login, forgot/reset password, and OTP verification flows under `/api/v1/auth/**`. Admin login redirects to `/admin`.

6. **Error Handling**
   - Custom `/error` controller renders `error/general.html` or `error/404.html` with English copy.

---

## 3. Database Schema (MySQL / Cloud SQL)

### Posts (`posts`)
| Column             | Type           | Description                                                      |
|--------------------|----------------|------------------------------------------------------------------|
| `id`               | BIGINT PK      | Auto-increment                                                   |
| `title`, `slug`    | VARCHAR        | Title and unique SEO slug                                        |
| `content`, `short_description` | TEXT | Article body and summary                                         |
| `category_id`, `source_id` | FK    | Relations to categories/sources                                  |
| `source_url`, `img_url`, `tags`, `crawl_url` | Source metadata                                          |
| `status`           | ENUM           | `DRAFT`, `UNCRAWL`, `CRAWLED`, `PUBLISHED`, `DELETED`             |
| `unique_content`   | BOOLEAN        | Manual flag                                                       |
| `seo_title`, `seo_description`, `seo_keywords` | SEO metadata                                      |
| Timestamps         | `created_at`, `updated_at`, `published_at`                                    |

### Categories (`categories`)
| Column       | Description                                  |
|--------------|----------------------------------------------|
| `id`, `name` | Primary key and unique name                  |
| `description`, `status` | Optional note + enum (`ACTIVE`, `INACTIVE`, `DELETED`) |

### Sources (`sources`)
| Column            | Description                                                      |
|-------------------|------------------------------------------------------------------|
| `id`, `category_id`, `url` | Seed URL per category                                   |
| CSS selectors     | `title_selector`, `content_selector`, `description_selector`, `image_selector`, `removal_selector` |
| `deleted`         | Soft-delete flag                                                 |

### Users (`user`)
| Column            | Description                                                      |
|-------------------|------------------------------------------------------------------|
| `full_name`, `email`, `password` | Basic identity (password is BCrypted)             |
| `enabled`, `otp`, `otp_expired_time`, `last_otp_sent_time` | OTP state management   |
| `created_at`, `updated_at`       | Auditing                                          |

---

## 4. HTTP/API Surface

### Public Routes
| Method | Path            | Description                                      |
|--------|-----------------|--------------------------------------------------|
| GET    | `/` `/home`     | Filterable list of `CRAWLED`/`PUBLISHED` posts    |
| GET    | `/posts/{id}`   | Post detail view (redirects home if status invalid) |

### Auth Routes
| Method | Path                                | Notes                                     |
|--------|-------------------------------------|-------------------------------------------|
| GET/POST| `/api/v1/auth`, `/login`           | Login form and submission                 |
| GET/POST| `/api/v1/auth/register`            | Registration with confirm password        |
| GET/POST| `/api/v1/auth/verify-otp`          | OTP activation                            |
| GET/POST| `/api/v1/auth/forgot-password`     | Request reset OTP                         |
| GET/POST| `/api/v1/auth/reset-password`      | Validate OTP and update password          |
| GET     | `/api/v1/auth/resend-otp`, `/resend-reset-otp` | 60s throttle                        |
| POST    | `/logout`                          | Invalidate session                        |

### Admin Routes (secured)
| Feature     | Routes                                                                     |
|-------------|-----------------------------------------------------------------------------|
| Dashboard   | `GET /admin`                                                               |
| Users       | `GET /admin/users`, `GET/POST /edit/{id}`, `GET /delete/{id}`              |
| Categories  | `GET /admin/categories`, `GET/POST /add`, `GET/POST /edit/{id}`, `GET /delete/{id}` |
| Posts       | `GET /admin/posts`, `GET/POST /add`, `GET/POST /edit/{id}`, `GET /delete/{id}` |
| Sources     | `GET /admin/sources`, `GET/POST /add`, `GET/POST /edit/{id}`, `GET /delete/{id}` |
| Crawling    | `GET/POST /admin/crawl`, `POST /admin/crawl/run-pending`                    |

> Security: `/admin/**` guarded by Spring Security; other routes are public (with CSRF on forms).

---

## 5. Crawler Workflow Diagram

```mermaid
flowchart TD
    A[Active Sources] --> B[Queue Links]
    B -->|Create UNCRAWL posts| C[Post Table]
    C --> D[Crawl Content]
    D -->|Status → CRAWLED| E[Post Table]
    E --> F[Admin Review/Publish]
    F -->|Status → PUBLISHED| G[Public Portal]

    subgraph Queue Links
        B1[Fetch category page via Jsoup]
        B2[Extract anchor tags -> absolute URLs]
        B3[Filter domain + ".html"]
        B4[Persist as UNCRAWL posts]
        A --> B1 --> B2 --> B3 --> B4
    end

    subgraph Crawl Content
        D1[Fetch HTML for crawl_url]
        D2[Apply selectors & removal rules]
        D3[Resolve lazy image attributes]
        D4[Update post fields]
        B --> D1 --> D2 --> D3 --> D4
    end
```

Threads launch at startup via `@PostConstruct` to continually queue links and crawl posts with a 5-minute pause (`BOT_INTERVAL_MS`) between batches.

---

## 6. Security & OTP Lifecycle

1. **Registration** – Reject if email already enabled. If disabled, resend OTP. Otherwise create user with `enabled=false`, hash password, send OTP.
2. **OTP Resend** – Enforce 60-second wait; messages inform users why they must wait.
3. **Verification** – Valid OTP activates user (`enabled=true`, OTP cleared). Invalid/expired OTP returns descriptive errors.
4. **Forgot Password** – Generates a reset OTP, emails it, and accepts new password once OTP verified.
5. **Admin Access** – Spring Security form login with custom failure messages, remember-me (7-day tokens), logout at `/logout`.

---

## 7. Configuration & Environment

`src/main/resources/application.properties` includes:
- Cloud SQL JDBC URL (`SocketFactory`), username, password.
- Thymeleaf settings (prefix/suffix).
- SMTP configuration for Gmail (host, port, username, app password).
- Logging levels for Hibernate SQL output.

Environment variables override defaults:
- `PORT` (Cloud Run/App Engine uses `$PORT`).
- `DATABASE_NAME`, `SPRING_DATASOURCE_*`, `SPRING_MAIL_*`.
- `spring.jpa.hibernate.ddl-auto=update` is set; adjust for production migrations.

---

## 8. Build, Run, Deploy

```bash
# Install deps & package
./mvnw clean package -DskipTests

# Local development
./mvnw spring-boot:run

# Jar location
target/springcrawler-0.0.1-SNAPSHOT.jar

# Example Cloud Run deploy
gcloud run deploy springcrawler \
  --source . \
  --set-env-vars "PORT=8080,DATABASE_NAME=crawlerdb" \
  --allow-unauthenticated
```

Ensure Cloud SQL IAM permissions allow the service to connect. When running locally, use `cloud_sql_proxy` or direct MySQL port forwarding.

---

## 9. Future Enhancements

- REST/JSON APIs for posts to support mobile or SPA clients.
- Scheduled crawling (Cloud Scheduler/PubSub) instead of background threads.
- Role-based access control (editor vs. admin).
- Rich HTML rendering instead of plain-text content extraction.
- Automated test suite for services/controllers.

---

## 10. Basic Usage

1. **Register / Login**
   - Visit `/api/v1/auth/register`, fill in details, confirm password, and enter the emailed OTP.
   - Sign in at `/api/v1/auth` and you will be redirected to `/admin`.

2. **Create Categories**
   - Navigate to `/admin/categories`, add or edit categories that will organize posts and sources.

3. **Define Sources**
   - Under `/admin/sources`, add a source per category with the correct CSS selectors for title, content, description, image, and any removal selectors.

4. **Queue Links**
   - Go to `/admin/crawl`, choose a source, click *Start crawling* to queue new `UNCRAWL` posts.

5. **Process UNCRAWL Posts**
   - Still on `/admin/crawl`, click *Run UNCRAWL queue* (or wait for the background bot) to fetch content, images, and metadata.

6. **Review & Publish Posts**
   - Visit `/admin/posts` to edit the crawled entries, add SEO metadata, mark “unique content,” and set status to `PUBLISHED`.

7. **Public Site**
   - Access `/` to verify that the published posts appear with search, sort, and category filters; click a post to view `/posts/{id}`.

8. **Password Reset / OTP Resend**
   - Users can trigger `/api/v1/auth/forgot-password` for reset OTPs; admins can resend OTPs from registration flow if needed.

This sequence covers a typical crawl-to-publication cycle. Repeat steps 3–7 for additional sources and categories.

---

This README consolidates the full architectural overview, database schema, routes, and flows. Update it whenever core functionality changes.
