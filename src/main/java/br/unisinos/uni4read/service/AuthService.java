package br.unisinos.uni4read.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.unisinos.uni4read.dto.AuthResponseDTO;
import br.unisinos.uni4read.dto.LoginRequestDTO;
import br.unisinos.uni4read.dto.RegisterRequestDTO;
import br.unisinos.uni4read.dto.UserResponseDTO;
import br.unisinos.uni4read.entity.RefreshToken;
import br.unisinos.uni4read.entity.User;
import br.unisinos.uni4read.repository.RefreshTokenRepository;
import br.unisinos.uni4read.repository.UserRepository;
import br.unisinos.uni4read.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTokenLifetimeDays;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${auth.jwt.refresh-token-expiration-days}") long refreshTokenLifetimeDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenLifetimeDays = refreshTokenLifetimeDays;
    }

    @Transactional
    public AuthSession register(RegisterRequestDTO request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        user.setEnabled(true);
        return createSession(userRepository.save(user));
    }

    @Transactional
    public AuthSession login(LoginRequestDTO request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(User::isEnabled)
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("E-mail ou senha inválidos");
        }
        return createSession(user);
    }

    @Transactional
    public AuthSession refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida");
        }
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> token.getRevokedAt() == null)
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .filter(token -> token.getUser().isEnabled())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada"));
        current.setRevokedAt(Instant.now());
        return createSession(current.getUser());
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    public UserResponseDTO toResponse(User user) {
        return new UserResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    private AuthSession createSession(User user) {
        String rawRefreshToken = UUID.randomUUID() + "." + UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenLifetimeDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);
        return new AuthSession(jwtService.createAccessToken(user), rawRefreshToken, toResponse(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não disponível", exception);
        }
    }

    public record AuthSession(String accessToken, String refreshToken, UserResponseDTO user) {
        public AuthResponseDTO response() {
            return new AuthResponseDTO(accessToken, user);
        }
    }
}
