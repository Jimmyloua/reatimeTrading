# Phase 1: Foundation and User Management - Research

**Researched:** 2026-03-21
**Domain:** Spring Security 7 JWT Authentication, Spring Session Redis, File Upload Handling
**Confidence:** HIGH

## Summary

This phase establishes the authentication and user management foundation for the Real-Time Trading Platform. The implementation uses Spring Boot 3.5.x with Spring Security 7 for JWT-based stateless authentication, Spring Session with Redis for distributed session management, and local filesystem storage for avatar uploads (v1). All decisions from CONTEXT.md are locked and must be followed.

**Primary recommendation:** Implement JWT-based authentication with Spring Security 7 filter chain, using BCrypt password hashing (Spring Security default), 15-minute access tokens with 7-day refresh tokens, and 200x200px avatar thumbnails stored in a configurable local directory.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Registration Flow
- **D-01:** Users receive immediate access after registration (no email verification required for v1)
- **D-02:** Registration collects only email and password (display name set later in profile)
- **D-03:** Password requirements: minimum 8 characters, no complexity rules
- **D-04:** Duplicate email registration returns generic error "Email already registered" (prevents email enumeration)

#### Profile Setup Timing
- **D-05:** Profile setup is optional after registration - users choose when to complete it
- **D-06:** Profile (display name) required before any interaction: creating listings or starting chats
- **D-07:** When users attempt an action requiring profile, show inline profile setup prompt (modal or form)
- **D-08:** Users without a display name show "New User" placeholder until they set one

#### Avatar Upload
- **D-09:** Avatar images stored on local filesystem for v1 (development setup - migration to S3 planned for production)
- **D-10:** Maximum file size: 5 MB for avatar uploads
- **D-11:** Supported formats: JPEG, PNG, WebP
- **D-12:** Users can only replace current avatar (no multiple avatar gallery)

### Claude's Discretion

- Email format validation approach
- Password hashing algorithm (Spring Security default recommended)
- JWT token expiration time and refresh strategy
- Default avatar image for users who don't upload one
- Avatar image dimensions and resizing behavior
- Session timeout policies

### Deferred Ideas (OUT OF SCOPE)

None - discussion stayed within phase scope.

</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-01 | User can register with email and password | Spring Security 7 UserDetailsService, BCrypt, registration endpoint pattern |
| AUTH-02 | User can log in with email and password | JWT token generation, authentication filter, login endpoint |
| AUTH-03 | User session persists across browser refresh (JWT-based) | Refresh token strategy, Redis session storage, frontend token persistence |
| AUTH-04 | User can log out from any page | Token invalidation, Redis blacklist, frontend token clearing |
| PROF-01 | User can create profile with display name | User entity update, display name validation, uniqueness check optional |
| PROF-02 | User can upload avatar image | File upload endpoint, image validation, thumbnail generation, local storage |
| PROF-03 | User can view their own profile with listing count and join date | Profile API endpoint, user entity with createdAt, listing count query |
| PROF-04 | User can view other users' profiles | Public profile endpoint, privacy considerations, listing count visible |

</phase_requirements>

## Standard Stack

### Core Backend
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 3.5.x | Backend framework | Mandated. Virtual threads (JDK 21), Spring Security 7.x included |
| JDK | 21 LTS | Runtime | Mandated. Virtual threads for high-concurrency |
| Spring Security | 7.0.x | Authentication/authorization | JWT-based auth, method-level security |
| Spring Data JPA | 4.0.x | ORM and data access | User entity persistence, derived queries |
| Spring Data Redis | 4.0.x | Redis integration | Session storage, token blacklist |
| Spring Session | 3.5.x | Session management | Distributed sessions in Redis |
| JJWT | 0.13.x | JWT token handling | Token generation/validation |
| MySQL Connector | 8.x | Database driver | MySQL 8 connectivity |
| Lettuce | 7.5.x | Redis client | Async Redis, Spring Boot default |

