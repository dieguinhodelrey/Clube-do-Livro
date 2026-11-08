package br.unisinos.uni4read.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import br.unisinos.uni4read.entity.RefreshToken;
import br.unisinos.uni4read.repository.RefreshTokenRepository;
import br.unisinos.uni4read.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    private static final String PASSWORD = "test-password";
    private static final String REFRESH_COOKIE = "uni4read_refresh_v2";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository users;
    @Autowired
    private RefreshTokenRepository tokens;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String email;
    private MvcResult registration;

    @BeforeEach
    void registerIndependentReader() throws Exception {
        email = "auth-" + UUID.randomUUID() + "@example.com";
        registration = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"  Leitora de teste  ","email":"%s","password":"%s"}
                        """.formatted(email.toUpperCase(Locale.ROOT), PASSWORD)))
                .andExpect(status().isOk()).andReturn();
    }

    @AfterEach
    void removeTestReaderAndItsSessions() {
        users.findByEmailIgnoreCase(email).ifPresent(user -> {
            tokens.deleteAll(tokens.findAll().stream()
                    .filter(token -> token.getUser().getId().equals(user.getId())).toList());
            users.delete(user);
        });
    }

    @Test
    void registrationNormalizesNamesAndEmailAndProtectsSessionData() throws Exception {
        assertThat(JsonPath.<String>read(registration.getResponse().getContentAsString(), "$.user.name"))
                .isEqualTo("Leitora de teste");
        assertThat(JsonPath.<String>read(registration.getResponse().getContentAsString(), "$.user.email"))
                .isEqualTo(email);
        assertThat(JsonPath.<String>read(registration.getResponse().getContentAsString(), "$.user.role"))
                .isEqualTo("USER");
        assertThat(registration.getResponse().getContentAsString()).doesNotContain("passwordHash");
        var user = users.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
        assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD);
        MockCookie cookie = cookie(registration, REFRESH_COOKIE);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(7 * 24 * 60 * 60);
        assertThat(cookie.getSecure()).isFalse();
        assertThat(registration.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anyMatch(header -> header.contains("SameSite=Lax"));
        assertThat(cookie(registration, "uni4read_refresh").getMaxAge()).isZero();
        assertThat(storedToken().getTokenHash()).isNotEqualTo(cookie.getValue());
    }

    @Test
    void duplicateEmailAndInvalidRegistrationReturnExplicitErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Outra leitora","email":"%s","password":"%s"}
                        """.formatted(email.toUpperCase(Locale.ROOT), PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \",\"email\":\"invalid-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void loginTokenCanAccessMeAndProtectedRoutesRequireIt() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email.toUpperCase(Locale.ROOT), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty()).andReturn();
        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.accessToken").isEmpty());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/feed")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongCredentialsDoNotCreateASession() throws Exception {
        for (String address : new String[] { email, "missing-" + UUID.randomUUID() + "@example.com" }) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(address, "wrong-password")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
        }
        assertThat(tokens.findAll().stream()
                .filter(token -> token.getUser().getId().equals(storedToken().getUser().getId())).count())
                .isEqualTo(1);
    }

    @Test
    void refreshRotatesTheCookieAndRejectsReuseWithoutInvalidatingTheNewSession() throws Exception {
        MockCookie original = cookie(registration, REFRESH_COOKIE);
        MvcResult renewed = mvc.perform(post("/api/auth/refresh").cookie(original))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty()).andReturn();
        MockCookie rotated = cookie(renewed, REFRESH_COOKIE);
        assertThat(rotated.getValue()).isNotEqualTo(original.getValue());
        MvcResult rejected = mvc.perform(post("/api/auth/refresh").cookie(original))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401)).andReturn();
        assertThat(cookie(rejected, REFRESH_COOKIE).getMaxAge()).isZero();
        mvc.perform(post("/api/auth/refresh").cookie(rotated))
                .andExpect(status().isOk());
    }

    @Test
    void absentInvalidAndExpiredRefreshCookiesAreRejectedAndCleared() throws Exception {
        MvcResult absent = mvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized()).andReturn();
        assertThat(cookie(absent, REFRESH_COOKIE).getMaxAge()).isZero();
        mvc.perform(post("/api/auth/refresh").cookie(new MockCookie(REFRESH_COOKIE, "invalid")))
                .andExpect(status().isUnauthorized());
        RefreshToken current = storedToken();
        current.setExpiresAt(Instant.now().minusSeconds(1));
        tokens.saveAndFlush(current);
        MvcResult expired = mvc.perform(post("/api/auth/refresh").cookie(cookie(registration, REFRESH_COOKIE)))
                .andExpect(status().isUnauthorized()).andReturn();
        assertThat(cookie(expired, REFRESH_COOKIE).getMaxAge()).isZero();
    }

    @Test
    void logoutRevokesRefreshAndClearsBothCookies() throws Exception {
        MockCookie original = cookie(registration, REFRESH_COOKIE);
        MvcResult logout = mvc.perform(post("/api/auth/logout").cookie(original))
                .andExpect(status().isNoContent()).andReturn();
        assertThat(cookie(logout, REFRESH_COOKIE).getMaxAge()).isZero();
        assertThat(cookie(logout, "uni4read_refresh").getMaxAge()).isZero();
        mvc.perform(post("/api/auth/refresh").cookie(original))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
    }

    @Test
    void disabledReadersCannotLoginOrRenew() throws Exception {
        var user = users.findByEmailIgnoreCase(email).orElseThrow();
        user.setEnabled(false);
        users.saveAndFlush(user);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, PASSWORD))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(cookie(registration, REFRESH_COOKIE)))
                .andExpect(status().isUnauthorized());
    }

    private String loginBody(String address, String password) {
        return "{\"email\":\"" + address + "\",\"password\":\"" + password + "\"}";
    }

    private MockCookie cookie(MvcResult result, String name) {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(header -> header.startsWith(name + "="))
                .map(MockCookie::parse).findFirst().orElseThrow();
    }

    private RefreshToken storedToken() {
        UUID userId = users.findByEmailIgnoreCase(email).orElseThrow().getId();
        return tokens.findAll().stream().filter(token -> token.getUser().getId().equals(userId))
                .findFirst().orElseThrow();
    }
}
