package com.premium.member.c.bootstrap.support;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * 真实 PostgreSQL + Redis 上的 C 端 HTTP 集成测试基类。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "test"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class CIntegrationIT {

    protected static final String PASSWORD = "Test123456";
    private static final String CARD_SECRET = "test-benefit-card-secret";
    private static final Path DB_DIR = locateAdminDb();

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
                    .withDatabaseName("premium_member")
                    .withUsername("premium_member")
                    .withPassword("premium_member_dev_only")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(DB_DIR.resolve("schema.sql").toString()),
                            "/docker-entrypoint-initdb.d/01-admin-schema.sql")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(DB_DIR.resolve("data.sql").toString()),
                            "/docker-entrypoint-initdb.d/02-admin-data.sql")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("db/schema.sql"),
                            "/docker-entrypoint-initdb.d/03-c-schema.sql");

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

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected HttpResult get(String path, String token) {
        return exchange(HttpMethod.GET, path, null, token);
    }

    protected HttpResult post(String path, Object body, String token) {
        return exchange(HttpMethod.POST, path, body, token);
    }

    protected HttpResult exchange(HttpMethod method, String path, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        if (token != null) {
            headers.setBearerAuth(token);
        }
        try {
            String payload = body == null ? null : objectMapper.writeValueAsString(body);
            ResponseEntity<String> response = rest.exchange(path, method,
                    new HttpEntity<>(payload, headers), String.class);
            String text = response.getBody() == null ? "" : response.getBody();
            JsonNode json = text.stripLeading().startsWith("{")
                    ? objectMapper.readTree(text) : null;
            return new HttpResult(response.getStatusCode().value(), json, text, response.getHeaders());
        } catch (Exception ex) {
            throw new IllegalStateException("HTTP request failed: " + path, ex);
        }
    }

    protected Account createAccount(String phone, String memberNo) {
        Long memberId = jdbc.queryForObject("""
                INSERT INTO pm_member (member_no, username, owner_id, nickname, phone, level,
                                       points, balance, status, joined_at, remark)
                VALUES (?, ?, 1, 'E2E 会员', ?, 'BASIC', 0, 0, 1, now(), 'integration test')
                RETURNING id
                """, Long.class, memberNo, "it_" + memberNo, phone);
        Long userId = jdbc.queryForObject("""
                INSERT INTO c_user (phone, password_hash, nickname, avatar, status)
                VALUES (?, ?, 'E2E', '', 1)
                RETURNING id
                """, Long.class, phone, passwordEncoder.encode(PASSWORD));
        return new Account(memberId, userId, phone, login(phone));
    }

    protected String login(String phone) {
        HttpResult result = post("/c/auth/login",
                Map.of("phone", phone, "password", PASSWORD), null);
        assertThat(result.status()).as("login status for %s", phone).isEqualTo(200);
        return result.json().path("data").path("accessToken").asText();
    }

    protected long createActivePeriod(long memberId, long planVersionId) {
        Long orderId = jdbc.queryForObject("""
                INSERT INTO orders (order_no, member_id, user_id, plan_version_id, amount_cents,
                                    status, channel, source, purpose, remark, created_at, updated_at, paid_at)
                VALUES (?, ?, 1, ?, 5900, 'PAID', 'MOCK', 'H5', 'MEMBER_PLAN', '',
                        now(), now(), now())
                RETURNING id
                """, Long.class, "PMIT" + System.nanoTime(), memberId, planVersionId);
        return jdbc.queryForObject("""
                INSERT INTO pm_member_period (member_id, period_no, start_at, end_at, status, order_id)
                VALUES (?, ?, now() - INTERVAL '1 day', now() + INTERVAL '30 days', 'ACTIVE', ?)
                RETURNING id
                """, Long.class, memberId,
                "IT" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 14),
                orderId);
    }

    protected long createPlan(String code, int imageQuota, int videoQuota) {
        return jdbc.queryForObject("""
                INSERT INTO plan_version (code, name, price_cents, duration_days, ai_image_quota,
                                          ai_video_quota, benefit_quota, status, version_no)
                VALUES (?, ?, 100, 30, ?, ?, 0, 1, 1)
                RETURNING id
                """, Long.class, code, code, imageQuota, videoQuota);
    }

    protected long createInputAsset(long memberId) {
        String storageKey = "it/input-" + System.nanoTime() + ".png";
        try {
            Path file = Path.of("target/ai-test").resolve(storageKey);
            Files.createDirectories(file.getParent());
            Files.write(file, new byte[] {(byte) 0x89, 'P', 'N', 'G'});
        } catch (Exception ex) {
            throw new IllegalStateException("cannot create test input asset", ex);
        }
        return jdbc.queryForObject("""
                INSERT INTO asset (owner_member_id, purpose, mime, size, storage_key, status,
                                   expire_at, expose_count)
                VALUES (?, 'INPUT', 'image/png', 4, ?, 'ACTIVE', now() + INTERVAL '1 day', 0)
                RETURNING id
                """, Long.class, memberId, storageKey);
    }

    protected long templateId(String code) {
        return jdbc.queryForObject("SELECT id FROM ai_template WHERE code = ?", Long.class, code);
    }

    protected void insertCard(String sku, String code) {
        jdbc.update("""
                INSERT INTO benefit_card (batch_no, product_sku, code_hash, code_cipher, code_mask)
                VALUES (?, ?, ?, ?, ?)
                """, "IT", sku, sha256(code), encrypt(code),
                code.substring(0, 3) + "****" + code.substring(code.length() - 3));
    }

    private String encrypt(String plain) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            SecretKeySpec key = new SecretKeySpec(sha256Bytes(CARD_SECRET), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("card encrypt failed", ex);
        }
    }

    private static String sha256(String value) {
        return HexFormat.of().formatHex(sha256Bytes(value));
    }

    private static byte[] sha256Bytes(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static Path locateAdminDb() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("admin-server/admin-bootstrap/src/main/resources/db");
            if (Files.isRegularFile(candidate.resolve("schema.sql"))) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("admin-server db directory not found from "
                + System.getProperty("user.dir"));
    }

    public record Account(long memberId, long userId, String phone, String token) {
    }

    public record HttpResult(int status, JsonNode json, String text, HttpHeaders headers) {

        public String bizCode() {
            return json == null ? null : json.path("bizCode").asText(null);
        }

        public JsonNode data() {
            return json == null ? null : json.path("data");
        }
    }
}