### Core Frontend
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React | 19.2.x | UI framework | Mandated. Actions, concurrent rendering |
| TypeScript | 5.9.x | Type safety | Strict mode for all frontend code |
| Vite | 8.x | Build tool | Fast HMR, development server |
| TanStack Query | 5.91.x | Server state management | Auth state, API caching |
| Zustand | 5.0.x | Client state management | UI state, auth token storage |
| Axios | 1.13.x | HTTP client | API calls, auth interceptors |
| React Hook Form | 7.71.x | Form handling | Login/register forms |
| Zod | 4.3.x | Schema validation | Form validation, API validation |
| TailwindCSS | 4.2.x | Styling | Utility-first CSS |
| Lucide React | 0.577.x | Icons | Lightweight, tree-shakeable |

### Supporting Backend
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| MapStruct | 1.6.x | DTO mapping | User entity to UserDTO mapping |
| Liquibase | 12.x | Database migrations | Schema versioning |
| SpringDoc OpenAPI | 3.0.x | API documentation | Swagger UI, OpenAPI spec |
| Testcontainers | 2.0.x | Integration testing | MySQL, Redis containers for tests |

### Supporting Frontend
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| React Dropzone | 15.0.x | File uploads | Avatar upload component |
| clsx | 2.x | Conditional classes | Dynamic class composition |
| tailwind-merge | 3.x | Tailwind class merging | Dedupe conflicting classes |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Zustand for auth state | localStorage directly | Zustand provides hydration, TypeScript safety, devtools |
| Axios | fetch API | Axios provides interceptors for auth token injection |
| BCrypt | Argon2 | Argon2 is newer but BCrypt is Spring Security default |
| Local filesystem | S3 | S3 requires infrastructure; local is simpler for v1 |

**Installation:**

Backend (Maven pom.xml):
```xml
<!-- Core -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.13.x</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.13.x</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.13.x</version>
    <scope>runtime</scope>
</dependency>

<!-- Database -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Utilities -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.x</version>
</dependency>
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

Frontend (package.json):
```json
{
  "dependencies": {
    "react": "^19.2.4",
    "react-dom": "^19.2.4",
    "@tanstack/react-query": "^5.91.3",
    "zustand": "^5.0.12",
    "axios": "^1.13.6",
    "react-hook-form": "^7.71.2",
    "zod": "^4.3.6",
    "react-dropzone": "^15.0.0",
    "lucide-react": "^0.577.0"
  },
  "devDependencies": {
    "typescript": "^5.9.3",
    "vite": "^8.0.1",
    "tailwindcss": "^4.2.2",
    "clsx": "^2.1.0",
    "tailwind-merge": "^3.0.0"
  }
}
```

**Version verification:** Versions verified from npm registry on 2026-03-21. Spring Boot 3.5.x versions from CLAUDE.md (verified 2026-03-21).

## Architecture Patterns

### Recommended Project Structure

```
backend/
├── src/main/java/com/tradingplatform/
│   ├── TradingPlatformApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java           # SecurityFilterChain, JWT filter
│   │   ├── RedisConfig.java              # Redis connection, session config
│   │   ├── JwtConfig.java                # JWT properties, secret key
│   │   └── WebConfig.java                # CORS, static resources
│   ├── security/
│   │   ├── JwtAuthenticationFilter.java  # JWT validation filter
│   │   ├── JwtTokenProvider.java         # Token generation/validation
│   │   └── UserPrincipal.java            # UserDetails implementation
│   ├── user/
│   │   ├── User.java                     # User entity
│   │   ├── UserController.java           # Registration, profile endpoints
│   │   ├── UserService.java              # Business logic
│   │   ├── UserRepository.java           # JPA repository
│   │   └── dto/
│   │       ├── RegisterRequest.java
│   │       ├── LoginRequest.java
│   │       ├── LoginResponse.java
│   │       └── UserProfileResponse.java
│   ├── avatar/
│   │   ├── AvatarController.java         # Upload/delete endpoints
│   │   ├── AvatarService.java            # File handling, validation
│   │   └── AvatarStorageService.java     # Local filesystem storage
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── ApiException.java
│       └── ErrorCode.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── db/changelog/                     # Liquibase migrations
└── pom.xml

