package br.unisinos.uni4read.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CorsIntegrationTest {

    private static final String[] ORIGINS = {
            "http://localhost:5500", "http://127.0.0.1:5500" };

    @Autowired
    private MockMvc mvc;

    @Test
    void allowedOriginsSupportTheApplicationMethodsAndHeaders() throws Exception {
        for (String origin : ORIGINS) {
            for (String method : new String[] { "GET", "POST", "PATCH", "DELETE", "OPTIONS" }) {
                mvc.perform(options("/api/feed").header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                        .andExpect(status().isOk())
                        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString(method)))
                        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                                containsString("Authorization")))
                        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                                containsString("Content-Type")));
            }
        }
    }

    @Test
    void actualLoginRefreshAuthenticatedAccessAndLogoutWorkForBothOrigins() throws Exception {
        for (String origin : ORIGINS) {
            mvc.perform(get("/api/books").header(HttpHeaders.ORIGIN, origin))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
            MvcResult login = mvc.perform(post("/api/auth/login").header(HttpHeaders.ORIGIN, origin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"alice@example.com\",\"password\":\"password\"}"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")).andReturn();
            String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
            mvc.perform(get("/api/auth/me").header(HttpHeaders.ORIGIN, origin)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                    .andExpect(jsonPath("$.user.email").value("alice@example.com"));
            MvcResult renewed = mvc.perform(post("/api/auth/refresh").header(HttpHeaders.ORIGIN, origin)
                    .cookie(refreshCookie(login)))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")).andReturn();
            mvc.perform(post("/api/auth/logout").header(HttpHeaders.ORIGIN, origin).cookie(refreshCookie(renewed)))
                    .andExpect(status().isNoContent())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        }
    }

    @Test
    void unapprovedOriginsReceiveNoCorsPermission() throws Exception {
        for (String origin : new String[] { "https://unapproved.example", "http://localhost:9999", "null" }) {
            mvc.perform(options("/api/feed").header(HttpHeaders.ORIGIN, origin)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
            mvc.perform(get("/api/books").header(HttpHeaders.ORIGIN, origin))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    @Test
    void unapprovedMethodsAndHeadersAreRejected() throws Exception {
        mvc.perform(options("/api/feed").header(HttpHeaders.ORIGIN, ORIGINS[0])
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        mvc.perform(options("/api/feed").header(HttpHeaders.ORIGIN, ORIGINS[0])
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Unapproved-Header"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private MockCookie refreshCookie(MvcResult result) {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith("uni4read_refresh_v2="))
                .map(MockCookie::parse).findFirst().orElseThrow();
    }
}
