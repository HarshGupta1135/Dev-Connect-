package com.example.DevConnect.filter;

import com.example.DevConnect.service.UserDetailsServiceImpl;
import com.example.DevConnect.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        /*
         * A token this filter cannot make sense of leaves the request unauthenticated,
         * and Spring Security answers 401/403 — which clients read as "sign in again".
         *
         * Letting the exception escape instead produced a 500: this filter runs outside
         * the @RestControllerAdvice, so nothing translated it. That is the wrong answer
         * to a rejected credential, and actively harmful — a client cannot tell a broken
         * server from its own stale token, so it keeps the token and keeps failing.
         *
         * Reachable whenever the signing key is rotated, when a token is tampered with,
         * and when a token names an email that no longer exists because the account
         * changed it.
         */
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (JwtException | IllegalArgumentException ex) {
                logger.debug("Rejecting unreadable JWT: " + ex.getMessage());
                filterChain.doFilter(request, response);
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails;
            try {
                userDetails = this.userDetailsService.loadUserByUsername(username);
            } catch (UsernameNotFoundException ex) {
                logger.debug("JWT names a user that no longer exists: " + username);
                filterChain.doFilter(request, response);
                return;
            }

            if (jwtUtil.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
