# 🛠️ ConnectSphere — Step-by-Step Development Guide

> Build order matters. Each step depends on the previous one. Follow this exact sequence.

---

## Phase 1: Project Setup & Configuration

### Step 1.1 — Initialize Spring Boot Project
- Create Spring Boot project (Maven, Java 17+)
- Add dependencies in `pom.xml`:
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-security`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-websocket`
  - `spring-boot-starter-data-redis`
  - `spring-kafka`
  - `postgresql` driver
  - `jjwt` (io.jsonwebtoken) for JWT
  - `springdoc-openapi` for Swagger
  - `aws-java-sdk-s3` for S3
  - `lombok` (optional, if you want)

### Step 1.2 — Application Configuration
- `application.yml` — common config
- `application-dev.yml` — local dev (H2 or local PostgreSQL, localhost Redis)
- `application-prod.yml` — production (RDS, ElastiCache, MSK)
- Configure:
  - DataSource (PostgreSQL)
  - JPA (ddl-auto: update for dev, validate for prod)
  - Server port
  - JWT secret & expiration values

### Step 1.3 — Package Structure
```
com.connectsphere/
├── config/           # SecurityConfig, RedisConfig, KafkaConfig, S3Config, WebSocketConfig, SwaggerConfig
├── controller/       # AuthController, UserController, PostController, ChatController, AdminController
├── service/          # AuthService, UserService, PostService, FollowService, LikeService, CommentService, ChatService, AdminService, S3Service, KafkaProducerService
├── repository/       # UserRepository, PostRepository, CommentRepository, LikeRepository, FollowRepository, MessageRepository, ReportRepository
├── entity/           # User, Post, Comment, Like, Follow, Message, Report
│   └── enums/        # Role, AuthProvider, Passion, ReportStatus
├── dto/
│   ├── request/      # RegisterRequest, LoginRequest, PostRequest, CommentRequest, etc.
│   └── response/     # ApiResponse, UserResponse, PostResponse, AuthResponse, etc.
├── security/         # JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetails, CustomUserDetailsService
├── exception/        # GlobalExceptionHandler, custom exceptions
├── kafka/            # KafkaProducer, KafkaConsumer, topic configs
├── websocket/        # ChatWebSocketHandler, WebSocketEventListener
└── util/             # Constants, helper classes
```

---

## Phase 2: Entity Layer (DO THIS FIRST)

> **Why first?** Everything depends on entities. JPA creates your tables.

### Step 2.1 — Create All Enums
| Enum | Values |
|------|--------|
| `Role` | ROLE_USER, ROLE_ADMIN |
| `AuthProvider` | LOCAL, GOOGLE, GITHUB |
| `Passion` | TECHNOLOGY, PROGRAMMING, MUSIC, etc. (30 values) |
| `ReportStatus` | PENDING, REVIEWED, RESOLVED, DISMISSED |

### Step 2.2 — Create All Entities (in this order)

| Order | Entity | Why This Order |
|-------|--------|----------------|
| 1 | `User` | Independent — no FK to other entities |
| 2 | `Post` | Depends on User (author_id FK) |
| 3 | `Comment` | Depends on User + Post |
| 4 | `Like` | Depends on User + Post |
| 5 | `Follow` | Depends on User (follower + following) |
| 6 | `Message` | Depends on User (sender + receiver) |
| 7 | `Report` | Depends on User + Post |

### Step 2.3 — Create All Repositories
- `UserRepository extends JpaRepository<User, UUID>`
- `PostRepository extends JpaRepository<Post, UUID>`
- `CommentRepository extends JpaRepository<Comment, UUID>`
- `LikeRepository extends JpaRepository<Like, UUID>`
- `FollowRepository extends JpaRepository<Follow, UUID>`
- `MessageRepository extends JpaRepository<Message, UUID>`
- `ReportRepository extends JpaRepository<Report, UUID>`

**Run the app** → Verify all tables are created in PostgreSQL ✅

---

## Phase 3: Exception Handling (Before any service logic)

> **Why now?** Every service will throw exceptions. Set this up once, use everywhere.

### Step 3.1 — Custom Exceptions
```
ResourceNotFoundException    → 404 (User not found, Post not found)
BadRequestException          → 400 (Invalid input)
UnauthorizedException        → 401 (Invalid credentials)
ForbiddenException           → 403 (Not your post, not admin)
DuplicateResourceException   → 409 (Username/email already taken)
```

### Step 3.2 — Global Exception Handler
- `@RestControllerAdvice` class
- Handle each custom exception → return proper HTTP status + message
- Handle `MethodArgumentNotValidException` → return validation errors
- Handle generic `Exception` → return 500