frontend/
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── api/
│   │   ├── client.ts                     # Axios instance with interceptors
│   │   └── authApi.ts                    # Auth endpoints
│   ├── stores/
│   │   └── authStore.ts                  # Zustand auth state
│   ├── hooks/
│   │   └── useAuth.ts                    # Auth hook for components
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── RegisterPage.tsx
│   │   └── ProfilePage.tsx
│   ├── components/
│   │   ├── AvatarUpload.tsx
│   │   ├── ProtectedRoute.tsx
│   │   └── ProfilePrompt.tsx
│   └── types/
│       └── user.ts
├── index.html
├── vite.config.ts
└── package.json
```

### Pattern 1: JWT Authentication Filter Chain

**What:** Spring Security 7 filter chain that validates JWT tokens on each request and sets authentication context.

**When to use:** All authenticated endpoints. This is the core authentication mechanism.

**Example:**
```java
// Source: Spring Security 7 documentation pattern
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/uploads/avatars/**").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### Pattern 2: JWT Token Provider

**What:** Service for generating and validating JWT tokens with configurable expiration.

**When to use:** On login (generate), on each request (validate), on refresh (generate new).

**Example:**
```java
// Source: JJWT 0.13.x documentation pattern
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token.expiration:900000}") // 15 minutes
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:604800000}") // 7 days
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateAccessToken(UserPrincipal user) {
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("roles", user.getAuthorities())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
            .signWith(getSigningKey())
            .compact();
    }

    public String generateRefreshToken(UserPrincipal user) {
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("type", "refresh")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
            .signWith(getSigningKey())
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }
}
```

### Pattern 3: User Entity with Profile

**What:** JPA entity that separates authentication fields from profile fields, supporting optional display name.

**When to use:** All user data storage, supports locked decisions D-02, D-05, D-08.

**Example:**
```java
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "is_profile_complete", nullable = false)
    private boolean profileComplete = false;

    @Transient
    public String getDisplayNameOrFallback() {
        return displayName != null ? displayName : "New User";
    }
}
```

### Pattern 4: Avatar Upload with Validation

**What:** File upload service that validates file type, size, generates thumbnail, and stores to local filesystem.

**When to use:** Avatar upload endpoint, follows D-09, D-10, D-11, D-12.

**Example:**
```java
@Service
@RequiredArgsConstructor
public class AvatarService {

    @Value("${avatar.upload.dir:./uploads/avatars}")
    private String uploadDir;

    @Value("${avatar.max-size:5242880}") // 5 MB
    private long maxFileSize;

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp"
    );

    private static final int THUMBNAIL_SIZE = 200;

    public String storeAvatar(MultipartFile file, Long userId) {
        validateFile(file);

        String filename = generateFilename(userId, file.getContentType());
        Path targetPath = Paths.get(uploadDir, filename);

        try {
            // Create directory if needed
            Files.createDirectories(targetPath.getParent());

            // Generate thumbnail
            BufferedImage original = ImageIO.read(file.getInputStream());
            BufferedImage thumbnail = createThumbnail(original);
            ImageIO.write(thumbnail, getFormat(file.getContentType()), targetPath.toFile());

            return filename;
        } catch (IOException e) {
            throw new ApiException(ErrorCode.AVATAR_UPLOAD_FAILED, "Failed to store avatar");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_AVATAR, "File is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new ApiException(ErrorCode.INVALID_AVATAR, "File exceeds 5 MB limit");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ApiException(ErrorCode.INVALID_AVATAR,
                "Only JPEG, PNG, and WebP are allowed");
        }
    }

    private BufferedImage createThumbnail(BufferedImage original) {
        // Maintain aspect ratio, fit within 200x200
        int width = original.getWidth();
        int height = original.getHeight();
        int newSize = Math.min(width, height);

        int x = (width - newSize) / 2;
        int y = (height - newSize) / 2;

        BufferedImage cropped = original.getSubimage(x, y, newSize, newSize);
        BufferedImage thumbnail = new BufferedImage(THUMBNAIL_SIZE, THUMBNAIL_SIZE, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = thumbnail.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cropped, 0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE, null);
        g.dispose();

        return thumbnail;
    }

    private String generateFilename(Long userId, String contentType) {
        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
        return "user_%d.%s".formatted(userId, extension);
    }
}
```

### Pattern 5: Zustand Auth Store with Persistence

**What:** Frontend auth state management with localStorage persistence and hydration.

**When to use:** Managing auth tokens and user state on frontend.

**Example:**
```typescript
// Source: Zustand 5.x best practices
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: User | null;
  isAuthenticated: boolean;
  setTokens: (access: string, refresh: string) => void;
  setUser: (user: User) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      setTokens: (access, refresh) =>
        set({ accessToken: access, refreshToken: refresh, isAuthenticated: true }),
      setUser: (user) => set({ user }),
      logout: () =>
        set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false }),
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    }
  )
);
```

### Pattern 6: Axios Interceptor for Token Injection

**What:** Axios instance that automatically injects access tokens and handles 401 responses for token refresh.

**When to use:** All API calls from frontend.

**Example:**
```typescript
// Source: Axios 1.13.x interceptor pattern
import axios from 'axios';
import { useAuthStore } from './stores/authStore';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: Add access token
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: Handle 401, attempt refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = useAuthStore.getState().refreshToken;
        const response = await axios.post('/api/auth/refresh', { refreshToken });
        const { accessToken, refreshToken: newRefresh } = response.data;

        useAuthStore.getState().setTokens(accessToken, newRefresh);
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;

        return apiClient(originalRequest);
      } catch {
        useAuthStore.getState().logout();
        window.location.href = '/login';
        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

### Pattern 7: Registration with Generic Error

**What:** Registration endpoint that returns generic error for duplicate email (D-04).

**When to use:** User registration endpoint.

**Example:**
```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(request.getEmail(), request.getPassword());

            UserPrincipal principal = UserPrincipal.create(user);
            String accessToken = tokenProvider.generateAccessToken(principal);
            String refreshToken = tokenProvider.generateRefreshToken(principal);

            return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken, user.getId()));
        } catch (DataIntegrityViolationException e) {
            // D-04: Generic error to prevent email enumeration
            throw new ApiException(ErrorCode.REGISTRATION_FAILED,
                "Registration failed. Please try again.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = tokenProvider.generateAccessToken(principal);
        String refreshToken = tokenProvider.generateRefreshToken(principal);

        return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken, principal.getId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        userService.invalidateRefreshToken(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }
}
```

### Anti-Patterns to Avoid

- **Storing JWT in localStorage without XSS consideration:** Access tokens in memory, only refresh tokens in localStorage (HttpOnly cookies preferred but require CSRF protection)
- **Using JWT secret as plain string:** Must use Base64-encoded 256-bit key minimum for HS256
- **Storing passwords without BCrypt:** Spring Security's BCryptPasswordEncoder is the standard
- **Validating email with complex regex:** Use simple format check + verification email (future), don't block valid addresses
- **Returning user exists error on registration:** Violates D-04, enables email enumeration attacks
- **Synchronous file upload blocking threads:** Use Spring's async processing for large uploads
- **Hardcoding avatar paths:** Use configurable `avatar.upload.dir` property
- **Not validating MIME type on uploads:** Only extension validation allows malicious files

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Password hashing | Custom hash function | BCryptPasswordEncoder | Spring Security default, proven secure, handles salt |
| JWT generation | Custom JWT builder | JJWT library | Edge cases, security, spec compliance |
| Token validation | Manual parsing | JJWT parser + JwtDecoder | Signature verification, expiration handling |
| Session management | Custom session cache | Spring Session + Redis | Distributed, cluster-safe, TTL support |
| File upload validation | Custom MIME check | Spring MultipartFile + Apache Tika | True MIME detection, not just extension |
| Image resizing | Custom scaling code | Thumbnailator or imgscalr | Aspect ratio, quality, format handling |
| CORS configuration | Manual headers | Spring CORS config | Preflight handling, credentials, origin patterns |
| Form validation | Manual validation | React Hook Form + Zod | Type-safe, performant, accessible errors |
| API state | useState + useEffect | TanStack Query | Caching, background refetch, optimistic updates |

**Key insight:** Spring Security 7 provides production-ready authentication infrastructure. Building custom auth almost always introduces vulnerabilities. JJWT handles edge cases like clock skew, algorithm confusion, and token injection that custom implementations miss.

## Common Pitfalls

### Pitfall 1: JWT Secret Key Too Short

**What goes wrong:** Using a short or predictable secret key allows token forgery attacks.

**Why it happens:** Developers use simple strings like "my-secret-key" or "development" not realizing JWT requires a cryptographically strong key.

**How to avoid:** Generate a Base64-encoded 256-bit (32-byte) minimum key:
```bash
openssl rand -base64 32
```
Store in environment variable, never in code.

**Warning signs:** JWT signature verification fails inconsistently, tokens work on one instance but not another.

### Pitfall 2: Refresh Token Not Invalidated on Logout

**What goes wrong:** Logging out doesn't invalidate refresh tokens, allowing continued access.

**Why it happens:** Stateless JWT design makes token invalidation challenging.

**How to avoid:** Use Redis to store refresh token IDs (or hash) with TTL matching refresh token expiration. On logout, add to blacklist. Check blacklist during refresh.

**Warning signs:** Users remain logged in after logout, refresh token continues to work.

### Pitfall 3: Email Enumeration via Registration

**What goes wrong:** Registration endpoint returns different errors for "email already exists" vs "other errors", allowing attackers to discover registered emails.

**Why it happens:** Helpful error messages seem user-friendly but leak information.

**How to avoid:** Per D-04, always return generic "Registration failed" or "Email already registered" message. Use same message for existing email and other errors.

**Warning signs:** Registration returns "Email already registered" only for existing emails, different message for other errors.

### Pitfall 4: Avatar Upload Path Traversal

**What goes wrong:** Attacker uploads file with path like `../../../etc/passwd` or `../../config/application.yml`.

**Why it happens:** Using user-provided filename directly in file path.

**How to avoid:** Never use user-provided filename. Generate filename from user ID and whitelist extension. Validate final path is within allowed directory.

**Warning signs:** Filenames contain `..`, `/`, or `\` characters.

### Pitfall 5: Missing Profile Complete Check

**What goes wrong:** Users can create listings or start chats without display name, violating D-06.

**Why it happens:** Auth checks only verify user is logged in, not that profile is complete.

**How to avoid:** Add `@ProfileRequired` annotation or check `user.isProfileComplete()` in controllers that require profile. Return 403 with `PROFILE_INCOMPLETE` error code.

**Warning signs:** Listings appear with "New User" placeholder, chat messages show null display name.

### Pitfall 6: File Upload Memory Exhaustion

**What goes wrong:** Large file uploads consume all memory, crashing the server.

**Why it happens:** Spring loads entire MultipartFile into memory by default.

**How to avoid:** Set `spring.servlet.multipart.max-file-size=5MB` and `max-request-size=5MB`. Consider streaming for larger files (future S3 migration).

**Warning signs:** OutOfMemoryError during uploads, slow response times during concurrent uploads.

## Code Examples

### Complete User Entity with Liquibase Migration

```java
// User.java - JPA Entity
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    @Email
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "display_name", length = 100)
    @Size(max = 100)
    private String displayName;

    @Column(name = "avatar_path", length = 500)
    private String avatarPath;

    @Column(name = "refresh_token_hash", length = 64)
    private String refreshTokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "is_profile_complete", nullable = false)
    @Builder.Default
    private boolean profileComplete = false;

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    // Convenience method for D-08
    public String getDisplayNameOrFallback() {
        return displayName != null ? displayName : "New User";
    }

    // Called after display name is set
    public void updateProfileComplete() {
        this.profileComplete = displayName != null && !displayName.isBlank();
    }
}
```

```xml
<!-- db/changelog/001-create-users-table.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="001-create-users-table" author="system">
        <createTable tableName="users">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="email" type="VARCHAR(255)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="password" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="display_name" type="VARCHAR(100)"/>
            <column name="avatar_path" type="VARCHAR(500)"/>
            <column name="refresh_token_hash" type="VARCHAR(64)"/>
            <column name="created_at" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMP"/>
            <column name="is_profile_complete" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex tableName="users" indexName="idx_users_email" unique="true">
            <column name="email"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

### Profile Required Aspect

```java
// Annotation for methods requiring complete profile
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProfileRequired {
}

// Aspect that checks profile completion
@Aspect
@Component
@RequiredArgsConstructor
public class ProfileRequiredAspect {

    private final UserRepository userRepository;

    @Before("@annotation(profileRequired) && args(.., principal)")
    public void checkProfileComplete(JoinPoint joinPoint, ProfileRequired profileRequired, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (!user.isProfileComplete()) {
            throw new ApiException(ErrorCode.PROFILE_INCOMPLETE,
                "Profile setup required. Please set your display name.");
        }
    }
}

// Usage in controller
@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    @PostMapping
    @ProfileRequired // D-06: Profile required before creating listings
    public ResponseEntity<ListingResponse> createListing(
        @Valid @RequestBody CreateListingRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        // ...
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Session-based auth with server-side sessions | JWT with stateless validation | 2015+ | Horizontal scaling without session affinity |
| Custom password hashing | BCrypt with adaptive cost | 2010+ | Protection against GPU attacks |
| In-memory token storage | Redis-backed token blacklist | 2015+ | Token revocation for logout |
| Multiple avatar sizes | Single thumbnail + on-demand resize | 2018+ | Storage efficiency |
| Form-encoded login | JSON login with JWT response | 2016+ | SPA-friendly, mobile-friendly |

**Deprecated/outdated:**
- **SHA-256 for passwords:** Use BCrypt only. SHA families are too fast for password hashing.
- **JWT in URL parameters:** Security risk, exposes tokens in logs and history. Use Authorization header.
- **Synchronous file processing:** Use async for file I/O. Virtual threads in JDK 21 make this simpler.
- **JSESSIONID for SPAs:** Cookies with CSRF tokens or JWT tokens. JSESSIONID doesn't scale horizontally.

## Open Questions

1. **Default Avatar Image**
   - What we know: Users without uploaded avatar need a placeholder.
   - What's unclear: Should this be a generated initial-based avatar, a generic silhouette, or a placeholder image?
   - Recommendation: Use initial-based avatar (user's first letter of email or display name) with consistent color. More personal than silhouette, no storage needed.

2. **Email Format Validation Strictness**
   - What we know: Simple regex can block valid emails; complex regex still misses edge cases.
   - What's unclear: How strict should validation be?
   - Recommendation: Use Spring's `@Email` annotation which does basic format check. Don't over-validate. Legitimate users with unusual email formats should not be blocked.

3. **Refresh Token Rotation**
   - What we know: Refresh tokens can be stolen; rotation limits damage.
   - What's unclear: Should we implement refresh token rotation (issue new refresh token with each access token refresh)?
   - Recommendation: Yes, implement rotation. When refresh token is used, invalidate old one and issue new. Detect reuse attempts as potential theft, invalidate all tokens for user.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Testcontainers 2.0.x |
| Config file | `src/test/resources/application-test.yml` |
| Quick run command | `mvn test -Dtest="*Test" -DfailIfNoTests=false` |
| Full suite command | `mvn verify` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|--------------|
| AUTH-01 | User can register with email and password | integration | `mvn test -Dtest=AuthControllerTest#testRegisterSuccess` | Wave 0 |
| AUTH-01 | Duplicate email returns generic error (D-04) | unit | `mvn test -Dtest=UserServiceTest#testRegisterDuplicateEmail` | Wave 0 |
| AUTH-02 | User can log in with valid credentials | integration | `mvn test -Dtest=AuthControllerTest#testLoginSuccess` | Wave 0 |
| AUTH-02 | Invalid password returns 401 | integration | `mvn test -Dtest=AuthControllerTest#testLoginInvalidPassword` | Wave 0 |
| AUTH-03 | JWT token validates correctly | unit | `mvn test -Dtest=JwtTokenProviderTest#testValidateToken` | Wave 0 |
| AUTH-03 | Refresh token returns new access token | integration | `mvn test -Dtest=AuthControllerTest#testRefreshToken` | Wave 0 |
| AUTH-04 | Logout invalidates refresh token | integration | `mvn test -Dtest=AuthControllerTest#testLogout` | Wave 0 |
| PROF-01 | User can set display name | integration | `mvn test -Dtest=UserControllerTest#testSetDisplayName` | Wave 0 |
| PROF-02 | Avatar upload accepts valid files | integration | `mvn test -Dtest=AvatarControllerTest#testUploadAvatar` | Wave 0 |
| PROF-02 | Avatar upload rejects invalid files | unit | `mvn test -Dtest=AvatarServiceTest#testValidateInvalidFile` | Wave 0 |
| PROF-03 | User can view own profile | integration | `mvn test -Dtest=UserControllerTest#testGetOwnProfile` | Wave 0 |
| PROF-04 | User can view other users' profiles | integration | `mvn test -Dtest=UserControllerTest#testGetOtherProfile` | Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn test -Dtest="*Test"`
- **Per wave merge:** `mvn verify`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/.../config/TestcontainersConfiguration.java` — Testcontainers config for MySQL/Redis
- [ ] `src/test/java/.../config/TestRedisConfiguration.java` — Embedded Redis or Testcontainers
- [ ] `src/test/resources/application-test.yml` — Test configuration
- [ ] `src/test/java/.../controller/AuthControllerTest.java` — AUTH-01, AUTH-02, AUTH-03, AUTH-04
- [ ] `src/test/java/.../controller/UserControllerTest.java` — PROF-01, PROF-03, PROF-04
- [ ] `src/test/java/.../controller/AvatarControllerTest.java` — PROF-02
- [ ] `src/test/java/.../service/UserServiceTest.java` — Unit tests
- [ ] `src/test/java/.../service/AvatarServiceTest.java` — Unit tests
- [ ] `src/test/java/.../security/JwtTokenProviderTest.java` — Unit tests

*(If no gaps: "None - existing test infrastructure covers all phase requirements")*

## Sources

### Primary (HIGH confidence)
- Spring Security 7 Reference Documentation - JWT authentication, filter chains, method security
- Spring Session Reference Documentation - Redis session configuration
- JJWT GitHub Documentation - Token generation, parsing, validation patterns
- Spring Boot 3.5 Documentation - Virtual threads, auto-configuration
- CLAUDE.md Project Instructions - Mandated stack versions, verified 2026-03-21

### Secondary (MEDIUM confidence)
- npm Registry - Verified React 19.2.4, TanStack Query 5.91.3, Zustand 5.0.12, Axios 1.13.6, TypeScript 5.9.3, Vite 8.0.1, TailwindCSS 4.2.2, React Hook Form 7.71.2, Zod 4.3.6, React Dropzone 15.0.0, Lucide React 0.577.0, React Leaflet 5.0.0

### Tertiary (LOW confidence)
- Web patterns for file upload validation - Industry best practices, recommend verification with Spring documentation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All versions verified from npm registry and project mandates
- Architecture: HIGH - Spring Security 7 patterns are well-documented, JJWT integration is standard
- Pitfalls: HIGH - Common security issues in JWT/file upload are well-documented

**Research date:** 2026-03-21
**Valid until:** 30 days - Stack is stable, patterns are well-established