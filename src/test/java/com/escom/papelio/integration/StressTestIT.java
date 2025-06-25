package com.escom.papelio.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stress and load testing for the Papelio application.
 * This class simulates JMeter-style testing to evaluate:
 * 1. Load Testing - System behavior under expected load
 * 2. Stress Testing - System breaking point under extreme load
 */
@SpringBootTest
@AutoConfigureMockMvc
public class StressTestIT {

    private static final Logger logger = LoggerFactory.getLogger(StressTestIT.class);

    @Autowired
    private MockMvc mockMvc;

    private final AtomicBoolean testShouldContinue = new AtomicBoolean(true);
    private final AtomicInteger successfulRegistrations = new AtomicInteger(0);
    private final AtomicInteger successfulLogins = new AtomicInteger(0);
    private final AtomicInteger successfulDashboardAccesses = new AtomicInteger(0);
    private final AtomicInteger successfulSearches = new AtomicInteger(0);
    private final AtomicInteger failedOperations = new AtomicInteger(0);
    private final List<Long> responseTimesList = new CopyOnWriteArrayList<>();

    // Separate response times by endpoint for more detailed analysis
    private final List<Long> registrationResponseTimes = new CopyOnWriteArrayList<>();
    private final List<Long> loginResponseTimes = new CopyOnWriteArrayList<>();
    private final List<Long> dashboardResponseTimes = new CopyOnWriteArrayList<>();
    private final List<Long> searchResponseTimes = new CopyOnWriteArrayList<>();

    private static class UserCredential {
        final String email;
        final String password;
        final String name;

        UserCredential(String email, String password, String name) {
            this.email = email;
            this.password = password;
            this.name = name;
        }
    }

