# Papelio Application Testing Documentation

This document provides an overview of the testing strategy, types of tests, and tools used in the Papelio application.

## 1. Overall Testing Strategy

The project employs a multi-layered testing approach to ensure code quality and application stability. This includes:

1.  **Unit Tests:**
    *   **Service Layer:** Focus on testing the business logic within service classes in isolation. Dependencies like repositories are mocked to ensure that only the service's logic is under test. This verifies data transformations, validations, and interactions with data access layers.
    *   **Controller Layer (MVC & REST):** Test the logic within controllers that handle web requests (both traditional MVC and REST APIs). Services and other dependencies are mocked to isolate controller behavior, ensuring correct request mapping, parameter handling, view resolution (for MVC), and response generation (especially for REST APIs, checking status codes and JSON payloads).

2.  **Integration Tests:**
    *   **Component Interaction:** Verify the collaboration between different parts of the application, such as controllers, services, and repositories, often involving the actual Spring application context. This ensures that components are wired correctly and interact as expected.
    *   **Authentication & Security:** Dedicated integration tests validate the complete authentication and authorization flows, including user registration, login, logout, and access control to different parts of the application based on user roles.
    *   **Database Interaction:** Some integration tests might interact with a test database (potentially using in-memory databases or test containers) to ensure data persistence and retrieval logic works correctly.
    *   **Full Application Context Loading:** A basic test ensures the Spring Boot application context loads without errors, confirming the overall configuration is sound.

3.  **Specialized Integration Tests (Potentially):**
    *   **Security Tests (`SecurityTestIT.java`):** Likely focus on more in-depth security vulnerability checks or specific security configurations.
    *   **Stress Tests (`StressTestIT.java`):** Aim to evaluate the application's performance and stability under heavy load conditions.

**Key Characteristics:**

*   **Isolation:** Unit tests heavily rely on mocking (using Mockito) to isolate the unit under test.
*   **Spring Ecosystem:** Leverages Spring Test, Spring Boot Test, and Spring Security Test for comprehensive testing within the Spring framework.
*   **Automation:** Tests are written using JUnit 5, allowing for automated execution as part of the build or CI/CD process.
*   **Coverage:** The strategy aims to cover critical aspects of the application, from individual business logic units to broader component interactions and security mechanisms.

## 2. Test Categories and Descriptions

### 2.1. Controller Tests

*   **Location:** `src/test/java/com/escom/papelio/controller/` (excluding REST controllers)
*   **Purpose:** Test the functionality of Spring MVC controllers, ensuring they handle web requests, interact with services (mocked), and return appropriate views.
*   **Key Tools:** JUnit 5, Mockito, Spring Test (`MockMvc`).
*   **Test Classes:**
    *   `AdminControllerTest.java`: Tests the admin panel and user management view rendering.
    *   `AuthControllerTest.java`: Tests endpoints related to user authentication (login, registration views).
    *   `SearchControllerTest.java`: Tests the controller responsible for handling search page rendering and search result displays.
    *   `UserControllerTest.java`: Tests user-specific view rendering, like the dashboard or profile pages.

### 2.2. REST Controller Tests

*   **Location:** `src/test/java/com/escom/papelio/controller/` (specific REST controllers)
*   **Purpose:** Test Spring MVC controllers that handle RESTful API requests, focusing on API logic, request/response (JSON) handling, and status codes. Services are mocked.
*   **Key Tools:** JUnit 5, Mockito, Spring Test (`MockMvc`), JSONPath.
*   **Test Classes:**
    *   `AdminRESTControllerTest.java`: Tests REST APIs for managing users (CRUD operations).
    *   `SearchRestControllerTest.java`: Tests REST APIs related to performing searches and retrieving search suggestions.
    *   `UserRestControllerTest.java`: Tests REST APIs for user-specific actions like managing favorites or viewing history.

### 2.3. Service Tests