### Step 3.3 — Standard API Response Wrapper
```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
}
```
Use this for ALL controller responses for consistency.

---

## Phase 4: Authentication & Security (MUST BE BEFORE ANY API)

> **Why now?** Every API endpoint needs to know WHO is making the request.

### Step 4.1 — JWT Token Provider
- `generateAccessToken(User user)` → 15 min expiry
- `generateRefreshToken(User user)` → 7 days expiry
- `validateToken(String token)` → returns true/false
- `getUserIdFromToken(String token)` → extracts userId

### Step 4.2 — CustomUserDetails & CustomUserDetailsService
- `CustomUserDetails implements UserDetails` — wraps your User entity
- `CustomUserDetailsService implements UserDetailsService` — loads user by email from DB

### Step 4.3 — JWT Authentication Filter
- `JwtAuthenticationFilter extends OncePerRequestFilter`
- Extract token from `Authorization: Bearer <token>` header
- Validate token → set authentication in SecurityContext

### Step 4.4 — Security Configuration
- `SecurityConfig` class with `SecurityFilterChain`
- Public endpoints: `/api/v1/auth/**`, `/swagger-ui/**`
- All other endpoints: authenticated
- Add JWT filter before `UsernamePasswordAuthenticationFilter`
- CORS configuration
- BCrypt password encoder bean

### Step 4.5 — Auth Service & Controller

| Endpoint | What It Does |
|----------|-------------|
| `POST /api/v1/auth/register` | Validate input → check duplicate email/username → hash password → save user → return JWT |
| `POST /api/v1/auth/login` | Find user by email → verify password → return access + refresh tokens |
| `POST /api/v1/auth/refresh-token` | Validate refresh token → generate new access token |
| `POST /api/v1/auth/forgot-password` | Generate reset token → store in Redis (TTL 15 min) → send email (or log it for now) |
| `POST /api/v1/auth/reset-password` | Verify token from Redis → update password → delete token |
| `GET /api/v1/auth/verify-email` | Verify token from Redis → set `isVerified = true` |

**Test:** Register → Login → Use token on a protected endpoint → Verify 401 without token ✅

---

## Phase 5: User Profile Service

> **Why now?** Users exist, auth works. Now let them manage profiles.

### Step 5.1 — User Service

| Method | What It Does |
|--------|-------------|
| `getMyProfile()` | Get logged-in user from SecurityContext |
| `updateProfile(UpdateProfileRequest)` | Update bio, passion, dateOfBirth |
| `getUserById(UUID id)` | Get any user's public profile |
| `searchUsers(String query, Pageable)` | Search by username (LIKE query) |

### Step 5.2 — User Controller

| Endpoint | Method |
|----------|--------|
| `GET /api/v1/users/me` | getMyProfile |
| `PUT /api/v1/users/me` | updateProfile |
| `GET /api/v1/users/{id}` | getUserById |
| `GET /api/v1/users/search?q=` | searchUsers |

### Step 5.3 — DTOs
- `UpdateProfileRequest` (bio, passion, dateOfBirth)
- `UserResponse` (id, username, email, bio, avatarUrl, passion, followerCount, followingCount, createdAt)

**Test:** Update profile → Get profile → Search users ✅

---

## Phase 6: Follow / Unfollow System

> **Why now?** The news feed (Phase 8) depends on who you follow. Build this first.

### Step 6.1 — Follow Repository Custom Queries
```java
boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
long countByFollowingId(UUID userId);  // follower count
long countByFollowerId(UUID userId);   // following count
Page<Follow> findByFollowingId(UUID userId, Pageable pageable);  // get followers
Page<Follow> findByFollowerId(UUID userId, Pageable pageable);   // get following
```

### Step 6.2 — Follow Service

| Method | Logic |
|--------|-------|
| `followUser(UUID targetUserId)` | Check not self-follow → check not already following → save Follow |
| `unfollowUser(UUID targetUserId)` | Check follow exists → delete Follow |
| `getFollowers(UUID userId, Pageable)` | Return paginated follower list |
| `getFollowing(UUID userId, Pageable)` | Return paginated following list |
| `isFollowing(UUID targetUserId)` | Check if current user follows target |

### Step 6.3 — Follow Controller

| Endpoint | Method |
|----------|--------|
| `POST /api/v1/users/{id}/follow` | followUser |
| `DELETE /api/v1/users/{id}/follow` | unfollowUser |
| `GET /api/v1/users/{id}/followers` | getFollowers |
| `GET /api/v1/users/{id}/following` | getFollowing |