    /**
     * Stress test that progressively increases load until system failure
     * Simulates JMeter stress testing by finding the breaking point
     */
    @Test
    @Timeout(value = 20, unit = TimeUnit.MINUTES)
    public void performStressTestUntilFailure() throws Exception {
        // Configuration parameters
        int initialThreads = 10;        // Initial user load
        int maxThreads = 200;           // Maximum user load to test
        int incrementStep = 10;         // User load increment per step
        int requestsPerThread = 15;     // Actions per user
        long stabilizationDelayMs = 500; // Delay between load increases

        // Test metrics
        int lastStableThreadCount = 0;  // Last thread count that completed successfully
        double avgResponseTimeAtBreaking = 0; // Average response time at breaking point

        ExecutorService executorService = null;
        
        logger.info("==========================================");
        logger.info("JMeter-STYLE STRESS TEST STARTED AT: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        logger.info("Initial threads: {}, Max threads: {}, Increment: {}, Requests per thread: {}",
                initialThreads, maxThreads, incrementStep, requestsPerThread);
        logger.info("==========================================");
        
        try {
            LocalDateTime startTime = LocalDateTime.now();

            for (int threadCount = initialThreads; 
                 threadCount <= maxThreads && testShouldContinue.get(); 
                 threadCount += incrementStep) {
                
                executorService = Executors.newFixedThreadPool(threadCount);
                List<UserCredential> registeredUsers = new CopyOnWriteArrayList<>();
                CountDownLatch latch = new CountDownLatch(threadCount);

                logger.info("Testing with {} concurrent users...", threadCount);
                
                // Submit tasks to the thread pool
                for (int i = 0; i < threadCount; i++) {
                    executorService.submit(() -> {
                        try {
                            for (int j = 0; j < requestsPerThread && testShouldContinue.get(); j++) {
                                // Register a new user
                                UserCredential user = generateRandomUser();
                                boolean registered = registerUser(user);
                                
                                if (registered) {
                                    registeredUsers.add(user);
                                    successfulRegistrations.incrementAndGet();
                                    
                                    // Login with the registered user
                                    boolean loggedIn = loginUser(user);
                                    
                                    if (loggedIn) {
                                        successfulLogins.incrementAndGet();
                                        
                                        // Access dashboard
                                        boolean accessed = accessDashboard();
                                        if (accessed) {
                                            successfulDashboardAccesses.incrementAndGet();
                                        }

                                        // Perform search - new operation to test API performance
                                        boolean searchPerformed = performSearch("machine learning");
                                        if (searchPerformed) {
                                            successfulSearches.incrementAndGet();
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            failedOperations.incrementAndGet();
                            logger.error("Test failure detected: {}", e.getMessage(), e);
                            testShouldContinue.set(false);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                // Wait for all threads to complete
                boolean completed = latch.await(3, TimeUnit.MINUTES);
                if (!completed) {
                    logger.warn("Test timed out with {} threads", threadCount);
                    avgResponseTimeAtBreaking = calculateAverageResponseTime(responseTimesList);
                    break;
                }
                
                // Shutdown the executor service
                executorService.shutdown();
                boolean terminated = executorService.awaitTermination(45, TimeUnit.SECONDS);
                if (!terminated) {
                    executorService.shutdownNow();
                }
                
                // Check if test should continue
                if (!testShouldContinue.get()) {
                    logger.info("Test failed with {} threads", threadCount);
                    avgResponseTimeAtBreaking = calculateAverageResponseTime(responseTimesList);
                    break;
                }
                
                // Print current statistics
                printStatistics(threadCount);
                lastStableThreadCount = threadCount;

                // Give the system time to stabilize
                Thread.sleep(stabilizationDelayMs);
            }
            
            Duration testDuration = Duration.between(startTime, LocalDateTime.now());
            logger.info("Stress test completed in {} minutes", testDuration.toMinutes());
            logger.info("System breaking point: {} concurrent users", lastStableThreadCount + incrementStep);
            logger.info("Average response time at breaking point: {}ms", avgResponseTimeAtBreaking);

            printFinalStatistics();
            
            // Perform assertions to validate the stress test results
            assertTrue(lastStableThreadCount > 0, "System should handle at least some concurrent users");

        } finally {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
            }
        }
    }

    /**
     * Load test that verifies system performance under expected load
     * Simulates JMeter load testing with constant user load
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    public void performLoadTest() throws Exception {
        // Configuration for expected production load
        int concurrentUsers = 50;  // Expected number of concurrent users
        int requestsPerUser = 20;  // Expected actions per user
        long testDurationSeconds = 60; // Test duration in seconds

        // Performance thresholds
        long maxAcceptableAvgResponseTime = 500; // Max acceptable average response time in ms
        long maxAcceptable95thPercentile = 1000; // Max acceptable 95th percentile response time in ms

        ExecutorService executorService = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch completionLatch = new CountDownLatch(concurrentUsers);
        AtomicBoolean keepRunning = new AtomicBoolean(true);

        logger.info("==========================================");
        logger.info("JMeter-STYLE LOAD TEST STARTED AT: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        logger.info("Concurrent users: {}, Test duration: {} seconds", concurrentUsers, testDurationSeconds);
        logger.info("==========================================");

        try {
            // Schedule test end after specified duration
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                logger.info("Load test duration completed, stopping test...");
                keepRunning.set(false);
            }, testDurationSeconds, TimeUnit.SECONDS);

            // Submit user tasks
            for (int i = 0; i < concurrentUsers; i++) {
                executorService.submit(() -> {
                    try {
                        UserCredential user = generateRandomUser();
                        boolean registered = registerUser(user);

                        if (registered) {
                            // Keep performing actions until test time expires
                            int actionsPerformed = 0;
                            while (keepRunning.get() && actionsPerformed < requestsPerUser) {
                                boolean loggedIn = loginUser(user);
                                if (loggedIn) {
                                    accessDashboard();
                                    performSearch("artificial intelligence");
                                    performSearch("machine learning algorithms");
                                }
                                actionsPerformed++;

                                // Small delay between user actions
                                Thread.sleep(100);
                            }
                        }
                    } catch (Exception e) {
                        failedOperations.incrementAndGet();
                        logger.error("Error during load test: {}", e.getMessage());
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            // Wait for test completion
            completionLatch.await();
            scheduler.shutdown();

            // Calculate and validate performance metrics
            double avgResponseTime = calculateAverageResponseTime(responseTimesList);
            long p95ResponseTime = calculatePercentile(responseTimesList, 95);

            logger.info("Load test completed. Average response time: {}ms, 95th percentile: {}ms",
                    avgResponseTime, p95ResponseTime);

            // Assert that performance meets requirements
            assertTrue(avgResponseTime < maxAcceptableAvgResponseTime,
                    "Average response time should be below " + maxAcceptableAvgResponseTime + "ms");
            assertTrue(p95ResponseTime < maxAcceptable95thPercentile,
                    "95th percentile response time should be below " + maxAcceptable95thPercentile + "ms");

        } finally {
            executorService.shutdownNow();
        }
    }

    // Helper methods

    private UserCredential generateRandomUser() {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String email = "stress_test_" + uuid + "@example.com";
        String password = "StrongPass_" + uuid + "!";
        String name = "Test User " + uuid;
        
        return new UserCredential(email, password, name);
    }
    
    private boolean registerUser(UserCredential user) {
        try {
            long startTime = System.currentTimeMillis();
            
            MvcResult result = mockMvc.perform(post("/register")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("name", user.name)
                    .param("password", user.password)
                    .param("email", user.email)
                    .param("confirmPassword", user.password))
                    .andReturn();
            
            long responseTime = System.currentTimeMillis() - startTime;
            responseTimesList.add(responseTime);
            registrationResponseTimes.add(responseTime);

            int status = result.getResponse().getStatus();
            if (status >= 200 && status < 400) {
                logger.debug("User registered successfully: {}", user.email);
                return true;
            } else {
                logger.debug("Registration failed for user: {}, status: {}", user.email, status);
                return false;
            }
        } catch (Exception e) {
            failedOperations.incrementAndGet();
            logger.error("Error during registration: {}", e.getMessage());
            testShouldContinue.set(false);
            return false;
        }
    }
    
    private boolean loginUser(UserCredential user) {
        try {
            long startTime = System.currentTimeMillis();
            
            MvcResult result = mockMvc.perform(formLogin("/login")
                    .user("username", user.email)
                    .password(user.password))
                    .andReturn();
            
            long responseTime = System.currentTimeMillis() - startTime;
            responseTimesList.add(responseTime);
            loginResponseTimes.add(responseTime);

            int status = result.getResponse().getStatus();
            if (status >= 200 && status < 400) {
                logger.debug("User logged in successfully: {}", user.email);
                return true;
            } else {
                logger.debug("Login failed for user: {}, status: {}", user.email, status);
                return false;
            }
        } catch (Exception e) {
            failedOperations.incrementAndGet();
            logger.error("Error during login: {}", e.getMessage());
            testShouldContinue.set(false);
            return false;
        }
    }
    
    private boolean accessDashboard() {
        try {
            long startTime = System.currentTimeMillis();
            
            MvcResult result = mockMvc.perform(get("/dashboard"))
                    .andReturn();
            
            long responseTime = System.currentTimeMillis() - startTime;
            responseTimesList.add(responseTime);
            dashboardResponseTimes.add(responseTime);

            int status = result.getResponse().getStatus();
            if (status >= 200 && status < 400) {
                logger.debug("Dashboard accessed successfully");
                return true;
            } else {
                logger.debug("Dashboard access failed, status: {}", status);
                return false;
            }
        } catch (Exception e) {
            failedOperations.incrementAndGet();
            logger.error("Error accessing dashboard: {}", e.getMessage());
            testShouldContinue.set(false);
            return false;
        }
    }
    
    private boolean performSearch(String query) {
        try {
            long startTime = System.currentTimeMillis();

            MvcResult result = mockMvc.perform(post("/api/search")
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"query\":\"" + query + "\",\"page\":0,\"size\":10}"))
                    .andReturn();

            long responseTime = System.currentTimeMillis() - startTime;
            responseTimesList.add(responseTime);
            searchResponseTimes.add(responseTime);

            int status = result.getResponse().getStatus();
            if (status >= 200 && status < 400) {
                logger.debug("Search performed successfully: {}", query);
                return true;
            } else {
                logger.debug("Search failed: {}, status: {}", query, status);
                return false;
            }
        } catch (Exception e) {
            failedOperations.incrementAndGet();
            logger.error("Error performing search: {}", e.getMessage());
            testShouldContinue.set(false);
            return false;
        }
    }

    private void printStatistics(int threadCount) {
        logger.info("------------------------");
        logger.info("Statistics for {} threads:", threadCount);
        logger.info("Successful registrations: {}", successfulRegistrations.get());
        logger.info("Successful logins: {}", successfulLogins.get());
        logger.info("Successful dashboard accesses: {}", successfulDashboardAccesses.get());
        logger.info("Successful searches: {}", successfulSearches.get());
        logger.info("Failed operations: {}", failedOperations.get());
        
        if (!responseTimesList.isEmpty()) {
            printResponseTimeStats("Overall", responseTimesList);
            printResponseTimeStats("Registration", registrationResponseTimes);
            printResponseTimeStats("Login", loginResponseTimes);
            printResponseTimeStats("Dashboard", dashboardResponseTimes);
            printResponseTimeStats("Search", searchResponseTimes);
        }
        logger.info("------------------------");
    }
    
    private void printResponseTimeStats(String operationType, List<Long> times) {
        if (times.isEmpty()) return;

        double avg = calculateAverageResponseTime(times);
        long max = times.stream().mapToLong(Long::longValue).max().orElse(0);
        long p50 = calculatePercentile(times, 50);
        long p90 = calculatePercentile(times, 90);
        long p95 = calculatePercentile(times, 95);
        long p99 = calculatePercentile(times, 99);

        logger.info("{} response times - Avg: {}ms, Max: {}ms, 50th: {}ms, 90th: {}ms, 95th: {}ms, 99th: {}ms",
                operationType, String.format("%.2f", avg), max, p50, p90, p95, p99);
    }

    private double calculateAverageResponseTime(List<Long> times) {
        if (times.isEmpty()) return 0;
        return times.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private long calculatePercentile(List<Long> times, int percentile) {
        if (times.isEmpty()) return 0;
        List<Long> sortedTimes = new ArrayList<>(times);
        java.util.Collections.sort(sortedTimes);
        int index = (int) Math.ceil(percentile / 100.0 * sortedTimes.size()) - 1;
        index = Math.max(0, Math.min(index, sortedTimes.size() - 1));
        return sortedTimes.get(index);
    }

    private void printFinalStatistics() {
        logger.info("\n========== FINAL RESULTS ==========");
        logger.info("TEST COMPLETED AT: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        logger.info("Total successful registrations: {}", successfulRegistrations.get());
        logger.info("Total successful logins: {}", successfulLogins.get());
        logger.info("Total successful dashboard accesses: {}", successfulDashboardAccesses.get());
        logger.info("Total successful searches: {}", successfulSearches.get());
        logger.info("Total failed operations: {}", failedOperations.get());
        
        logger.info("\nPERFORMANCE SUMMARY:");
        printResponseTimeStats("Overall", responseTimesList);
        printResponseTimeStats("Registration", registrationResponseTimes);
        printResponseTimeStats("Login", loginResponseTimes);
        printResponseTimeStats("Dashboard", dashboardResponseTimes);
        printResponseTimeStats("Search", searchResponseTimes);
        logger.info("====================================");
    }
}
