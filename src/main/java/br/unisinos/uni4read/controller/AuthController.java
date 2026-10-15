package br.unisinos.uni4read.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.unisinos.uni4read.dto.AuthResponseDTO;
import br.unisinos.uni4read.dto.LoginRequestDTO;
import br.unisinos.uni4read.dto.RegisterRequestDTO;
import br.unisinos.uni4read.entity.User;
import br.unisinos.uni4read.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private static final String REFRESH_COOKIE = "uni4read_refresh_v2";
    private static final String LEGACY_REFRESH_COOKIE = "uni4read_refresh";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return sessionResponse(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return sessionResponse(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        try {
            return sessionResponse(authService.refresh(refreshToken));
        } catch (ResponseStatusException exception) {
            throw new InvalidRefreshTokenException(exception);
        }
    }

    @GetMapping("/me")
    public AuthResponseDTO me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return new AuthResponseDTO(null, authService.toResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .header(HttpHeaders.SET_COOKIE, clearLegacyRefreshCookie().toString())
                .build();
    }

    private ResponseEntity<AuthResponseDTO> sessionResponse(AuthService.AuthSession session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                .header(HttpHeaders.SET_COOKIE, clearLegacyRefreshCookie().toString())
                .body(session.response());
    }

    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private ResponseCookie clearLegacyRefreshCookie() {
        return ResponseCookie.from(LEGACY_REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<Void> invalidRefreshToken(InvalidRefreshTokenException exception) {
        return ResponseEntity.status(exception.getCauseException().getStatusCode())
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .header(HttpHeaders.SET_COOKIE, clearLegacyRefreshCookie().toString())
                .build();
    }

    private static class InvalidRefreshTokenException extends RuntimeException {
        private final ResponseStatusException causeException;

        private InvalidRefreshTokenException(ResponseStatusException causeException) {
            super(causeException);
            this.causeException = causeException;
        }

        private ResponseStatusException getCauseException() {
            return causeException;
        }
    }
}
