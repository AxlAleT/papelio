# Papelio - Research & Authentication System

Papelio is a research platform developed using Spring Boot. It allows users to search for academic articles, manage user roles, track research history, and ensure compliance with data security standards.

## Features

### **1. Core Search & User Management**
- **Basic Article Search:** Find articles by keywords/title via Semantic Scholar/CrossRef APIs.
- **Article Metadata Enrichment:** DOI, citations, and author details fetched asynchronously.
- **User Registration & Credential Storage:** Secure registration with email/password, BCrypt encoding.
- **Form-Based Authentication:** Secure login with CSRF protection and session management.
- **Role-Based Dashboard Access:** Restrict admin features using role-based access control.

### **2. Personalization & History**
- **Search History Tracking:** Logs user queries and timestamps, linked to authenticated users.
- **Favorites Management:** Save/remove articles for easy access (many-to-many JPA).
- **Personalized Search Suggestions:** Suggestions based on user history, periodically analyzed.

### **3. Compliance & Data Security**
- **API Rate Limit Compliance:** Retries and usage monitoring to avoid exceeding quotas.
- **GDPR-Compliant Data Deletion:** Secure, auditable user data deletion upon request.
- **Secure API Key Storage:** Externalized secrets via `application.properties`.
- **Audit Logging:** Tracks searches, logins, and user activity with Spring Data Auditing.

### **4. Recommendation Engine**
- **Semantic Scholar-Based Recommendations:** Related articles via recommendation API.
- **Trending Research Alerts:** Highlights popular publications, scheduled polling and caching.

### **5. Performance & Scalability**
- **API Response Caching:** Uses Caffeine (and optionally Redis) for frequent queries.
- **Concurrent Session Support:** Spring Session with Redis for distributed session storage.
- **Load-Tolerant Authentication:** Optimized Spring Security filter chain for high concurrency.
- **Health Monitoring:** Tracks API latency and errors.

### **6. Accessibility & Integration**
- **Theme Management:** Supports dark/light/auto themes, user preferences in CSS variables.
- **Metadata Normalization:** Standardizes data from CrossRef/Semantic Scholar via DTO mapping.
- **Error Handling:** Global exception handling with user-friendly API/client errors.

### **7. Advanced Security**
- **Brute-Force Login Prevention:** Blocks repeated failed login attempts.
- **Session Timeout Management:** Automatic logout for inactive users.
- **Password Policy Enforcement:** Requires strong passwords with custom validation.

## Installation & Deployment

### **1. Local Execution**

1. **Clone the repository:**
   ```bash
   git clone https://github.com/AxlAleT/papelio
   cd papelio
   ```

2. **Build and package the application:**
    ```bash
    ./mvnw clean package -DskipTests
    ```

3. **Run the application:**
    ```bash
    java -jar target/*.jar
    ```
   The application will be available at [http://localhost:8080](http://localhost:8080).

### **2. Docker Deployment**

1. **Build the Docker image:**
   ```bash
   docker build -t papelio .
   ```

2. **Run using Docker Compose:**
   ```bash
   docker-compose up
   ```
    - Application will be accessible at [http://localhost](http://localhost).
    - PostgreSQL database will be mapped to port 5433.

## License
This project is open-source and available under the MIT License.