**Test:** Follow → Verify count → Get followers list → Unfollow → Verify count decreased ✅

---

## Phase 7: S3 File Upload (Before Posts, because posts need images)

> **Why now?** Posts support images. Avatars need upload. Set up S3 first.

### Step 7.1 — S3 Configuration
```java
@Configuration
public class S3Config {
    @Bean
    public AmazonS3 amazonS3() {
        return AmazonS3ClientBuilder.standard()
            .withRegion("ap-south-1")
            .withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(accessKey, secretKey)))
            .build();
    }
}
```

### Step 7.2 — S3 Service

| Method | What It Does |
|--------|-------------|
| `uploadFile(MultipartFile file, String folder)` | Validate file type/size → generate unique filename → upload to S3 → return URL |
| `deleteFile(String fileUrl)` | Extract key from URL → delete from S3 |

- Folder structure: `avatars/{userId}/filename.jpg`, `posts/{postId}/filename.jpg`
- Max file size: 5MB
- Allowed types: jpg, jpeg, png, gif, webp

### Step 7.3 — Avatar Upload Endpoint
| Endpoint | What It Does |
|----------|-------------|
| `POST /api/v1/users/me/avatar` | Upload image → S3 → save URL to user.avatarUrl |

**Test:** Upload avatar → Check S3 bucket → Verify URL saved in user profile ✅

---

## Phase 8: Post Service (Core Feature)

> **Why now?** Users exist, follows work, S3 works. Now they can create content.

### Step 8.1 — Post Service

| Method | Logic |
|--------|-------|
| `createPost(PostRequest, MultipartFile)` | Save post → if image, upload to S3 → set imageUrl → save |
| `getPostById(UUID id)` | Find post → throw if deleted → return |
| `updatePost(UUID id, PostRequest)` | Verify ownership → update content |
| `deletePost(UUID id)` | Verify ownership → soft delete (isDeleted = true) |
| `getFeed(Pageable)` | Get list of users I follow → fetch their posts → sort by createdAt DESC |
| `searchPosts(String query, Pageable)` | Search by content LIKE query |

### Step 8.2 — Post Controller

| Endpoint | Method |
|----------|--------|
| `POST /api/v1/posts` | createPost (multipart: content + image) |
| `GET /api/v1/posts/{id}` | getPostById |
| `PUT /api/v1/posts/{id}` | updatePost |
| `DELETE /api/v1/posts/{id}` | deletePost |
| `GET /api/v1/posts/feed` | getFeed (paginated) |
| `GET /api/v1/posts/search?q=` | searchPosts |

### Step 8.3 — Post Repository Custom Queries
```java
Page<Post> findByAuthorIdInAndIsDeletedFalseOrderByCreatedAtDesc(
    List<UUID> authorIds, Pageable pageable);  // for feed

Page<Post> findByContentContainingIgnoreCaseAndIsDeletedFalse(
    String keyword, Pageable pageable);  // for search
```

**Test:** Create post with image → Verify on S3 → Get feed → Delete → Verify soft deleted ✅

---

## Phase 9: Like System

> **Why now?** Posts exist. Now users can interact with them.

### Step 9.1 — Like Repository
```java
Optional<Like> findByUserIdAndPostId(UUID userId, UUID postId);
boolean existsByUserIdAndPostId(UUID userId, UUID postId);
long countByPostId(UUID postId);
```

### Step 9.2 — Like Service

| Method | Logic |
|--------|-------|
| `toggleLike(UUID postId)` | If already liked → remove like → decrement post.likeCount. If not liked → add like → increment post.likeCount |

> Toggle pattern is better UX than separate like/unlike endpoints.

### Step 9.3 — Like Controller

| Endpoint | Method |
|----------|--------|
| `POST /api/v1/posts/{id}/like` | toggleLike → returns `{ "liked": true/false }` |

**Test:** Like → Verify likeCount = 1 → Like again → Verify likeCount = 0 ✅

---

## Phase 10: Comment System

### Step 10.1 — Comment Repository
```java
Page<Comment> findByPostIdOrderByCreatedAtDesc(UUID postId, Pageable pageable);
long countByPostId(UUID postId);
```

### Step 10.2 — Comment Service

| Method | Logic |
|--------|-------|
| `addComment(UUID postId, CommentRequest)` | Save comment → increment post.commentCount |
| `getComments(UUID postId, Pageable)` | Return paginated comments for a post |
| `deleteComment(UUID commentId)` | Verify ownership → delete → decrement post.commentCount |

### Step 10.3 — Comment Controller

| Endpoint | Method |
|----------|--------|
| `POST /api/v1/posts/{id}/comments` | addComment |
| `GET /api/v1/posts/{id}/comments` | getComments |
| `DELETE /api/v1/comments/{id}` | deleteComment |

