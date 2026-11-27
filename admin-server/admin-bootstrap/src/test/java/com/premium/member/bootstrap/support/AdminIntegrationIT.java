package com.premium.member.bootstrap.support;

import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 PostgreSQL + Redis 上的 B 端 HTTP 集成测试基类。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AdminIntegrationIT {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
                    .withDatabaseName("premium_member")
                    .withUsername("premium_member")
                    .withPassword("premium_member_dev_only")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("db/schema.sql"),
                            "/docker-entrypoint-initdb.d/01-schema.sql")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("db/data.sql"),
                            "/docker-entrypoint-initdb.d/02-data.sql");

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:8.2-alpine"))
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort())
                    .withStartupTimeout(Duration.ofMinutes(2));

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
    }

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected ObjectMapper objectMapper;

    protected HttpResult get(String path, String token) {
        return exchange(HttpMethod.GET, path, null, token, null);
    }

    protected HttpResult post(String path, Object body, String token, String cookie) {
        return exchange(HttpMethod.POST, path, body, token, cookie);
    }

    protected HttpResult exchange(HttpMethod method, String path, Object body,
            String token, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        if (token != null) {
            headers.setBearerAuth(token);
        }
        if (cookie != null) {
            headers.set(HttpHeaders.COOKIE, cookie);
        }
        try {
            String payload = body == null ? null : objectMapper.writeValueAsString(body);
            ResponseEntity<String> response = rest.exchange(path, method,
                    new HttpEntity<>(payload, headers), String.class);
            JsonNode json = null;
            String text = response.getBody() == null ? "" : response.getBody();
            if (text.stripLeading().startsWith("{")) {
                json = objectMapper.readTree(text);
            }
            return new HttpResult(response.getStatusCode().value(), json, text, response.getHeaders());
        } catch (Exception ex) {
            throw new IllegalStateException("HTTP request failed: " + path, ex);
        }
    }

    protected Login login(String username, String password) {
        HttpResult result = post("/auth/login",
                java.util.Map.of("username", username, "password", password), null, null);
        assertThat(result.status()).as("login status for %s: %s", username, result.text())
                .isEqualTo(200);
        assertThat(result.json().path("code").asInt()).isZero();
        String token = result.json().path("data").path("accessToken").asText();
        String setCookie = result.headers().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(token).isNotBlank();
        assertThat(setCookie).contains("jwt=");
        String cookie = setCookie == null ? null : setCookie.split(";", 2)[0];
        return new Login(token, cookie);
    }

    public record Login(String token, String cookie) {
    }

    public record HttpResult(int status, JsonNode json, String text, HttpHeaders headers) {

        public String bizCode() {
            return json == null ? null : json.path("bizCode").asText(null);
        }

    }
}
