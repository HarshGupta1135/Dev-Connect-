package com.example.DevConnect.filter;

import com.example.DevConnect.service.UserDetailsServiceImpl;
import com.example.DevConnect.service.UserPrincipal;
import com.example.DevConnect.support.TestData;
import com.example.DevConnect.util.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private UserDetailsServiceImpl userDetailsService;
    @Mock
    private FilterChain chain;

    private JwtAuthFilter filter;
    private JwtUtil jwtUtil;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", TestData.JWT_SECRET);
        filter = new JwtAuthFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(filter, "userDetailsService", userDetailsService);
        request = new MockHttpServletRequest("GET", "/api/account/me");
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenAuthenticatesWithTheUsersRoles() throws Exception {
        UserPrincipal principal = new UserPrincipal(TestData.user(1L, "dev@gmail.com", "DEVELOPER"));
        when(userDetailsService.loadUserByUsername("dev@gmail.com")).thenReturn(principal);
        request.addHeader("Authorization", "Bearer " + jwtUtil.generateToken(principal));

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("dev@gmail.com", auth.getName());
        assertEquals(List.of("ROLE_DEVELOPER"), auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        verify(chain).doFilter(request, response);
    }

    @Test
    void noHeaderLeavesTheRequestAnonymous() throws Exception {
        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void nonBearerHeaderIsIgnored() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void garbageTokenContinuesUnauthenticatedInsteadOfFailing() throws Exception {
        request.addHeader("Authorization", "Bearer not-a-jwt");

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(200, response.getStatus());
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenForDeletedOrRenamedUserContinuesUnauthenticated() throws Exception {
        UserPrincipal principal = new UserPrincipal(TestData.user(1L, "gone@gmail.com", "DEVELOPER"));
        when(userDetailsService.loadUserByUsername("gone@gmail.com")).thenThrow(new UsernameNotFoundException("gone"));
        request.addHeader("Authorization", "Bearer " + jwtUtil.generateToken(principal));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