**Test:** Add comment → Verify commentCount → Get comments → Delete → Verify count decreased ✅

---

## Phase 11: Report System

### Step 11.1 — Report Service

| Method | Logic |
|--------|-------|
| `reportPost(UUID postId, ReportRequest)` | Save report with status PENDING |
| `getReports(Pageable)` | Admin only — get all pending reports |
| `resolveReport(UUID reportId, String adminNotes)` | Admin — set status RESOLVED |
| `dismissReport(UUID reportId, String adminNotes)` | Admin — set status DISMISSED |

### Step 11.2 — Integrate into Admin Controller (Phase 15)

**Test:** Report a post → Admin views reports → Resolve/Dismiss ✅

---

## Phase 12: Redis Integration

> **Why now?** Core features work. Now optimize performance.

### Step 12.1 — Redis Configuration
```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

### Step 12.2 — Where to Use Redis

| Use Case | Key Pattern | TTL |
|----------|-------------|-----|
| User profile cache | `user:{userId}` | 30 min |
| Post cache | `post:{postId}` | 15 min |
| Feed cache | `feed:{userId}:page:{pageNum}` | 5 min |
| Email verify token | `email-verify:{token}` | 24 hours |
| Password reset token | `password-reset:{token}` | 15 min |
| Online status | `online:{userId}` | 5 min (refresh on heartbeat) |
| Rate limiting | `rate:{userId}:{endpoint}` | 1 min |

### Step 12.3 — Implement Cache Service
- `cacheUserProfile()` / `getCachedUserProfile()`
- `invalidateUserCache()` — call on profile update
- `cachePost()` / `invalidatePostCache()` — call on post create/update/delete
- Move token storage from DB to Redis (forgot-password, email-verify)

**Test:** Hit profile API → Check Redis has cached data → Update profile → Verify cache invalidated ✅

---

## Phase 13: Kafka Integration

> **Why now?** Core features + caching done. Now make it event-driven.

### Step 13.1 — Kafka Configuration
- Topics: `post.created`, `post.liked`, `user.followed`, `user.signup`
- Producer config + Consumer config
- Consumer groups

### Step 13.2 — Kafka Producer Service
```java
kafkaTemplate.send("post.created", postEvent);
kafkaTemplate.send("user.followed", followEvent);
```

### Step 13.3 — Where to Produce Events

| Event | When | Topic |
|-------|------|-------|
| New post created | After saving post | `post.created` |
| Post liked | After toggling like | `post.liked` |
| User followed someone | After saving follow | `user.followed` |
| New user registered | After registration | `user.signup` |
| Post reported | After saving report | `post.reported` |

### Step 13.4 — Kafka Consumers

| Topic | Consumer Action |
|-------|----------------|
| `post.created` | Invalidate feed cache for all followers |
| `user.followed` | Invalidate feed cache for the follower |
| `user.signup` | Log analytics event / send welcome email |
| `post.reported` | Notify admin (future: push notification) |

**Test:** Create post → Check Kafka topic received event → Verify consumer processed it ✅

---

## Phase 14: Real-Time Chat (WebSocket)

> **Why now?** All REST APIs done. Chat is a separate feature.

### Step 14.1 — WebSocket Configuration
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    // STOMP over SockJS
    // /ws endpoint
    // /topic, /queue prefixes
}
```

### Step 14.2 — Chat Service

| Method | Logic |
|--------|-------|
| `sendMessage(UUID receiverId, MessageRequest)` | Save to DB → send via WebSocket → publish to Kafka |
| `getConversations()` | Get list of users I've chatted with + last message + unread count |
| `getMessages(UUID userId, Pageable)` | Get message history between me and userId |
| `markAsRead(UUID senderId)` | Set isRead = true for all messages from sender |

### Step 14.3 — Chat Controller (REST + WebSocket)

| Type | Endpoint | Method |
|------|----------|--------|
| WS | `/ws/chat` | WebSocket connection |
| REST | `GET /api/v1/chat/conversations` | getConversations |
| REST | `GET /api/v1/chat/{userId}/messages` | getMessages |

### Step 14.4 — Online Status (Redis)
- On WebSocket connect → set `online:{userId}` in Redis (TTL 5 min)
- On WebSocket disconnect → delete key
- Heartbeat every 3 min → refresh TTL

**Test:** Connect two users via WebSocket → Send message → Verify real-time delivery → Check DB persistence ✅

---

## Phase 15: Admin Dashboard APIs

> **Why now?** All user-facing features done. Admin needs data from all modules.

