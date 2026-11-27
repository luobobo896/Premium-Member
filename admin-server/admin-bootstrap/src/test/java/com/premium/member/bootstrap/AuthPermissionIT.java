package com.premium.member.bootstrap;

import com.premium.member.bootstrap.support.AdminIntegrationIT;
import com.premium.member.bootstrap.support.AdminIntegrationIT.HttpResult;
import com.premium.member.bootstrap.support.AdminIntegrationIT.Login;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPermissionIT extends AdminIntegrationIT {

    @Test
    void loginRefreshAndUnauthorized() {
        Login admin = login("admin", "Admin@123456");
        HttpResult memberList = get("/members?page=1&pageSize=1", admin.token());
        assertThat(memberList.status()).isEqualTo(200);
        assertThat(memberList.json().path("data").path("items").isArray()).isTrue();

        HttpResult anonymous = get("/members?page=1&pageSize=1", null);
        assertThat(anonymous.status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(anonymous.bizCode()).isEqualTo("UNAUTHORIZED");

        HttpResult refresh = post("/auth/refresh", null, null, admin.cookie());
        assertThat(refresh.status()).isEqualTo(200);
        assertThat(refresh.text()).startsWith("eyJ");
        HttpResult codes = get("/auth/codes", refresh.text());
        assertThat(codes.status()).isEqualTo(200);
        assertThat(codes.json().path("data").isArray()).isTrue();

        HttpResult badPassword = post("/auth/login",
                java.util.Map.of("username", "admin", "password", "wrong"), null, null);
        assertThat(badPassword.status()).isEqualTo(403);
        assertThat(badPassword.bizCode()).isEqualTo("INVALID_CREDENTIALS");

        HttpResult noCookieRefresh = post("/auth/refresh", null, null, null);
        assertThat(noCookieRefresh.status()).isEqualTo(403);
        assertThat(noCookieRefresh.bizCode()).isEqualTo("REFRESH_TOKEN_INVALID");

        HttpResult logout = post("/auth/logout", null, admin.token(), admin.cookie());
        assertThat(logout.status()).isEqualTo(200);
        assertThat(logout.headers().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
        HttpResult afterLogout = post("/auth/refresh", null, null, admin.cookie());
        assertThat(afterLogout.status()).isEqualTo(403);
        assertThat(afterLogout.bizCode()).isEqualTo("REFRESH_TOKEN_INVALID");
    }

    @Test
    void viewerIsForbiddenAndSelfScopeHidesOthers() {
        Login viewer = login("viewer", "Viewer@123456");
        assertThat(get("/members?page=1&pageSize=1", viewer.token()).status()).isEqualTo(200);

        HttpResult create = post("/members",
                java.util.Map.of("username", "viewer-blocked", "level", "BASIC", "status", 1),
                viewer.token(), null);
        assertThat(create.status()).isEqualTo(403);
        assertThat(create.bizCode()).isEqualTo("FORBIDDEN");

        HttpResult orders = get("/system/orders?page=1&pageSize=1", viewer.token());
        assertThat(orders.status()).isEqualTo(403);
        assertThat(orders.bizCode()).isEqualTo("FORBIDDEN");

        Login self = login("self_manager", "Admin@123456");
        HttpResult other = get("/members/1", self.token());
        assertThat(other.status()).isEqualTo(404);
        assertThat(other.bizCode()).isEqualTo("MEMBER_NOT_FOUND");

        HttpResult own = get("/members/5", self.token());
        assertThat(own.status()).isEqualTo(200);
        assertThat(own.json().path("data").path("id").asLong()).isEqualTo(5);

        HttpResult scoped = get("/members?page=1&pageSize=50", self.token());
        assertThat(scoped.status()).isEqualTo(200);
        assertThat(scoped.json().path("data").path("total").asLong()).isEqualTo(1);
        assertThat(scoped.json().path("data").path("items").get(0).path("id").asLong())
                .isEqualTo(5);
    }
}
