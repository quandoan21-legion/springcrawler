# SpringCrawler Platform – Technical Overview

Comprehensive reference for architecture, flows, database layer, and HTTP/API surface of the SpringCrawler application.

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
   - Admin defines sources with category association and CSS selectors (title, content, description, image, removal).
   - `SourceService` persists the selectors; invalid categories trigger validation errors.

2. **Link Discovery**
   - `SourceCrawlService.queueArticlesFromSourcesBatch()` visits each configured category URL, scrapes anchor tags, keeps links that belong to the same domain and contain `.html`, and inserts them as `Post` records with `status=UNCRAWL`.

3. **Content Extraction**
   - `SourceCrawlService.crawlUnCrawlPost()` processes batches of UNCRAWL posts:
     - Pulls HTML via Jsoup.
     - Applies selectors; removes unwanted DOM nodes using `removalSelector`.
     - Resolves lazy-loaded images via `src`, `data-src`, `data-original`, `srcset`, etc.
     - Updates `Post` fields (title, content, short description, image URL) and status → `CRAWLED`.

4. **Publishing**
   - Admin can edit posts in `/admin/posts`, set SEO metadata, toggle status to `PUBLISHED`, and optionally mark “unique content.”
   - Public routes (`/`, `/posts/{id}`) only show posts with `status` in `{CRAWLED, PUBLISHED}` and respect keyword/category filters.

5. **User Authentication**
   - Registration sends OTP via email; account remains disabled until OTP verification.
   - Forgot password uses a separate OTP channel with 60s resend throttle.
   - Admin login uses Spring Security form login; successful login redirects to `/admin`.

6. **Error Handling**
   - Custom `/error` controller renders `error/general.html` or `error/404.html`, both localized in English.

---

## 3. Database Schema

### Posts (`posts`)
| Column             | Type               | Description                                                     |
|--------------------|--------------------|-----------------------------------------------------------------|
| `id`               | BIGINT PK          | Auto-increment                                                  |
| `title`            | VARCHAR            | Scraped or manually entered title                               |
| `slug`             | VARCHAR UNIQUE     | SEO-friendly slug                                               |
| `content`          | TEXT               | Full article text                                               |
| `short_description`| TEXT               | Optional summary                                                |
| `category_id`      | FK → `categories`  | Category assignment                                             |
| `source_id`        | FK → `sources`     | Original source definition                                      |
| `source_url`       | VARCHAR            | Original article URL                                            |
| `img_url`          | VARCHAR            | Selected hero image                                             |
| `tags`             | VARCHAR            | Comma-separated tags                                            |
| `status`           | ENUM               | `DRAFT`, `UNCRAWL`, `CRAWLED`, `PUBLISHED`, `DELETED`           |
| `unique_content`   | BOOLEAN            | Admin flag for unique rewriting                                 |
| `seo_title`, `seo_description`, `seo_keywords` | SEO metadata                                         |
| `crawl_url`        | VARCHAR            | URL used for crawling                                           |
| `created_at`, `updated_at`, `published_at` | Timestamps                                            |

### Categories (`categories`)
| Column       | Description                                  |
|--------------|----------------------------------------------|
| `id`         | BIGINT PK                                     |
| `name`       | Unique category name                          |
| `description`| Optional details                              |
| `status`     | `ACTIVE`, `INACTIVE`, `DELETED`               |

### Sources (`sources`)
| Column            | Description                                                      |
|-------------------|------------------------------------------------------------------|
| `id`              | BIGINT PK                                                        |
| `category_id`     | FK → `categories`                                                |
| `url`             | Seed/category URL                                                |
| `title_selector`  | CSS query for article title                                      |
| `content_selector`| CSS query for article content                                    |
| `description_selector` | CSS query for summary                                      |
| `image_selector`  | CSS query for hero image                                         |
| `removal_selector`| CSS query for elements to remove from content                    |
| `deleted`         | Soft-delete flag                                                 |

### Users (`user`)
| Column            | Description                                                      |
|-------------------|------------------------------------------------------------------|
| `id`, `full_name`, `email`, `password` | Basic user fields (password is BCrypted)     |
| `enabled`         | Activated after OTP                                              |
| `otp`, `otp_expired_time`, `last_otp_sent_time` | OTP state management               |
| `created_at`, `updated_at` | Timestamps                                             |

---

## 4. API & Route Details

### Public/User-Facing
| Method | Path                    | Params                                   | Description                                           |
|--------|------------------------|------------------------------------------|-------------------------------------------------------|
| GET    | `/` `/home`            | `page`, `size`, `q`, `sort`, `category`  | Filterable listing of crawled/published posts         |
| GET    | `/posts/{id}`          |                                          | Single post page; redirects home if status invalid    |