### Step 15.1 — Admin Service

| Method | What It Returns |
|--------|----------------|
| `getDashboardStats()` | Total users, total posts, total reports pending, active users today |
| `getUserGrowth(period)` | Daily/weekly/monthly new user counts |
| `getAllUsers(Pageable)` | Paginated user list with stats |
| `banUser(UUID userId)` | Set user.isActive = false |
| `unbanUser(UUID userId)` | Set user.isActive = true |
| `deleteUser(UUID userId)` | Hard delete user + cascade |
| `getPendingReports(Pageable)` | All reports with status PENDING |
| `resolveReport(UUID reportId)` | Set status RESOLVED, optionally delete post |
| `getMostActiveUsers()` | Top 10 users by post count |

### Step 15.2 — Admin Controller
- All endpoints under `/api/v1/admin/**`
- Protected by `@PreAuthorize("hasRole('ADMIN')")`

### Step 15.3 — Admin Repository Queries
```java
// UserRepository
long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
List<Object[]> countUsersByDateGrouped();  // for growth chart

// PostRepository
long countByIsDeletedFalse();

// ReportRepository
long countByStatus(ReportStatus status);
```

**Test:** Login as admin → Get dashboard → Ban a user → Verify user can't login ✅

---

## Phase 16: OAuth2 Login (Google & GitHub)

> **Why later?** Local auth works. OAuth is an enhancement.

### Step 16.1 — OAuth2 Flow
1. Frontend redirects user to Google/GitHub
2. User authorizes → callback with authorization code
3. Frontend sends code to backend
4. Backend exchanges code for access token with provider
5. Backend fetches user profile from provider
6. If user exists (by email) → login → return JWT
7. If new user → create account (auto-generate username) → return JWT

### Step 16.2 — OAuth Service
- `googleLogin(String authCode)` → exchange with Google → get profile → login or register
- `githubLogin(String authCode)` → exchange with GitHub → get profile → login or register

**Test:** Login with Google → Verify user created with authProvider=GOOGLE → Login again → Same user ✅

---

## Phase 17: Testing

### Step 17.1 — Unit Tests (Service Layer)
- Mock repositories with Mockito
- Test every service method
- Test edge cases (duplicate follow, self-follow, delete others' post)

### Step 17.2 — Integration Tests
- `@SpringBootTest` with Testcontainers (PostgreSQL, Redis)
- Test full flow: Register → Login → Create Post → Like → Comment

### Step 17.3 — Controller Tests
- `@WebMvcTest` with `MockMvc`
- Test request validation
- Test auth (401 without token, 403 wrong role)

**Target:** 80%+ coverage on service layer ✅

---

## Phase 18: Docker & Docker Compose

### Step 18.1 — Dockerfile (multi-stage)
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 18.2 — docker-compose.yml
- PostgreSQL
- Redis
- Kafka + Zookeeper
- ConnectSphere app (depends_on all above)

**Test:** `docker-compose up` → Hit API → Everything works ✅

---

## Phase 19: Jenkins CI/CD Pipeline

### Step 19.1 — Jenkinsfile
```
Pipeline: Checkout → Build → Test → Docker Build → Push to ECR → Deploy
```

**Test:** Push to GitHub → Jenkins triggers → Docker image pushed → Deployed ✅

---

## Phase 20: AWS Deployment

- **ECS/EC2** — Run Docker containers
- **RDS** — PostgreSQL
- **ElastiCache** — Redis
- **S3** — Already configured (media files)
- **CloudWatch** — Logging & monitoring
- **ALB** — Load balancer in front of ECS

---

## 📋 Quick Reference: Build Order

| # | What | Depends On |
|---|------|-----------|
| 1 | Project setup + config | Nothing |
| 2 | Entities + Repositories | Config |
| 3 | Exception handling | Nothing |
| 4 | **Auth + Security (JWT)** | User entity |
| 5 | User profile CRUD | Auth |
| 6 | Follow/Unfollow | User |
| 7 | S3 file upload | Config |
| 8 | **Post CRUD + Feed** | User + Follow + S3 |
| 9 | Like system | Post |
| 10 | Comment system | Post |
| 11 | Report system | Post + User |
| 12 | **Redis caching** | All CRUD done |
| 13 | **Kafka events** | All CRUD done |
| 14 | WebSocket chat | Auth + Redis |
| 15 | Admin dashboard | Everything |
| 16 | OAuth2 | Auth |
| 17 | Testing | Everything |
| 18 | Docker | Everything |
| 19 | Jenkins CI/CD | Docker |
| 20 | AWS deployment | Docker + Jenkins |
