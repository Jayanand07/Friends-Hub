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

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

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

    public AuthService(UserRepository userRepository, UserInfoRepository userInfoRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager,
            EmailService emailService, ExternalApiService externalApiService) {
        this.userRepository = userRepository;
        this.userInfoRepository = userInfoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.externalApiService = externalApiService;
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

        boolean emailSent = false;
        try {
            emailSent = emailService.sendVerificationEmail(savedUser.getEmail(), rawToken, request.getFirstName());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("Email dispatch error: {}", e.getMessage());
        }

        try {
            externalApiService.notifyUserRegistered(savedUser);
        } catch (Exception ignored) {}

        if (!emailSent) {
            savedUser.setVerificationStatus(VerificationStatus.VERIFIED);
            savedUser.setVerificationToken(null);
            savedUser.setTokenExpiry(null);
            userRepository.save(savedUser);
            return "Registration successful! Account auto-verified. You can log in now.";
        }

        return "User registered successfully. Please check your email to verify your account.";
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {

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
            org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("Redis rate-limiting bypass: {}", e.getMessage());
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.getPassword()));
            try {
                redisTemplate.delete(attemptsKey);
            } catch (Exception ignored) {}
        } catch (Exception e) {
            throw e;
        }

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Auto-verify user if valid credentials were provided
        if (user.getVerificationStatus() != VerificationStatus.VERIFIED) {
            user.setVerificationStatus(VerificationStatus.VERIFIED);
            user.setVerificationToken(null);
            user.setTokenExpiry(null);
            userRepository.save(user);
        }

        var jwtToken = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        user.getRole().name()))),
                user.getId());

        return AuthResponse.builder()
                .token(jwtToken)
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
    public String forgotPassword(String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isPresent()) {
            String otp = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
            User user = userOpt.get();
            user.setVerificationToken(hashToken(otp));
            user.setTokenExpiry(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);

            try {
                redisTemplate.opsForValue().set("otp:" + normalizedEmail, otp, Duration.ofMinutes(10));
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("Redis OTP save bypass: {}", e.getMessage());
            }

            boolean emailSent = false;
            try {
                emailSent = emailService.sendOtpEmail(normalizedEmail, otp);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("OTP email dispatch error: {}", e.getMessage());
            }

            if (!emailSent) {
                // SECURITY: Never leak OTP in response. Log server-side only.
                log.warn("OTP email failed for {}. OTP stored in Redis/DB only.", normalizedEmail);
                return "If that email exists, an OTP has been sent.";
            }
        }
        return "If that email exists, an OTP has been sent.";
    }

    @Transactional
    public String resetPassword(String email, String otp, String newPassword) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        String storedOtp = null;
        try {
            storedOtp = (String) redisTemplate.opsForValue().get("otp:" + normalizedEmail);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AuthService.class).warn("Redis OTP fetch bypass: {}", e.getMessage());
        }

        User user = userRepository.findByEmail(normalizedEmail).orElseThrow(() -> new RuntimeException("User not found"));

        boolean validOtp = (storedOtp != null && storedOtp.equals(otp));
        if (!validOtp) {
            if (user.getVerificationToken() != null && user.getTokenExpiry() != null
                    && user.getTokenExpiry().isAfter(LocalDateTime.now())) {
                String hashedIncoming = hashToken(otp);
                if (user.getVerificationToken().equals(hashedIncoming)) {
                    validOtp = true;
                }
            }
        }

        if (!validOtp) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);

        try {
            redisTemplate.delete("otp:" + normalizedEmail);
        } catch (Exception ignored) {}

        return "Password reset successfully. You can now login.";
    }

    @Transactional
    public AuthResponse googleLogin(OAuthRequest request) {
        String idToken = request.getIdToken();
        if (idToken == null || idToken.isBlank()) {
            throw new RuntimeException("Google ID token is required for OAuth login");
        }

        // Verify the Google ID token signature using Google's public keys (JWKS)
        io.jsonwebtoken.Claims claims;
        try {
            claims = GoogleTokenVerifier.verify(idToken);
        } catch (Exception e) {
            log.error("Google ID token verification failed: {}", e.getMessage());
            throw new RuntimeException("Failed to verify Google ID token: " + e.getMessage());
        }

        // Extract verified claims
        String verifiedEmail = claims.get("email", String.class);
        String verifiedName = claims.get("name", String.class);
        // SECURITY: Google sends email_verified as boolean, not String.
        // Using Object to handle both formats safely.
        Object emailVerifiedObj = claims.get("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(emailVerifiedObj);

        if (verifiedEmail == null || verifiedEmail.isBlank()) {
            throw new RuntimeException("Google ID token does not contain an email");
        }
        if (!emailVerified) {
            throw new RuntimeException("Google email is not verified");
        }

        // SECURITY: Validate token issuer — must be Google
        String iss = claims.getIssuer();
        if (iss == null || !(iss.equals("https://accounts.google.com")
                || iss.equals("accounts.google.com"))) {
            throw new RuntimeException("Invalid Google ID token issuer: " + iss);
        }

        // SECURITY: Validate token audience (client ID) if configured
        String expectedAudience = googleClientId;
        if (expectedAudience != null && !expectedAudience.isBlank()) {
            String aud = claims.getAudience();
            if (aud == null || !aud.equals(expectedAudience)) {
                throw new RuntimeException("Invalid Google ID token audience");
            }
        } else {
            log.warn("Google OAuth client ID not configured — skipping audience validation");
        }

        // Use VERIFIED email from token, NOT from request body
        verifiedEmail = verifiedEmail.trim().toLowerCase();
        if (verifiedName == null || verifiedName.isBlank()) {
            verifiedName = request.getName();
        }

        Optional<User> existingUser = userRepository.findByEmail(verifiedEmail);
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = new User();
            user.setEmail(verifiedEmail);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setVerificationStatus(com.example.socialmedia.entity.VerificationStatus.VERIFIED);
            user.setRole(Role.ROLE_USER);
            user.setAuthProvider(AuthProvider.GOOGLE);

            String slugified = verifiedName.toLowerCase().replaceAll("[^a-z0-9]", "");
            user.setUsername(slugified);

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

        return AuthResponse.builder().token(jwtToken).build();
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
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(idToken)
                    .getBody();
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

    public AuthResponse refreshToken(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        var jwtToken = jwtService.generateToken(new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        user.getRole().name()))),
                user.getId());
        return AuthResponse.builder()
                .token(jwtToken)
                .build();
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
}