### Authentication
| Method | Path                           | Notes                                                        |
|--------|--------------------------------|-------------------------------------------------------------|
| GET    | `/api/v1/auth`                 | Login page; handles `error` & `logout` query indicators     |
| POST   | `/api/v1/auth/login`           | CSRF-protected form login                                   |
| GET    | `/api/v1/auth/register`        | Registration form                                            |
| POST   | `/api/v1/auth/register`        | Validates confirm password, sends OTP                       |
| GET/POST| `/api/v1/auth/verify-otp`     | OTP input + submission                                       |
| GET/POST| `/api/v1/auth/forgot-password`, `/reset-password` | Reset OTP flow          |
| GET    | `/api/v1/auth/resend-otp`, `/resend-reset-otp` | Respect 60-second throttle                          |
| POST   | `/logout`                      | Invalidates session, redirects to login                     |

### Admin (Requires Login)
| Feature         | Routes                                                                 |
|-----------------|------------------------------------------------------------------------|
| Dashboard       | `GET /admin`                                                           |
| Users           | `GET /admin/users`, `GET/POST /admin/users/edit/{id}`, `GET /delete/{id}` |
| Categories      | `GET /admin/categories`, `GET/POST /add`, `GET/POST /edit/{id}`, `GET /delete/{id}` |
| Posts           | `GET /admin/posts`, `GET/POST /add`, `GET/POST /edit/{id}`, `GET /delete/{id}` |
| Sources         | `GET /admin/sources`, `GET/POST /add`, `GET/POST /edit/{id}`, `GET /delete/{id}` |
| Crawling        | `GET/POST /admin/crawl`, `POST /admin/crawl/run-pending`                |

> Security: `/admin/**` guarded by Spring Security; all other routes are public (with CSRF protection on forms).

---

## 5. Crawler Workflow (Detailed)

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

SourceService.getActiveSources()
   ↓
SourceCrawlService.queueArticlesFromSourcesBatch()
   • GET category page via Jsoup
   • Extract anchor tags → absolute URLs
   • Filter by desired domain + ".html"
   • Persist new Post rows with status UNCRAWL
   ↓
SourceCrawlService.crawlUnCrawlPost()
   • Fetch page content
   • Apply removalSelector on content element
   • Build Post fields (title/content/description/imgUrl)
   • Validate fallback selectors (defaults point to VnExpress DOM)
   • Status → CRAWLED, save to DB
```

Threads are launched at startup via `@PostConstruct` to continually queue links and crawl posts with a 5-minute sleep between batches (`BOT_INTERVAL_MS`).

---

## 6. Security & OTP Flow

1. **Registration** – If email exists and is enabled, reject. If email exists but disabled, re-send OTP. Else create user with `enabled=false` and send OTP.
2. **OTP Resend** – Enforced wait of 60 seconds between sends. Messages clearly inform the user.
3. **Verification** – If OTP matches and not expired, user is activated (`enabled=true`, OTP cleared).
4. **Forgot Password** – Generates a new OTP, reuses `sendNewOTP`, updates password once OTP validated.
5. **Admin Access** – Spring Security form login, custom failure messages, remember-me with 7-day token.

---

## 7. Environment & Configuration

`src/main/resources/application.properties` contains:
- Cloud SQL JDBC URL, username, password.
- Thymeleaf configuration (prefix/suffix).
- SMTP settings for Gmail (`spring.mail.*`).
- Logging levels for Hibernate SQL debugging.

Override using environment variables:
- `PORT` – server port (Cloud Run uses `$PORT`).
- `DATABASE_NAME` – Schema name for Cloud SQL.
- `SPRING_DATASOURCE_*`, `SPRING_MAIL_*` as needed.

---

## 8. Build, Run, Deploy

```bash
# Install dependencies & package (skip tests)
./mvnw clean package -DskipTests

# Local dev server
./mvnw spring-boot:run

# Build artifact
target/springcrawler-0.0.1-SNAPSHOT.jar

# Deploy steps (example Cloud Run):
gcloud run deploy springcrawler \
  --source . \
  --set-env-vars "PORT=8080,DATABASE_NAME=crawlerdb" \
  --allow-unauthenticated
```

> Ensure Cloud SQL Instance connection is available via `com.google.cloud.sql.mysql.SocketFactory`.

---

## 9. Future Enhancements

- Add RESTful APIs for posts (JSON) for SPA/mobile clients.
- Scheduled crawling via Cloud Scheduler / PubSub.
- Multi-role support (editor vs. admin) with role-based security.
- Rich text rendering (HTML content) instead of `.text()` extraction.
- Unit/integration tests for services and controllers.

---

This document should serve as the canonical reference for new contributors and DevOps engineers. Update it whenever data models or flows change.
