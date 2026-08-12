# 🎨 UI Maker — Design System Analyzer

> Hệ thống phân tích Design System tự động từ bất kỳ website nào.  
> Nhập URL → Crawl trang web → Trích xuất design tokens → Sinh báo cáo Markdown.

---

## 📖 Mục lục

- [Tổng quan](#-tổng-quan)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Chức năng chính](#-chức-năng-chính)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Cài đặt và chạy dự án](#-cài-đặt-và-chạy-dự-án)
- [API Endpoints](#-api-endpoints)
- [Luồng xử lý chính](#-luồng-xử-lý-chính)
- [Cấu hình môi trường](#-cấu-hình-môi-trường)

---

## 🔍 Tổng quan

**UI Maker** là một nền tảng phân tích Design System tự động, cho phép người dùng nhập URL của bất kỳ website nào, hệ thống sẽ tự động:

1. **Thu thập (Crawl)** — Quét nhiều trang web song song bằng headless browser
2. **Trích xuất (Extract)** — Phân tích CSS computed styles để rút trích design tokens
3. **Phân tích (Analyze)** — Tổng hợp dữ liệu từ nhiều trang, tính toán tần suất sử dụng và độ phủ
4. **Tạo tài liệu (Report)** — Sinh báo cáo Markdown chi tiết kèm các khuyến nghị cải thiện

---

## 🏗 Kiến trúc hệ thống

Dự án sử dụng kiến trúc **Microservices** gồm 4 service độc lập, giao tiếp bất đồng bộ qua **Message Queue**:

```
┌─────────────┐     HTTP      ┌──────────────┐    RabbitMQ    ┌────────────────────┐
│  design-ui  │ ─────────────▶│  design-api  │──────────────▶│  playwright-worker │
│  (React)    │               │ (Spring Boot)│               │  (Node.js)         │
└─────────────┘               └──────┬───────┘               └────────────────────┘
                                     │                                 │
                                     │ RabbitMQ                        │ Crawl completed
                                     ▼                                 │
                              ┌──────────────────┐                     │
                              │ design-ai-service │◀────────────────────┘
                              │ (Python)          │     (via design-api)
                              └──────────────────┘

              ┌──────────┐          ┌────────────┐
              │ MongoDB  │          │  RabbitMQ  │
              │ (Data)   │          │  (Queue)   │
              └──────────┘          └────────────┘
```

### Luồng giao tiếp giữa các service

| Từ | Đến | Phương thức | Mô tả |
|---|---|---|---|
| `design-ui` | `design-api` | REST API (HTTP) | Gửi yêu cầu phân tích, lấy kết quả |
| `design-api` | `playwright-worker` | RabbitMQ (`design.crawl.exchange`) | Gửi yêu cầu crawl website |
| `playwright-worker` | `design-api` | RabbitMQ (`crawl.completed` / `crawl.failed`) | Trả kết quả crawl |
| `design-api` | `design-ai-service` | RabbitMQ (`design.analysis.exchange`) | Gửi dữ liệu để phân tích AI |
| `design-ai-service` | `design-api` | RabbitMQ (`analysis.completed` / `analysis.failed`) | Trả kết quả phân tích |

---

## ✨ Chức năng chính

### 1. Quản lý người dùng & Xác thực
- **Đăng ký** tài khoản với validation dữ liệu đầu vào
- **Đăng nhập** bằng JWT Token (HMAC-SHA512)
- **Phân quyền** theo vai trò (User / Admin) với Spring Security
- **Token Revocation** — Thu hồi token khi logout (blacklist bằng MongoDB)
- **Token Introspection** — Kiểm tra tính hợp lệ của token

### 2. Thu thập Website (Crawling)
- **Headless Browser Crawling** — Sử dụng Playwright Chromium để render đầy đủ JavaScript
- **Đa trang** — Crawl đồng thời nhiều trang (tối đa 11 trang/lần)
- **Design Token Extraction** — Trích xuất trực tiếp từ DOM các design tokens:
  - 🎨 **Colors** — Màu text, background, border, outline (tối đa 100 màu)
  - 🔤 **Typography** — Font family, size, weight, line-height, letter-spacing (tối đa 100 styles)
  - 📏 **Spacing** — Padding, margin, gap values (tối đa 100 tokens)
  - 🔘 **Border Radius** — Bo góc của các element (tối đa 100 tokens)
  - 🌑 **Shadows** — Box-shadow values (tối đa 100 tokens)
  - 🏷 **CSS Variables** — Các custom properties trên `:root` (tối đa 500 biến)
- **Bảo mật** — `PublicUrlGuard` chống SSRF (Server-Side Request Forgery), chặn private IP, localhost
- **Rate Limit Handling** — Tự động retry khi gặp HTTP 429 (tối đa 3 lần) với Retry-After header
- **Timeout & Deadline** — Giới hạn thời gian crawl toàn bộ job (120s) và từng trang (30s)
- **Document Size Limit** — Giới hạn kích thước tài liệu (5MB)

### 3. Tổng hợp Design System (Aggregation)
- **Cross-page Aggregation** — Gộp dữ liệu từ nhiều trang, đếm tần suất sử dụng (`usageCount`)
- **Page Coverage** — Tính tỷ lệ phủ của mỗi token trên toàn bộ trang (`pageCoverage`)
- **Context Tracking** — Ghi lại ngữ cảnh sử dụng (element selector, CSS property)
- **CSS Variable Variants** — Phát hiện cùng một CSS variable có giá trị khác nhau trên các trang
- **Normalization** — Chuẩn hóa whitespace, lowercase để tránh trùng lặp

### 4. Phân tích AI & Khuyến nghị
- **Design Recommendations** — Đưa ra khuyến nghị cải thiện design system:
  - Cảnh báo spacing scale có quá nhiều giá trị (>12)
  - Cảnh báo sử dụng quá nhiều radius values (>5)
  - Cảnh báo sử dụng quá nhiều font families (>3)
- **Confidence Score** — Tính điểm tin cậy của phân tích (0.0–1.0) dựa trên số trang, màu sắc, typography đã phân tích
- **Markdown Report Generation** — Tự động sinh báo cáo Markdown đầy đủ bao gồm:
  - Summary (tổng quan số lượng tokens)
  - Colors table (giá trị, tần suất, context)
  - Typography table (font, size, weight, line-height)
  - Spacing table, Border Radius table, Shadows table
  - Recommendations (khuyến nghị cải thiện)

### 5. Giao diện người dùng
- **Trang chủ** — Nhập URL website cần phân tích
- **Trang phân tích** — Hiển thị tiến trình crawl và kết quả real-time (colors, typography, spacing, components)
- **Trang báo cáo Markdown** — Xem preview và tải xuống file `.md`

---

## 🛠 Công nghệ sử dụng

### Frontend — `design-ui`

| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| **React** | 19.x | UI library — xây dựng giao diện component-based |
| **TypeScript** | 6.x | Static type checking |
| **Vite** | 8.x | Build tool & dev server cực nhanh (HMR) |
| **React Router DOM** | 7.x | Client-side routing (SPA) |
| **ESLint** | 10.x | Code linting & quality |

### Backend — `design-api`

| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| **Java** | 21 (LTS) | Ngôn ngữ chính |
| **Spring Boot** | 3.5.x | Framework backend chính |
| **Spring Data MongoDB** | — | ODM cho MongoDB, tự động mapping document |
| **Spring AMQP** | — | Tích hợp RabbitMQ (publish/consume messages) |
| **Spring Security + OAuth2 Resource Server** | — | Xác thực JWT, phân quyền RBAC |
| **Nimbus JOSE+JWT** | — | Ký và xác minh JWT token (HMAC-SHA512) |
| **BCrypt** | — | Mã hóa mật khẩu (cost factor = 10) |
| **Lombok** | 1.18.x | Giảm boilerplate code (getter, setter, builder) |
| **MapStruct** | 1.5.x | Mapping tự động giữa Entity ↔ DTO |
| **Jakarta Validation** | — | Validation dữ liệu đầu vào (@Valid) |

### Crawl Worker — `playwright-worker`

| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| **Node.js + TypeScript** | — | Runtime & ngôn ngữ |
| **Playwright** | 1.62.x | Headless Chromium browser automation |
| **amqplib** | 2.x | RabbitMQ client (consume/publish messages) |
| **tsx** | 4.x | TypeScript execution engine (dev & prod) |

### AI Service — `design-ai-service`

| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| **Python** | 3.x | Ngôn ngữ chính |
| **aio-pika** | 9.x | Async RabbitMQ client (asyncio-based) |
| **Pydantic** | 2.x | Data validation & serialization |
| **Pydantic Settings** | 2.x | Quản lý cấu hình từ environment variables |
| **NumPy** | 2.x | Tính toán số học |
| **scikit-learn** | 1.5.x | Machine learning / phân tích dữ liệu |

### Infrastructure

| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| **Docker & Docker Compose** | — | Container hóa và orchestration tất cả services |
| **MongoDB** | 8.x | NoSQL database — lưu trữ users, analysis jobs, crawled pages, design systems |
| **RabbitMQ** | 4.x (Management) | Message broker — giao tiếp bất đồng bộ giữa các service |
| **Dead Letter Queue (DLQ)** | — | Xử lý message thất bại, tránh mất dữ liệu |

---

## 📁 Cấu trúc dự án

```
ui-maker/
├── compose.yaml                  # Docker Compose — orchestration tất cả services
├── .env                          # Biến môi trường (MongoDB, RabbitMQ, JWT)
│
├── design-ui/                    # 🖥 Frontend (React + Vite + TypeScript)
│   ├── src/
│   │   ├── pages/                # Các trang chính
│   │   │   ├── HomePage.tsx          # Nhập URL → bắt đầu phân tích
│   │   │   ├── AnalysisPage.tsx      # Hiển thị tiến trình & kết quả
│   │   │   ├── MarkdownReportPage.tsx# Xem & tải báo cáo Markdown
│   │   │   └── NotFoundPage.tsx      # Trang 404
│   │   ├── routes/               # Cấu hình routing (React Router)
│   │   ├── layouts/              # Layout components (AnalysisLayout)
│   │   ├── components/           # Reusable UI components
│   │   └── assets/               # Static assets
│   ├── vite.config.ts
│   └── package.json
│
├── design-api/                   # ⚙️ Backend API (Spring Boot + Java 21)
│   ├── src/main/java/com/designmd/designapi/
│   │   ├── auth/                 # Xác thực (login, logout, introspect)
│   │   │   ├── AuthenticationController.java
│   │   │   └── AuthenticationService.java
│   │   ├── user/                 # Quản lý người dùng (CRUD + RBAC)
│   │   │   ├── UserController.java
│   │   │   ├── UserService.java
│   │   │   └── User.java
│   │   ├── analysis/             # Quản lý analysis jobs
│   │   │   ├── AnalysisController.java
│   │   │   ├── AnalysisCommandService.java
│   │   │   ├── AnalysisQueryService.java
│   │   │   └── AnalysisJob.java
│   │   ├── crawl/                # Quản lý kết quả crawl
│   │   │   ├── CrawlRequestPublisher.java    # Gửi crawl request → RabbitMQ
│   │   │   ├── CrawlCompletedConsumer.java   # Nhận kết quả crawl
│   │   │   ├── CrawlFailedConsumer.java      # Nhận lỗi crawl
│   │   │   └── CrawlResultService.java
│   │   ├── design/               # Tổng hợp Design System
│   │   │   ├── DesignSystemController.java
│   │   │   ├── DesignAggregationService.java # Aggregation logic (698 LOC)
│   │   │   └── DesignSystemSnapshot.java
│   │   ├── ai/                   # Tích hợp AI analysis
│   │   │   ├── DesignAnalysisPublisher.java  # Gửi yêu cầu phân tích AI
│   │   │   ├── DesignAnalysisCompletedConsumer.java
│   │   │   └── DesignAnalysisResultService.java
│   │   ├── security/             # Cấu hình bảo mật
│   │   │   ├── SecurityConfig.java           # JWT filter chain, RBAC
│   │   │   └── JwtAuthenticationEntryPoint.java
│   │   ├── token/                # Token revocation (blacklist)
│   │   └── messaging/            # RabbitMQ configuration
│   │       ├── RabbitMqConfig.java           # Exchange, Queue, Binding
│   │       └── RabbitMqConstants.java
│   ├── pom.xml
│   └── Dockerfile
│
├── playwright-worker/            # 🕷 Crawl Worker (Node.js + Playwright)
│   ├── src/
│   │   ├── index.ts              # Entry point
│   │   ├── crawler/
│   │   │   └── crawl-service.ts      # Chromium crawling logic
│   │   ├── extraction/
│   │   │   └── design-token-extractor.ts  # DOM → design tokens
│   │   ├── screenshot/
│   │   │   └── screenshot-service.ts
│   │   ├── security/
│   │   │   └── public-url-guard.ts   # SSRF protection
│   │   ├── messaging/
│   │   │   ├── rabbitmq.ts           # RabbitMQ connection
│   │   │   ├── crawl-consumer.ts     # Consume crawl requests
│   │   │   └── result-publisher.ts   # Publish crawl results
│   │   ├── config/               # Environment config
│   │   ├── contracts/            # Message type definitions
│   │   └── storage/              # File storage
│   ├── package.json
│   └── Dockerfile
│
└── design-ai-service/            # 🤖 AI Analysis Service (Python)
    ├── app/
    │   ├── main.py               # Entry point
    │   ├── config.py             # Pydantic Settings configuration
    │   ├── analyzers/
    │   │   └── design_analyzer.py    # Phân tích design + recommendations
    │   ├── generators/
    │   │   └── markdown_generator.py # Sinh báo cáo Markdown
    │   ├── messaging/
    │   │   └── rabbitmq.py           # Async RabbitMQ consumer/publisher
    │   └── contracts/            # Event type definitions
    ├── tests/
    ├── requirements.txt
    └── Dockerfile
```

---

## 📋 Yêu cầu hệ thống

- **Docker** ≥ 20.10
- **Docker Compose** ≥ 2.0
- **RAM** ≥ 4GB (Playwright Chromium cần ~1GB shared memory)

### Phát triển local (không dùng Docker)

| Service | Yêu cầu |
|---|---|
| `design-ui` | Node.js ≥ 18, pnpm hoặc npm |
| `design-api` | Java 21, Maven 3.9+ |
| `playwright-worker` | Node.js ≥ 18, Playwright browsers |
| `design-ai-service` | Python ≥ 3.11 |

---

## 🚀 Cài đặt và chạy dự án

### Chạy bằng Docker Compose (Khuyến nghị)

```bash
# 1. Clone repository
git clone <repository-url>
cd ui-maker

# 2. Cấu hình biến môi trường
cp .env.example .env   # Chỉnh sửa nếu cần

# 3. Khởi chạy toàn bộ hệ thống
docker compose up --build -d

# 4. Kiểm tra trạng thái
docker compose ps
```

### Chạy từng service riêng (Development)

**Frontend:**
```bash
cd design-ui
npm install       # hoặc pnpm install
npm run dev       # → http://localhost:5173
```

**Backend API:**
```bash
cd design-api
./mvnw spring-boot:run    # → http://localhost:9999
```

**Playwright Worker:**
```bash
cd playwright-worker
npm install
npx playwright install chromium
npm run dev
```

**AI Service:**
```bash
cd design-ai-service
pip install -r requirements.txt
python -m app.main
```

---

## 📡 API Endpoints

### Authentication — `/auth`

| Method | Endpoint | Mô tả | Auth |
|---|---|---|---|
| `POST` | `/auth/token` | Đăng nhập, lấy JWT token | ❌ Public |
| `POST` | `/auth/introspect` | Kiểm tra token hợp lệ | ❌ Public |
| `POST` | `/auth/logout` | Đăng xuất, thu hồi token | ❌ Public |

### Users — `/users`

| Method | Endpoint | Mô tả | Auth |
|---|---|---|---|
| `POST` | `/users` | Đăng ký tài khoản mới | ❌ Public |
| `GET` | `/users` | Lấy danh sách users | 🔒 Admin only |
| `GET` | `/users/{userId}` | Lấy thông tin user (chỉ chính mình) | 🔒 Authenticated |
| `GET` | `/users/myInfo` | Lấy thông tin người dùng hiện tại | 🔒 Authenticated |
| `PUT` | `/users/{userId}` | Cập nhật thông tin user | 🔒 Authenticated |
| `DELETE` | `/users/{userId}` | Xoá tài khoản | 🔒 Authenticated |

### Analyses — `/analyses`

| Method | Endpoint | Mô tả | Auth |
|---|---|---|---|
| `POST` | `/analyses` | Tạo phân tích mới (gửi URL) | 🔒 Authenticated |
| `GET` | `/analyses` | Lấy danh sách phân tích (phân trang) | 🔒 Authenticated |
| `GET` | `/analyses/{analysisId}` | Chi tiết một phân tích | 🔒 Authenticated |
| `DELETE` | `/analyses/{analysisId}` | Xoá phân tích | 🔒 Authenticated |
| `GET` | `/analyses/{analysisId}/pages` | Danh sách trang đã crawl | 🔒 Authenticated |
| `GET` | `/analyses/{analysisId}/design-system` | Kết quả Design System tổng hợp | 🔒 Authenticated |

---

## 🔄 Luồng xử lý chính

```
Người dùng nhập URL
       │
       ▼
┌──────────────────────────────────────────────────────────────┐
│ 1. design-ui: POST /analyses { websiteUrl, additionalPaths }│
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ 2. design-api:                                               │
│    - Tạo AnalysisJob (status: CRAWL_REQUESTED)              │
│    - Publish message → RabbitMQ (design.crawl.exchange)      │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ 3. playwright-worker:                                        │
│    - Consume crawl request                                   │
│    - Khởi tạo Chromium headless (viewport 1440×900)         │
│    - Validate URL qua PublicUrlGuard (chống SSRF)           │
│    - Mở từng trang → chờ DOM loaded + network idle          │
│    - extractDesignTokens(): quét tối đa 5000 elements       │
│      → thu thập colors, typography, spacing, radii,         │
│        shadows, CSS variables                                │
│    - Publish kết quả → RabbitMQ (crawl.completed)           │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ 4. design-api:                                               │
│    - Consume crawl result                                    │
│    - Lưu CrawledPage vào MongoDB                            │
│    - DesignAggregationService.rebuild():                     │
│      → Gộp colors/typography/spacing/radii/shadows          │
│        từ tất cả trang đã crawl                              │
│      → Tính usageCount, pageCount, pageCoverage             │
│      → Lưu DesignSystemSnapshot vào MongoDB                 │
│    - Publish message → RabbitMQ (design.analysis.exchange)   │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ 5. design-ai-service:                                        │
│    - Consume analysis request                                │
│    - DesignAnalyzer.analyze():                               │
│      → Lọc & chuẩn hóa tokens (top 30 colors, 20 typo...) │
│      → Sinh recommendations (spacing, radius, font)         │
│      → Tính confidence score (0.0 – 1.0)                    │
│    - MarkdownGenerator.generate():                           │
│      → Sinh báo cáo Markdown đầy đủ                         │
│    - Publish kết quả → RabbitMQ (analysis.completed)        │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ 6. design-api:                                               │
│    - Consume analysis result                                 │
│    - Lưu DesignAnalysisResult + Markdown vào MongoDB        │
│    - Cập nhật AnalysisJob status: COMPLETED                 │
└──────────────────────┬───────────────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ 7. design-ui:                                                │
│    - Hiển thị kết quả phân tích (colors, typography,        │
│      spacing, components)                                    │
│    - Cho phép xem và tải báo cáo Markdown                   │
└──────────────────────────────────────────────────────────────┘
```

---

## ⚙️ Cấu hình môi trường

### Biến môi trường chính (`.env`)

| Biến | Mô tả | Giá trị mặc định |
|---|---|---|
| `MONGO_USERNAME` | MongoDB username | `design` |
| `MONGO_PASSWORD` | MongoDB password | `design-mongo-secret` |
| `RABBITMQ_USERNAME` | RabbitMQ username | `design` |
| `RABBITMQ_PASSWORD` | RabbitMQ password | `design-rabbit-secret` |
| `JWT_SIGNER_KEY` | HMAC-SHA512 secret key (64 bytes hex) | *(xem .env)* |

### Cấu hình Playwright Worker

| Biến | Mô tả | Giá trị |
|---|---|---|
| `MAX_CRAWL_PAGES` | Số trang tối đa mỗi lần crawl | `11` |
| `NAVIGATION_TIMEOUT_MS` | Timeout load trang (ms) | `30000` |
| `CRAWL_JOB_TIMEOUT_MS` | Timeout toàn bộ crawl job (ms) | `120000` |
| `MAX_DOCUMENT_BYTES` | Kích thước tối đa tài liệu (bytes) | `5000000` |
| `CRAWL_RETRY_ATTEMPTS` | Số lần retry khi crawl thất bại | `3` |
| `PUBLISH_RETRY_ATTEMPTS` | Số lần retry publish message | `3` |

---

## 🔐 Bảo mật

- **JWT Authentication** — Token được ký bằng HMAC-SHA512, hỗ trợ revocation qua blacklist
- **Password Hashing** — BCrypt với cost factor 10
- **RBAC** — Phân quyền theo role (User/Admin) bằng `@PreAuthorize` / `@PostAuthorize`
- **SSRF Protection** — `PublicUrlGuard` kiểm tra URL trước khi crawl:
  - Chặn private IP ranges (10.x, 172.16-31.x, 192.168.x)
  - Chặn localhost, loopback
  - Chỉ cho phép HTTP/HTTPS
- **CSRF** — Đã disable (stateless JWT API)
- **Request Validation** — Sử dụng Jakarta Validation (`@Valid`)

---

## 🧪 Kỹ thuật nổi bật

| Kỹ thuật | Mô tả |
|---|---|
| **Event-Driven Architecture** | Các service giao tiếp qua RabbitMQ messages, giảm coupling |
| **Dead Letter Queue (DLQ)** | Message thất bại được chuyển sang DLQ để debug và retry |
| **Headless Browser Crawling** | Sử dụng Playwright Chromium để render SPA, JavaScript-heavy websites |
| **DOM-level Design Token Extraction** | Quét `getComputedStyle()` trên tới 5000 DOM elements |
| **Cross-page Token Aggregation** | Gộp tokens từ nhiều trang, tính tần suất và độ phủ |
| **Async Message Processing** | Python service dùng `aio-pika` (asyncio) để xử lý non-blocking |
| **Publisher Confirms** | Đảm bảo message được gửi thành công tới RabbitMQ |
| **Graceful Retry** | Tự động retry với exponential backoff khi gặp lỗi mạng |
| **Rate Limit Aware** | Tôn trọng `Retry-After` header khi bị HTTP 429 |
| **CQRS Pattern** | Tách Command (write) và Query (read) service trong design-api |

---

## 📄 License

MIT License — Xem file [LICENSE](LICENSE) để biết thêm chi tiết.
