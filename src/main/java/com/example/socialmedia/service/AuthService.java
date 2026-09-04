package com.example.socialmedia.service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import com.example.socialmedia.dto.AuthResponse;
import com.example.socialmedia.dto.LoginRequest;
import com.example.socialmedia.dto.RegisterRequest;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.entity.UserInfo;
import com.example.socialmedia.entity.VerificationStatus;
import com.example.socialmedia.repository.RefreshTokenRepository;
import com.example.socialmedia.repository.UserInfoRepository;
import com.example.socialmedia.repository.UserRepository;
import com.example.socialmedia.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import io.jsonwebtoken.Jwts;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.Optional;
import com.example.socialmedia.entity.Role;
import com.example.socialmedia.entity.AuthProvider;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Duration;
import com.example.socialmedia.dto.OAuthRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    private static final ConcurrentHashMap<String, Integer> loginAttemptsFallback = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> loginFallbackExpiry = new ConcurrentHashMap<>();

    /** Fallback OTP attempt counters when Redis is unavailable: [count, windowEndMs] */
    private static final ConcurrentHashMap<String, long[]> otpAttemptsFallback = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final ExternalApiService externalApiService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @org.springframework.beans.factory.annotation.Value("${app.google.client-id:}")
    private String googleClientId;

    @org.springframework.beans.factory.annotation.Value("${supabase.url:}")
    private String supabaseUrl;

    @org.springframework.beans.factory.annotation.Value("${supabase.key:}")
    private String supabaseKey;

    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(UserRepository userRepository, UserInfoRepository userInfoRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager,
            EmailService emailService, ExternalApiService externalApiService,
            RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.userInfoRepository = userInfoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.externalApiService = externalApiService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public String register(RegisterRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already in use");
        }

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(rawToken);

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .verificationStatus(VerificationStatus.PENDING)
                .verificationToken(hashedToken)
                .tokenExpiry(LocalDateTime.now().plusHours(24))
                .build();

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Email already in use");
        }

        UserInfo userInfo = UserInfo.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .user(savedUser)
                .build();

        userInfoRepository.save(userInfo);

        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), rawToken, request.getFirstName());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("Email dispatch error: {}", e.getMessage());
        }

        try {
            externalApiService.notifyUserRegistered(savedUser);
        } catch (Exception ignored) {}

        return "User registered successfully. Please check your email to verify your account.";
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        String attemptsKey = "login:attempts:" + email;
        try {
            Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(attemptsKey, Duration.ofMinutes(1));
            }
            if (attempts != null && attempts > 10) {
                throw new RuntimeException("Too many login attempts. Please try again in 1 minute.");
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Too many login")) {
                throw e;
            }
            org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("Redis rate-limiting unavailable, using in-memory fallback: {}", e.getMessage());

            // In-memory fallback for rate limiting when Redis is down
            long now = System.currentTimeMillis();
            Long windowEnd = loginFallbackExpiry.get(attemptsKey);
            if (windowEnd == null || now > windowEnd) {
                loginFallbackExpiry.put(attemptsKey, now + 60_000L);
                loginAttemptsFallback.put(attemptsKey, 1);
            } else {
                int count = loginAttemptsFallback.merge(attemptsKey, 1, Integer::sum);
                if (count > 10) {
                    throw new RuntimeException("Too many login attempts. Please try again in 1 minute.");
                }
            }

            // Periodically evict expired entries to prevent unbounded map growth
            if (loginFallbackExpiry.size() > 10000) {
                long cutoff = now;
                loginFallbackExpiry.entrySet().removeIf(entry -> entry.getValue() < cutoff);
                loginAttemptsFallback.keySet().removeIf(k -> !loginFallbackExpiry.containsKey(k));
            }
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.getPassword()));
            try {
                redisTemplate.delete(attemptsKey);
            } catch (Exception ignored) {}
            // Clear in-memory fallback on successful login
            loginAttemptsFallback.remove(attemptsKey);
            loginFallbackExpiry.remove(attemptsKey);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please verify your email before logging in.");
        }

        var jwtToken = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        user.getRole().name()))),
                user.getId());

        String refreshToken = createAndSaveRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public String verifyAccount(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Verification token is required");
        }
        String hashedIncoming = hashToken(token);
        User user = userRepository.findByVerificationToken(hashedIncoming)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (user.getVerificationStatus() == VerificationStatus.VERIFIED) {
            return "Account already verified";
        }

        if (user.getTokenExpiry() != null && user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired. Please register again.");
        }

        // Constant-time comparison
        if (!MessageDigest.isEqual(
                hashedIncoming.getBytes(StandardCharsets.UTF_8),
                user.getVerificationToken().getBytes(StandardCharsets.UTF_8))) {
            throw new RuntimeException("Invalid verification token");
        }

        user.setVerificationStatus(VerificationStatus.VERIFIED);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);

        return "Account verified successfully";
    }

    @Transactional
    public String resendVerification(String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        if (normalizedEmail.isBlank()) {
            throw new RuntimeException("Email is required");
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getVerificationStatus() == VerificationStatus.VERIFIED) {
                return "Account is already verified. You can log in.";
            }

            String rawToken = UUID.randomUUID().toString();
            String hashedToken = hashToken(rawToken);

            user.setVerificationToken(hashedToken);
            user.setTokenExpiry(LocalDateTime.now().plusHours(24));
            userRepository.save(user);

            String firstName = "there";
            Optional<UserInfo> infoOpt = userInfoRepository.findByUser(user);
            if (infoOpt.isPresent() && infoOpt.get().getFirstName() != null && !infoOpt.get().getFirstName().isBlank()) {
                firstName = infoOpt.get().getFirstName();
            }

            try {
                emailService.sendVerificationEmail(user.getEmail(), rawToken, firstName);
                log.info("Dispatched new verification email to {}", maskEmail(user.getEmail()));
            } catch (Exception e) {
                log.warn("Resend verification email dispatch error: {}", e.getMessage());
            }
        }

        return "If that email exists and is unverified, a new verification link has been sent.";
    }

    @Transactional
    public String forgotPassword(String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isPresent()) {
            String otp = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
            User user = userOpt.get();
            user.setPasswordResetToken(hashToken(otp));
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);

            try {
                redisTemplate.opsForValue().set("otp:" + normalizedEmail, otp, Duration.ofMinutes(10));
            } catch (Exception e) {
                log.warn("Redis OTP save bypass: {}", e.getMessage());
            }

            log.info("Generated password reset OTP for user {}", maskEmail(normalizedEmail));
            try {
                emailService.sendOtpEmail(normalizedEmail, otp);
            } catch (Exception e) {
                // Make delivery failures LOUD — if users report "OTP never arrives",
                // this is almost always MAIL_USERNAME / MAIL_PASSWORD (Gmail App
                // Password) missing or wrong on the deployment.
                log.error("OTP EMAIL FAILED to send to {} — check MAIL_USERNAME / MAIL_PASSWORD "
                        + "env vars (must be a Gmail App Password, not the account password). "
                        + "Underlying error: {}", maskEmail(normalizedEmail), e.getMessage());
            }
        } else {
            log.info("Forgot password requested for non-existent email {}", maskEmail(normalizedEmail));
        }
        return "If that email exists, an OTP has been sent.";
    }

    @Transactional
    public String resetPassword(String email, String otp, String newPassword) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        String attemptsKey = "otp:attempts:" + normalizedEmail;

        // SECURITY (H-2): Per-account attempt limiting — a 6-digit OTP must not
        // be brute-forceable. After 5 wrong attempts the OTP is invalidated.
        int attempts = incrementOtpAttempts(attemptsKey);
        if (attempts > 5) {
            invalidateOtp(normalizedEmail, attemptsKey);
            throw new RuntimeException("Too many invalid attempts. Please request a new code.");
        }

        String storedOtp = null;
        try {
            storedOtp = (String) redisTemplate.opsForValue().get("otp:" + normalizedEmail);
        } catch (Exception e) {
            log.warn("Redis OTP fetch bypass: {}", e.getMessage());
        }

        // SECURITY: Neutral error — do not reveal whether the email is registered
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Invalid or expired OTP"));

        boolean validOtp = constantTimeEquals(storedOtp, otp);
        if (!validOtp) {
            if (user.getPasswordResetToken() != null && user.getResetTokenExpiry() != null
                    && user.getResetTokenExpiry().isAfter(LocalDateTime.now())) {
                String hashedIncoming = hashToken(otp);
                if (MessageDigest.isEqual(
                        hashedIncoming.getBytes(StandardCharsets.UTF_8),
                        user.getPasswordResetToken().getBytes(StandardCharsets.UTF_8))) {
                    validOtp = true;
                }
            }
        }

        if (!validOtp) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        // SECURITY: A password change must invalidate existing sessions —
        // revoke every refresh token so stolen tokens stop working immediately.
        try {
            refreshTokenRepository.deleteByUserEmail(normalizedEmail);
        } catch (Exception e) {
            log.warn("Failed to revoke refresh tokens after password reset: {}", e.getMessage());
        }

        invalidateOtp(normalizedEmail, attemptsKey);

        return "Password reset successfully. You can now login.";
    }

    /** SECURITY (H-2): count failed OTP attempts (Redis, with in-memory fallback). */
    private int incrementOtpAttempts(String attemptsKey) {
        try {
            Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(attemptsKey, Duration.ofMinutes(15));
            }
            if (attempts != null) {
                return attempts.intValue();
            }
        } catch (Exception e) {
            log.warn("Redis OTP attempt counter unavailable, using in-memory fallback: {}", e.getMessage());
        }
        long now = System.currentTimeMillis();
        long[] entry = otpAttemptsFallback.compute(attemptsKey, (k, v) -> {
            if (v == null || now > v[1]) {
                return new long[] { 1, now + 900_000L };
            }
            v[0] = v[0] + 1;
            return v;
        });
        return (int) entry[0];
    }

    /** SECURITY: wipe the OTP and its attempt counter (after success or lockout). */
    private void invalidateOtp(String email, String attemptsKey) {
        try {
            redisTemplate.delete("otp:" + email);
            redisTemplate.delete(attemptsKey);
        } catch (Exception ignored) {
        }
        otpAttemptsFallback.remove(attemptsKey);
        userRepository.findByEmailIgnoreCase(email).ifPresent(u -> {
            u.setPasswordResetToken(null);
            u.setResetTokenExpiry(null);
            userRepository.save(u);
        });
    }

    /** Constant-time string comparison to avoid timing side channels. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public AuthResponse googleLogin(OAuthRequest request) {
        String idToken = request.getIdToken();

        // SECURITY (C-1): Identity is NEVER taken from the request body — any
        // client could POST {"email": "victim@example.com"} and be issued tokens
        // for that account. The client must present a token that we verify
        // against the issuing provider (Google's JWKS or Supabase's auth API).
        if (idToken == null || idToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth token is required");
        }

        VerifiedOAuthUser verifiedUser = verifyOAuthToken(idToken);

        String verifiedEmail = verifiedUser.email;
        String verifiedName = verifiedUser.name;

        Optional<User> existingUser = userRepository.findByEmail(verifiedEmail);
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.getVerificationStatus() != VerificationStatus.VERIFIED) {
                user.setVerificationStatus(VerificationStatus.VERIFIED);
                user = userRepository.save(user);
            }
            if (userInfoRepository.findByUser(user).isEmpty()) {
                UserInfo userInfo = new UserInfo();
                userInfo.setFirstName(verifiedName);
                userInfo.setUser(user);
                userInfoRepository.save(userInfo);
            }
        } else {
            user = new User();
            user.setEmail(verifiedEmail);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setVerificationStatus(com.example.socialmedia.entity.VerificationStatus.VERIFIED);
            user.setRole(Role.ROLE_USER);
            user.setAuthProvider(AuthProvider.GOOGLE);

            String baseSlug = verifiedName.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (baseSlug.isBlank()) {
                baseSlug = "user";
            }
            String candidateUsername = baseSlug;
            int counter = 1;
            while (userRepository.existsByUsername(candidateUsername)) {
                candidateUsername = baseSlug + counter;
                counter++;
            }
            user.setUsername(candidateUsername);

            user = userRepository.save(user);

            UserInfo userInfo = new UserInfo();
            userInfo.setFirstName(verifiedName);
            userInfo.setUser(user);
            userInfoRepository.save(userInfo);
        }

        var jwtToken = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        user.getRole().name()))),
                user.getId());

        String refreshToken = createAndSaveRefreshToken(user.getEmail());

        return AuthResponse.builder().token(jwtToken).refreshToken(refreshToken).build();
    }

    private static class VerifiedOAuthUser {
        final String email;
        final String name;
        VerifiedOAuthUser(String email, String name) {
            this.email = email;
            this.name = name;
        }
    }

    /**
     * SECURITY (C-2): Verifies OAuth tokens cryptographically. No fallback to
     * unverified JWT payloads or client-supplied emails is permitted.
     *
     *  - Google ID tokens: RSA signature via Google's JWKS + iss + aud checks
     *  - Supabase access tokens: validated server-side by Supabase's
     *    /auth/v1/user endpoint (Supabase checks signature + expiry itself)
     */
    private VerifiedOAuthUser verifyOAuthToken(String idToken) {
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Malformed OAuth token");
        }

        // Peek at the issuer ONLY to decide which provider verifies the token.
        // Identity claims are never taken from this unverified payload.
        String payloadJson;
        try {
            payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Malformed OAuth token");
        }
        String iss = extractJsonField(payloadJson, "iss");

        if (iss != null && iss.contains("supabase")) {
            return verifySupabaseToken(idToken);
        }

        // Default: treat as a Google ID token (issuer + audience verified below)
        return verifyGoogleToken(idToken);
    }

    private VerifiedOAuthUser verifyGoogleToken(String idToken) {
        io.jsonwebtoken.Claims claims;
        try {
            claims = GoogleTokenVerifier.verify(idToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Google token verification failed: " + e.getMessage());
        }

        String iss = claims.getIssuer();
        if (!"https://accounts.google.com".equals(iss) && !"accounts.google.com".equals(iss)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unexpected token issuer");
        }

        // SECURITY: audience check — the token must be minted for THIS app,
        // preventing replay of tokens issued for other applications.
        Object audObj = claims.get("aud");
        String aud = audObj instanceof String ? (String) audObj
                : (audObj instanceof java.util.List<?> list && !list.isEmpty()) ? String.valueOf(list.get(0)) : null;
        if (googleClientId != null && !googleClientId.isBlank()) {
            if (!googleClientId.trim().equals(aud)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Token audience does not match this application");
            }
        } else {
            log.warn("app.google.client-id is not configured — Google token audience check skipped. "
                    + "Set it to prevent token replay from other apps.");
        }

        String verifiedEmail = claims.get("email", String.class);
        String verifiedName = claims.get("name", String.class);
        Object emailVerifiedObj = claims.get("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(emailVerifiedObj)
                || "true".equalsIgnoreCase(String.valueOf(emailVerifiedObj));

        if (verifiedEmail == null || verifiedEmail.isBlank() || !emailVerified) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google email is missing or not verified");
        }
        if (verifiedName == null || verifiedName.isBlank()) {
            verifiedName = verifiedEmail.split("@")[0];
        }
        return new VerifiedOAuthUser(verifiedEmail.trim().toLowerCase(), verifiedName);
    }

    /**
     * SECURITY (C-2): Supabase access tokens are validated by Supabase itself
     * (signature + expiry) via its auth user endpoint, using the service role
     * key. Identity comes from the verified response — never the client.
     */
    private VerifiedOAuthUser verifySupabaseToken(String idToken) {
        if (supabaseUrl == null || supabaseUrl.isBlank() || supabaseKey == null || supabaseKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Supabase is not configured — cannot verify token");
        }
        try {
            java.net.URL url = new java.net.URL(supabaseUrl + "/auth/v1/user");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", supabaseKey);
            conn.setRequestProperty("Authorization", "Bearer " + idToken);
            if (conn.getResponseCode() != 200) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Supabase rejected the token");
            }
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                String resp = sb.toString();
                String verifiedEmail = extractJsonField(resp, "email");
                if (verifiedEmail == null || verifiedEmail.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Supabase token has no email");
                }
                String verifiedName = null;
                String userMeta = extractJsonObject(resp, "user_metadata");
                if (userMeta != null) {
                    verifiedName = extractJsonField(userMeta, "full_name");
                    if (verifiedName == null) verifiedName = extractJsonField(userMeta, "name");
                }
                if (verifiedName == null || verifiedName.isBlank()) {
                    verifiedName = verifiedEmail.split("@")[0];
                }
                return new VerifiedOAuthUser(verifiedEmail.trim().toLowerCase(), verifiedName);
            } finally {
                conn.disconnect();
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Supabase token verification failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to verify Supabase token");
        }
    }

    private static String extractJsonField(String json, String field) {
        if (json == null) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractJsonObject(String json, String field) {
        if (json == null) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(field) + "\"\\s*:\\s*(\\{[^\\}]+\\})");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }



    /**
     * Verifies Google ID tokens using JWKS (JSON Web Key Set).
     * Fetches Google's public keys, caches them, and cryptographically
     * verifies the token signature — NOT just decoding the payload.
     */
    private static class GoogleTokenVerifier {
        private static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GoogleTokenVerifier.class);

        // Cache for Google's public keys: kid -> PublicKey
        private static volatile java.util.Map<String, PublicKey> cachedKeys = null;
        private static volatile long keysFetchedAt = 0;
        private static final long CACHE_TTL_MS = 86_400_000; // 24 hours

        static io.jsonwebtoken.Claims verify(String idToken) throws Exception {
            // Parse the JWT header to extract the kid (key ID)
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("Malformed Google ID token");
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String kid = extractKid(headerJson);
            if (kid == null) {
                // Some Google tokens don't have a kid — try all keys
                kid = "";
            }

            // Get Google's public keys (from cache or fetch)
            java.util.Map<String, PublicKey> keys = getPublicKeys();
            PublicKey publicKey = kid.isEmpty() ? keys.values().stream().findFirst().orElse(null) : keys.get(kid);
            if (publicKey == null) {
                // Key not found in cache — force refresh and try again
                cachedKeys = null;
                keys = getPublicKeys();
                publicKey = kid.isEmpty() ? keys.values().stream().findFirst().orElse(null) : keys.get(kid);
                if (publicKey == null) {
                    throw new RuntimeException("No matching Google public key found for kid: " + kid);
                }
            }

            // Verify the JWT signature and parse claims using jjwt
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        }

        private static String extractKid(String headerJson) {
            // Simple JSON parsing without a library
            String search = "\"kid\":\"";
            int idx = headerJson.indexOf(search);
            if (idx < 0) return null;
            int start = idx + search.length();
            int end = headerJson.indexOf('"', start);
            return end > start ? headerJson.substring(start, end) : null;
        }

        private static java.util.Map<String, PublicKey> getPublicKeys() throws Exception {
            if (cachedKeys != null && (System.currentTimeMillis() - keysFetchedAt) < CACHE_TTL_MS) {
                return cachedKeys;
            }

            synchronized (GoogleTokenVerifier.class) {
                if (cachedKeys != null && (System.currentTimeMillis() - keysFetchedAt) < CACHE_TTL_MS) {
                    return cachedKeys;
                }

                // Fetch JWKS from Google
                java.net.URL url = new java.net.URL(GOOGLE_JWKS_URL);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("GET");

                String response;
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    response = sb.toString();
                } finally {
                    conn.disconnect();
                }

                cachedKeys = parseJwks(response);
                keysFetchedAt = System.currentTimeMillis();
                log.info("Fetched {} Google public keys", cachedKeys.size());
                return cachedKeys;
            }
        }

        private static java.util.Map<String, PublicKey> parseJwks(String json) throws Exception {
            java.util.Map<String, PublicKey> keys = new java.util.HashMap<>();

            // Very basic JWKS parser — extract each key object between { }
            // Looking for: "kid":"...","n":"...","e":"..."
            int pos = 0;
            while (true) {
                int startIdx = json.indexOf('{', pos);
                if (startIdx < 0) break;
                int endIdx = json.indexOf('}', startIdx);
                if (endIdx < 0) break;
                String keyObj = json.substring(startIdx, endIdx + 1);
                pos = endIdx + 1;

                String kid = extractJwksField(keyObj, "kid");
                String n = extractJwksField(keyObj, "n");
                String e = extractJwksField(keyObj, "e");

                if (kid != null && n != null && e != null) {
                    keys.put(kid, buildPublicKey(n, e));
                }
            }

            return keys;
        }

        private static String extractJwksField(String json, String field) {
            String search = "\"" + field + "\":\"";
            int idx = json.indexOf(search);
            if (idx < 0) return null;
            int start = idx + search.length();
            int end = json.indexOf('"', start);
            return end > start ? json.substring(start, end) : null;
        }

        private static PublicKey buildPublicKey(String modulusB64, String exponentB64) throws Exception {
            byte[] modulusBytes = Base64.getUrlDecoder().decode(modulusB64);
            byte[] exponentBytes = Base64.getUrlDecoder().decode(exponentB64);
            java.math.BigInteger modulus = new java.math.BigInteger(1, modulusBytes);
            java.math.BigInteger exponent = new java.math.BigInteger(1, exponentBytes);
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        }
    }

    @Transactional
    public String createAndSaveRefreshToken(String userEmail) {
        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        String hash = hashToken(rawToken);
        com.example.socialmedia.entity.RefreshToken refreshToken = new com.example.socialmedia.entity.RefreshToken(
                hash, userEmail, java.time.LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * SECURITY (H-3): Revoke a single refresh token (logout). The hashed token
     * is deleted so it can no longer mint new access tokens.
     */
    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        try {
            refreshTokenRepository.deleteByTokenHash(hashToken(rawRefreshToken));
        } catch (Exception e) {
            log.warn("Failed to revoke refresh token: {}", e.getMessage());
        }
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
        }
        String hash = hashToken(rawRefreshToken);
        com.example.socialmedia.entity.RefreshToken tokenEntity = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (tokenEntity.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            refreshTokenRepository.delete(tokenEntity);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        String email = tokenEntity.getUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Rotate refresh token
        refreshTokenRepository.delete(tokenEntity);
        String newRefreshToken = createAndSaveRefreshToken(email);

        var newAccessToken = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        user.getRole().name()))),
                user.getId());

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        refreshTokenRepository.deleteByExpiryDateBefore(java.time.LocalDateTime.now());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    /** Mask email for logging: e.g. "user@example.com" -> "u*****@example.com" */
    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) return email;
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) return email;
        return email.charAt(0) + "*****" + email.substring(atIdx);
    }
}


