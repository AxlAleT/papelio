package com.escom.papelio.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security tests for Papelio application.
 * This class simulates OWASP ZAP style security testing by checking for common vulnerabilities:
 * 1. Cross-Site Scripting (XSS) attacks
 * 2. SQL Injection attacks
 * 3. CSRF protection
 * 4. Authentication bypass
 * 5. Sensitive data exposure
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTestIT {

    private static final Logger logger = LoggerFactory.getLogger(SecurityTestIT.class);

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test for Cross-Site Scripting (XSS) vulnerabilities
     * This test attempts to inject script tags in various input fields
     */
    @Test
    public void testXssProtection() throws Exception {
        String xssPayload = "<script>alert('XSS')</script>";
        String email = "security_test_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        // Test registration form for XSS protection
        MvcResult result = mockMvc.perform(post("/register")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", xssPayload)  // Inject XSS payload in name field
                .param("password", "SecurePassword123!")
                .param("email", email)
                .param("confirmPassword", "SecurePassword123!"))
                .andExpect(status().is3xxRedirection())  // Expect redirect to login on success
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("<script>"), "Response should not contain unescaped script tag");

        // Test search API for XSS protection
        result = mockMvc.perform(post("/api/search")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"" + xssPayload + "\",\"page\":0,\"size\":10}"))
                .andReturn();

        responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("<script>"), "Response should not contain unescaped script tag");

        logger.info("XSS protection test completed");
    }

    /**
     * Test for SQL Injection vulnerabilities
     * This test attempts to inject SQL commands in various input fields
     */
    @Test
    public void testSqlInjectionProtection() throws Exception {
        String[] sqlInjectionPayloads = {
            "' OR '1'='1",
            "'; DROP TABLE users; --",
            "' UNION SELECT * FROM users --"
        };

        for (String payload : sqlInjectionPayloads) {
            // Test login form for SQL injection protection
            MvcResult result = mockMvc.perform(formLogin("/login")
                    .user("username", payload)
                    .password(payload))
                    .andExpect(status().is3xxRedirection())  // Expect redirect (login should fail)
                    .andReturn();

            String location = result.getResponse().getHeader("Location");
            assertTrue(location != null && location.contains("error"),
                    "SQL injection attempt should fail login");

            // Test search API for SQL injection protection
            mockMvc.perform(post("/api/search")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"query\":\"" + payload + "\",\"page\":0,\"size\":10}"))
                    .andExpect(status().isOk());  // Should handle the input safely
        }

        logger.info("SQL injection protection test completed");
    }

    /**
     * Test for CSRF protection
     * This test attempts to perform state-changing operations without CSRF tokens
     */
    @Test
    public void testCsrfProtection() throws Exception {
        // Attempt registration without CSRF token
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "CSRF Test User")
                .param("password", "Password123!")
                .param("email", "csrf_test@example.com")
                .param("confirmPassword", "Password123!"))
                .andExpect(status().isForbidden());  // Should be forbidden without CSRF token

        // Attempt search without CSRF token
        mockMvc.perform(post("/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"test\",\"page\":0,\"size\":10}"))
                .andExpect(status().isForbidden());  // Should be forbidden without CSRF token

        logger.info("CSRF protection test completed");
    }

    /**
     * Test for secure authentication
     * This test attempts to bypass authentication
     */
    @Test
    public void testAuthenticationSecurity() throws Exception {
        // Attempt to access protected pages without authentication
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())  // Should redirect to login
                .andExpect(result -> {
                    String redirectUrl = result.getResponse().getRedirectedUrl();
                    assertTrue(redirectUrl != null && redirectUrl.contains("/login"),
                            "Unauthenticated access should redirect to login");
                });

        mockMvc.perform(get("/user/profile"))
                .andExpect(status().is3xxRedirection())  // Should redirect to login
                .andExpect(result -> {
                    String redirectUrl = result.getResponse().getRedirectedUrl();
                    assertTrue(redirectUrl != null && redirectUrl.contains("/login"),
                            "Unauthenticated access should redirect to login");
                });

        // Attempt to access admin endpoints without admin role
        // First register and login as a regular user
        String email = "auth_test_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        mockMvc.perform(post("/register")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Auth Test User")
                .param("password", "Password123!")
                .param("email", email)
                .param("confirmPassword", "Password123!"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(formLogin("/login")
                .user("username", email)
                .password("Password123!"))
                .andExpect(status().is3xxRedirection());

        // Now try to access admin endpoint
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());  // Should be forbidden for non-admin

        logger.info("Authentication security test completed");
    }

    /**
     * Test for sensitive data exposure
     * This test checks if sensitive data is properly protected
     */
    @Test
    public void testSensitiveDataProtection() throws Exception {
        // Check if password is exposed in user profile
        String email = "data_test_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String password = "SecurePassword123!";

        // Register a user
        mockMvc.perform(post("/register")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Data Test User")
                .param("password", password)
                .param("email", email)
                .param("confirmPassword", password))
                .andExpect(status().is3xxRedirection());

        // Login
        mockMvc.perform(formLogin("/login")
                .user("username", email)
                .password(password))
                .andExpect(status().is3xxRedirection());

        // Get user profile
        MvcResult result = mockMvc.perform(get("/api/user/profile")
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains(password), "Password should not be exposed in API response");

        // Check for sensitive headers
        Map<String, String> sensitiveHeaders = new HashMap<>();
        sensitiveHeaders.put("X-Powered-By", "Exposes technology stack");
        sensitiveHeaders.put("Server", "Exposes server information");

        result = mockMvc.perform(get("/"))
                .andReturn();

        for (Map.Entry<String, String> header : sensitiveHeaders.entrySet()) {
            String headerValue = result.getResponse().getHeader(header.getKey());
            if (headerValue != null) {
                logger.warn("Sensitive header found: {} - {}", header.getKey(), header.getValue());
            }
        }

        logger.info("Sensitive data protection test completed");
    }

    /**
     * Test for directory traversal attacks
     * This test attempts to access files outside the intended directory
     */
    @Test
    public void testDirectoryTraversalProtection() throws Exception {
        String[] traversalPayloads = {
            "../../../etc/passwd",
            "..\\..\\..\\Windows\\system.ini",
            "/WEB-INF/web.xml",
            "file:///etc/shadow"
        };

        for (String payload : traversalPayloads) {
            mockMvc.perform(get("/" + payload))
                    .andExpect(status().is4xxClientError());  // Should return 404 or similar
        }

        logger.info("Directory traversal protection test completed");
    }
}
