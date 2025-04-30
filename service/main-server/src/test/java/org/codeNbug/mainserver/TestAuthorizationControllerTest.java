/*
package org.codeNbug.mainserver;

import org.codeNbug.mainserver.domain.mock.TestAuthorizationController;
import org.codeNbug.mainserver.domain.user.constant.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.codeNbug.mainserver.global.security.filter.JwtAuthenticationFilter;
import org.springframework.test.context.TestPropertySource;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestAuthorizationController.class)
@TestPropertySource(locations = "classpath:application-test.yml")
class TestAuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AuthenticationProvider authenticationProvider;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("공개 엔드포인트는 인증 없이 접근 가능해야 함")
    void publicEndpoint_ShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/test/auth/public"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Public 경로 접근 성공"));
    }

    private void authenticateUser(UserRole role) {
        var authorities = Collections.singletonList(
            new SimpleGrantedAuthority(role.getRole()));
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "testUser", "password", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void authenticateWithMultipleRoles(UserRole... roles) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority(role.getRole()))
                .collect(Collectors.toList());
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "testUser", "password", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("USER 권한으로 사용자 엔드포인트 접근 가능")
    void userEndpoint_WithUserRole_ShouldBeAccessible() throws Exception {
        authenticateUser(UserRole.USER);

        mockMvc.perform(get("/api/test/auth/user"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("User access successful"));
    }

    @Test
    @DisplayName("ADMIN 권한으로 사용자 엔드포인트 접근 불가")
    void userEndpoint_WithAdminRole_ShouldBeForbidden() throws Exception {
        authenticateUser(UserRole.ADMIN);

        mockMvc.perform(get("/api/test/auth/user"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN 권한으로 관리자 엔드포인트 접근 가능")
    void adminEndpoint_WithAdminRole_ShouldBeAccessible() throws Exception {
        authenticateUser(UserRole.ADMIN);

        mockMvc.perform(get("/api/test/auth/admin"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Admin access successful"));
    }

    @Test
    @DisplayName("USER 권한으로 관리자 엔드포인트 접근 불가")
    void adminEndpoint_WithUserRole_ShouldBeForbidden() throws Exception {
        authenticateUser(UserRole.USER);

        mockMvc.perform(get("/api/test/auth/admin"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER 권한으로 사용자/관리자 공용 엔드포인트 접근 가능")
    void userOrAdminEndpoint_WithUserRole_ShouldBeAccessible() throws Exception {
        authenticateUser(UserRole.USER);

        mockMvc.perform(get("/api/test/auth/user-or-admin"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("User or Admin access successful"));
    }

    @Test
    @DisplayName("ADMIN 권한으로 사용자/관리자 공용 엔드포인트 접근 가능")
    void userOrAdminEndpoint_WithAdminRole_ShouldBeAccessible() throws Exception {
        authenticateUser(UserRole.ADMIN);

        mockMvc.perform(get("/api/test/auth/user-or-admin"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("User or Admin access successful"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 보호된 엔드포인트 접근 불가")
    void protectedEndpoints_WithoutAuth_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/test/auth/user"))
                .andDo(print())
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/test/auth/admin"))
                .andDo(print())
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/test/auth/user-or-admin"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("복수 권한 사용자는 해당하는 모든 엔드포인트 접근 가능")
    void multipleRoles_ShouldHaveAppropriateAccess() throws Exception {
        authenticateWithMultipleRoles(UserRole.USER, UserRole.ADMIN);

        mockMvc.perform(get("/api/test/auth/user"))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/test/auth/admin"))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/test/auth/user-or-admin"))
                .andDo(print())
                .andExpect(status().isOk());
    }
} */