*   **Location:** `src/test/java/com/escom/papelio/service/`
*   **Purpose:** Unit test the business logic within service classes. Dependencies (e.g., repositories) are mocked.
*   **Key Tools:** JUnit 5, Mockito.
*   **Test Classes:**
    *   `AdminServiceTest.java`: Tests logic for user management by administrators (listing, creating, updating, deleting users).
    *   `ArticleFavoriteServiceTest.java`: Tests logic related to managing users' favorite articles.
    *   `ArticleViewHistoryServiceTest.java`: Tests logic for recording and retrieving user article view history.
    *   `SearchHistoryServiceTest.java`: Tests logic for managing user search history.
    *   `SemanticScholarServiceTest.java`: Tests the service responsible for interacting with the Semantic Scholar API (or its mock).
    *   `UserServiceTest.java`: Tests general user-related business logic, potentially profile updates or user-specific data retrieval.

### 2.4. Integration Tests

*   **Location:** `src/test/java/com/escom/papelio/integration/`
*   **Purpose:** Test the interaction between multiple application components, often involving the full Spring application context and potentially a test database.
*   **Key Tools:** JUnit 5, Spring Boot Test (`@SpringBootTest`), `MockMvc`, Spring Security Test.
*   **Test Classes:**
    *   `AuthenticationFlowIntegrationTest.java`: Tests the complete user authentication and authorization lifecycle, including registration, login, logout, and access control.
    *   `SearchIntegrationTest.java`: Tests the search functionality end-to-end, from API request to service interaction and potentially database checks.
    *   `SecurityTestIT.java`: Likely contains more focused security-related integration tests, possibly checking specific configurations or vulnerability mitigations. (The `IT` suffix often denotes integration tests run by the Failsafe plugin).
    *   `StressTestIT.java`: Designed to test the application's performance and stability under load.

### 2.5. Application Context Tests

*   **Location:** `src/test/java/com/escom/papelio/`
*   **Purpose:** A basic integration test to ensure the Spring Boot application context loads successfully.
*   **Key Tools:** JUnit 5, Spring Boot Test (`@SpringBootTest`).
*   **Test Classes:**
    *   `papelioApplicationTests.java`: Contains a simple `contextLoads()` test to verify the application starts correctly.

## 3. Testing Tools and Technologies

The following tools and technologies are used for testing in this project:

*   **JUnit 5:** The core testing framework for writing and running tests (e.g., `@Test`, `@BeforeEach`, assertions).
*   **Mockito:** A mocking framework used to create test doubles (mocks, spies) of dependencies, enabling isolated unit testing (e.g., `@Mock`, `@InjectMocks`, `when()`, `verify()`).
*   **Spring Test:** Part of the Spring Framework, providing utilities for testing Spring applications. This includes:
    *   `MockMvc`: For testing Spring MVC controllers (both web and REST) without needing a full servlet container.
    *   `@SpringBootTest`: For loading the full Spring Boot application context in integration tests.
    *   `@AutoConfigureMockMvc`: To automatically configure `MockMvc` in integration tests.
*   **Spring Security Test:** Provides support for testing Spring Security features (e.g., `@WithMockUser`, `formLogin()`, security-specific matchers).
*   **Hamcrest Matchers:** Used for writing flexible and readable assertion statements (often used implicitly through Spring Test's `ResultMatchers`).
*   **JSONPath:** Used for querying and asserting parts of JSON responses in REST controller tests (via `MockMvcResultMatchers.jsonPath`).
*   **Jackson ObjectMapper:** (Potentially used directly or indirectly) for serializing/deserializing Java objects to/from JSON in tests involving REST APIs.
*   **Maven Surefire/Failsafe Plugin:** (Assumed, as this is a Maven project) These plugins are typically used to execute unit tests (Surefire) and integration tests (Failsafe) during the Maven build lifecycle.
*   **SQL Script Execution (`@Sql`):** (Potentially, based on common practices for integration tests) For setting up and tearing down database states for specific tests.

## 4. Test Plan Resources

*   **Test Data Setup:**
    *   Unit tests often create test data directly within the test methods or setup methods (e.g., `@BeforeEach`).
    *   Integration tests may use:
        *   Embedded databases (like H2) configured in `application-test.properties`.
        *   SQL scripts (`src/test/resources/sql/test-user.sql`) to populate data, often used with the `@Sql` annotation.
*   **Test Configuration:**
    *   `src/test/resources/application-test.properties`: Contains specific configurations for the test environment, such as database connection details for an in-memory database.
