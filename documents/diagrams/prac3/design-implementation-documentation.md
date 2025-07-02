# Ejercicio 3: Implementación del Modelo de Diseño y Patrones

## Documentación Completa del Modelo de Diseño y Patrones para Sistema Papelio

### 🎯 Visión General del Sistema

**Papelio** es una plataforma de investigación académica desarrollada con **Spring Boot 3.4.2** y **Java 21**, que implementa las siguientes características principales definidas en el README:

#### **Características Implementadas:**

1. **🔍 Core Search & User Management**
   - Búsqueda básica de artículos académicos via Semantic Scholar/CrossRef APIs
   - Enriquecimiento de metadatos de artículos (DOI, citas, detalles de autores)
   - Registro de usuarios con almacenamiento seguro de credenciales (BCrypt)
   - Autenticación basada en formularios con protección CSRF
   - Acceso al dashboard basado en roles

2. **� Personalization & History**
   - Seguimiento del historial de búsquedas vinculado a usuarios autenticados
   - Gestión de favoritos con relaciones many-to-many JPA
   - Sugerencias de búsqueda personalizadas basadas en historial

3. **🔒 Compliance & Data Security**
   - Cumplimiento de límites de tasa de APIs con reintentos y monitoreo
   - Eliminación de datos compatible con GDPR con auditoría
   - Almacenamiento seguro de claves API via application.properties
   - Registro de auditoría con Spring Data Auditing

4. **🤖 Recommendation Engine**
   - Recomendaciones basadas en Semantic Scholar API
   - Alertas de investigación tendencia con polling programado y caching

5. **⚡ Performance & Scalability**
   - Caching de respuestas API usando Caffeine (y opcionalmente Redis)
   - Soporte de sesiones concurrentes con Spring Session y Redis
   - Autenticación tolerante a carga con cadena de filtros optimizada
   - Monitoreo de salud con seguimiento de latencia y errores

6. **🎨 Accessibility & Integration**
   - Gestión de temas (oscuro/claro/automático)
   - Normalización de metadatos via mapeo DTO
   - Manejo global de excepciones

7. **🛡️ Advanced Security**
   - Prevención de ataques de fuerza bruta en login
   - Gestión de timeout de sesiones
   - Aplicación de políticas de contraseñas fuertes

#### **Stack Tecnológico (según pom.xml):**
- **Framework:** Spring Boot 3.4.2, Spring Security 6.x, Spring Data JPA
- **Lenguaje:** Java 21 LTS
- **Base de Datos:** PostgreSQL con Hibernate ORM
- **Caching:** Caffeine Cache, Redis (opcional)
- **Frontend:** Thymeleaf con Spring Security extras, Bootstrap
- **Resilience:** Resilience4j, Spring Retry
- **Testing:** JUnit 5, Mockito, Testcontainers
- **Monitoring:** Spring Boot Actuator, Micrometer, JaCoCo
- **Build:** Maven 3.x, Docker & Docker Compose
- **Quality:** SonarQube Integration

### �📋 Archivos de Diagramas Creados

1. **`design-class-diagram.puml`** - Diagrama completo de clases de diseño
2. **`strategy-pattern-implementation.puml`** - Implementación del patrón Strategy
3. **`factory-pattern-implementation.puml`** - Implementación del patrón Factory
4. **`technical-architecture-model.puml`** - Modelo de arquitectura técnica
5. **`deployment-diagram.puml`** - Diagrama de despliegue del sistema
6. **`docker-deployment-diagram.puml`** - Diagrama de despliegue Docker específico

---

## 🎯 1. Diagrama de Clases de Diseño

### 1.1 Organización en Paquetes

El sistema está organizado siguiendo una **arquitectura por capas** con separación clara de responsabilidades:

#### **Capa de Presentación (`controller`)**
- **Web Controllers:** Manejo de vistas HTML con Thymeleaf
- **REST Controllers:** APIs JSON para AJAX y servicios
- **Anotaciones:** `@Controller`, `@RestController`, `@RequestMapping`

#### **Capa de Lógica de Negocio (`service`)**
- **Servicios de Dominio:** Lógica específica del negocio
- **Servicios de Integración:** Comunicación con APIs externas
- **Anotaciones:** `@Service`, `@Transactional`, `@Cacheable`

#### **Capa de Acceso a Datos (`repository`)**
- **Repositorios JPA:** Interfaz de acceso a datos
- **Consultas Personalizadas:** `@Query` para operaciones complejas
- **Anotaciones:** `@Repository`, herencia de `JpaRepository`

#### **Modelos de Datos (`model`, `dto`)**
- **Entidades JPA:** Mapeo objeto-relacional
- **DTOs:** Transferencia de datos entre capas
- **Modelos Externos:** Representación de APIs externas

#### **Utilidades (`mapper`, `config`, `client`)**
- **Mappers:** Transformación entre objetos
- **Configuración:** Beans de Spring y configuraciones
- **Clientes:** Integración con servicios externos

### 1.2 Detalles de Implementación Incluidos

#### **Métodos con Parámetros y Tipos de Retorno**
```java
+ searchArticles(searchRequest: SearchRequestDTO): SearchResponseDTO
+ saveArticleFavorite(userEmail: String, articleId: String, title: String): boolean
+ getUserSearchHistory(userEmail: String): List<SearchHistory>
```

#### **Atributos con Visibilidad y Tipos**
```java
- id: Long <<@Id @GeneratedValue>>
- email: String <<@Column(unique=true, nullable=false) @Email>>
- searchDate: LocalDateTime <<@CreatedDate>>
```

#### **Anotaciones JPA y Spring**
- **Persistencia:** `@Entity`, `@Id`, `@GeneratedValue`, `@Column`
- **Validación:** `@NotBlank`, `@Email`, `@Size`
- **Spring:** `@Service`, `@Controller`, `@Autowired`, `@Cacheable`
- **Seguridad:** `@PreAuthorize`, `@Secured`

---

## 🔧 2. Patrones de Diseño Implementados

### 2.1 Patrón Strategy - Motor de Búsqueda

#### **Justificación de Selección:**
El patrón Strategy es ideal para el sistema Papelio porque:

1. **Múltiples Proveedores:** Semantic Scholar, CrossRef, ArXiv
2. **Algoritmos Intercambiables:** Diferentes estrategias de búsqueda
3. **Failover Automático:** Cambio dinámico entre proveedores
4. **Extensibilidad:** Fácil adición de nuevos proveedores
5. **Testing:** Aislamiento de lógica por proveedor

#### **Implementación del Patrón:**

**Strategy Interface:**
```java
public interface SearchStrategy {
    SearchResponseDTO search(SearchRequestDTO request);
    Optional<ArticleDTO> getArticleById(String id);
    SearchResponseDTO getRecommendations(RecommendationRequestDTO request);
    boolean isAvailable();
    int getPriority();
    String getProviderName();
}
```

**Concrete Strategies:**
- `SemanticScholarSearchStrategy` (Prioridad 1)
- `CrossRefSearchStrategy` (Prioridad 2) 
- `ArxivSearchStrategy` (Prioridad 3)

**Context Class:**
```java
@Service
public class SearchEngineContext {
    private List<SearchStrategy> strategies;
    private CircuitBreakerService circuitBreakerService;
    
    public SearchResponseDTO search(SearchRequestDTO request) {
        SearchStrategy strategy = selectBestStrategy();
        return executeWithFallback(() -> strategy.search(request));
    }
}
```

#### **Beneficios del Patrón Strategy:**
- **Flexibility:** Cambio dinámico de algoritmos en runtime
- **Maintainability:** Cada estrategia en clase separada
- **Testability:** Mock de estrategias individuales
- **Circuit Breaker Integration:** Failover automático entre proveedores
- **Performance:** Selección de proveedor más rápido disponible

### 2.2 Patrón Factory - Creación de DTOs

#### **Justificación de Selección:**
El patrón Factory Abstract es apropiado porque:

1. **Múltiples Formatos:** Diferentes estructuras de respuesta por API
2. **Normalización:** Conversión a formato común (ArticleDTO)
3. **Encapsulación:** Lógica de creación encapsulada
4. **Extensibilidad:** Nuevos proveedores sin cambiar código cliente
5. **Consistency:** Creación estandarizada de objetos

#### **Implementación del Patrón:**

**Abstract Factory:**
```java
public abstract class ResponseFactory {
    public abstract SearchResponseDTO createSearchResponse(
        List<ArticleDTO> articles, long total, int page, String query);
    public abstract ArticleDTO createArticleDTO(Object source);
    public abstract SearchResponseDTO createErrorResponse(
        Exception error, String query);
}
```

**Concrete Factories:**
- `SemanticScholarResponseFactory`
- `CrossRefResponseFactory`
- `ArxivResponseFactory`

**Factory Registry:**
```java
@Service
public class ResponseFactoryRegistry {
    private Map<String, ResponseFactory> factories;
    
    public ResponseFactory getFactory(String providerType) {
        return factories.get(providerType);
    }
}
```

#### **Beneficios del Patrón Factory:**
- **Encapsulation:** Lógica de creación de objetos encapsulada
- **Flexibility:** Fácil adición de nuevos tipos de respuesta
- **Consistency:** Formato estandarizado independiente del proveedor
- **Testing:** Mock factories para pruebas unitarias
- **Single Responsibility:** Cada factory maneja un tipo de proveedor

### 2.3 Builder Pattern - Construcción de DTOs Complejos

#### **Implementación del Pattern Builder:**
```java
public class SearchResponseBuilder {
    private List<ArticleDTO> articles;
    private long totalResults;
    private int currentPage;
    private String query;
    
    public SearchResponseBuilder articles(List<ArticleDTO> articles) {
        this.articles = articles;
        return this;
    }
    
    public SearchResponseDTO build() {
        validate();
        return new SearchResponseDTO(articles, totalResults, 
                                   currentPage, totalPages, query);
    }
}
```

---

## 🏗️ 3. Modelo de Implementación - Arquitectura Técnica

### 3.1 Stack Tecnológico

#### **Framework Principal**
- **Spring Boot 3.x** - Framework principal de aplicación
- **Spring MVC** - Patrón Model-View-Controller
- **Spring Security 6.x** - Autenticación y autorización
- **Spring Data JPA** - Acceso a datos simplificado
- **Spring Cache** - Abstracción de caching

#### **Persistencia**
- **PostgreSQL 15+** - Base de datos principal
- **Hibernate ORM** - Mapeo objeto-relacional
- **Redis 7.x** - Cache distribuido y sesiones
- **HikariCP** - Pool de conexiones optimizado

#### **Frontend**
- **Thymeleaf 3.x** - Motor de plantillas server-side
- **Bootstrap 5.x** - Framework CSS responsive
- **JavaScript ES6+** - Funcionalidad cliente
- **Chart.js** - Visualización de datos

#### **DevOps y Testing**
- **Maven 3.x** - Gestión de dependencias y build
- **Docker & Docker Compose** - Containerización
- **JUnit 5** - Testing unitario
- **Testcontainers** - Testing de integración

### 3.2 Arquitectura por Capas

#### **Presentation Layer (Web Tier)**
- **Web UI:** Interfaces de usuario con Thymeleaf
- **REST API:** Endpoints JSON para AJAX
- **MVC Controllers:** Controladores Spring MVC

#### **Business Logic Layer (Service Tier)**
- **Core Services:** Lógica de negocio principal
- **Pattern Implementations:** Strategy, Factory, Observer
- **Integration Layer:** Clientes de APIs externas

#### **Data Access Layer (Data Tier)**
- **Repository Layer:** Spring Data JPA repositories
- **Data Models:** Entidades JPA y modelos externos
- **Caching Layer:** Redis para optimización

#### **Infrastructure Layer**
- **Security Framework:** Spring Security
- **Configuration Management:** Spring Boot Configuration
- **Monitoring & Logging:** Actuator, Micrometer, Logback

### 3.3 Consideraciones de Calidad

#### **Seguridad**
- **Autenticación:** Form-based con Spring Security
- **Autorización:** Role-based (`@PreAuthorize`)
- **Protección CSRF:** Habilitada para formularios
- **Encriptación:** BCrypt para passwords
- **Sesiones:** Timeout automático de 30 minutos

#### **Rendimiento**
- **Caching:** Multi-nivel (application + Redis)
- **Connection Pooling:** HikariCP optimizado
- **Lazy Loading:** Carga diferida de datos grandes
- **Pagination:** Resultados paginados
- **Índices:** Optimización de consultas DB

#### **Escalabilidad**
- **Stateless Design:** Servicios sin estado
- **Horizontal Scaling:** Múltiples instancias de app
- **Redis Clustering:** Cache distribuido
- **Database Partitioning:** Preparado para sharding
- **Circuit Breaker:** Resilencia ante fallos

---

## 🚀 4. Diagrama de Despliegue

### 4.1 Componentes Físicos del Sistema

#### **Load Balancer (Nginx)**
- **Hardware:** 2 vCPUs, 4 GB RAM, 20 GB SSD
- **Funciones:** SSL termination, rate limiting, routing
- **Configuración:** HTTPS/443, HTTP/80, health checks

#### **Application Servers (Cluster)**
- **Hardware:** 4 vCPUs, 8 GB RAM, 50 GB SSD cada uno
- **Software:** Ubuntu 22.04 LTS, Java 17, Spring Boot
- **Configuración:** JVM heap 2GB, connection pool 20
- **Escalabilidad:** Horizontal scaling ready

#### **Database Server (PostgreSQL)**
- **Hardware:** 8 vCPUs, 16 GB RAM, 500 GB SSD RAID 1
- **Software:** PostgreSQL 15.x
- **Configuración:** Max 200 connections, 2GB shared buffers
- **Backup:** Daily full backups, WAL archiving, 30 days retention

#### **Cache Server (Redis)**
- **Hardware:** 4 vCPUs, 8 GB RAM, 100 GB SSD
- **Software:** Redis 7.x
- **Configuración:** 4GB max memory, LRU eviction
- **Uso:** Sessions, API cache, rate limiting

#### **Monitoring Server**
- **Hardware:** 2 vCPUs, 4 GB RAM, 200 GB SSD
- **Software:** Grafana, Prometheus, ELK Stack
- **Funciones:** Metrics, logs, alerts, dashboards

### 4.2 Protocolos de Comunicación

#### **Cliente ↔ Load Balancer**
- **Protocolo:** HTTPS/443 (HTTP/2)
- **Seguridad:** TLS 1.3, HSTS headers
- **Rate Limiting:** 100 requests/min/IP

#### **Load Balancer ↔ App Servers**
- **Protocolo:** HTTP/1.1
- **Balancing:** Round-robin con health checks
- **Timeout:** 30 seconds

#### **App Servers ↔ Database**
- **Protocolo:** TCP/5432 (PostgreSQL wire protocol)
- **Connection:** JDBC con HikariCP pool
- **Seguridad:** SSL encryption

#### **App Servers ↔ External APIs**
- **Protocolo:** HTTPS/443 (REST)
- **Rate Limiting:** Per-provider limits
- **Timeout:** 30-60 seconds con retry

### 4.3 Configuración de Alta Disponibilidad

#### **Redundancia**
- **App Servers:** Mínimo 2 instancias activas
- **Database:** Master-slave replication
- **Cache:** Redis master-slave setup
- **Load Balancer:** Backup nginx instance

#### **Monitoring y Alerting**
- **Health Checks:** Endpoint `/actuator/health`
- **Metrics:** CPU, memory, response time, error rate
- **Alerts:** Email/SMS para incidentes críticos
- **Dashboards:** Grafana para visualización en tiempo real

#### **Disaster Recovery**
- **RTO (Recovery Time Objective):** 4 horas
- **RPO (Recovery Point Objective):** 1 hora
- **Backups:** Geográficamente distribuidos
- **Testing:** Quarterly DR drills

---

## 📊 5. Métricas y Consideraciones de Implementación

### 5.1 Métricas de Rendimiento

#### **Objetivos de SLA**
- **Disponibilidad:** 99.9% uptime
- **Tiempo de Respuesta:** < 2 segundos (95th percentile)
- **Throughput:** 1000 concurrent users
- **Error Rate:** < 0.1%

#### **Capacidad del Sistema**
- **Búsquedas por día:** 100,000+
- **Usuarios concurrentes:** 1,000+
- **Almacenamiento:** 500 GB database, 100 GB cache
- **API Calls:** Respeta límites de proveedores externos

### 5.2 Seguridad en Implementación

#### **Principios de Seguridad**
- **Defense in Depth:** Múltiples capas de seguridad
- **Least Privilege:** Mínimos permisos necesarios
- **Secure by Default:** Configuración segura por defecto
- **Regular Updates:** Actualizaciones de seguridad automáticas

#### **Cumplimiento y Auditoría**
- **OWASP Compliance:** Top 10 security risks addressed
- **Data Protection:** GDPR compliance for EU users
- **Audit Logging:** Comprehensive activity logging
- **Security Scanning:** Automated vulnerability assessment

### 5.3 Estrategia de Testing

#### **Tipos de Testing**
- **Unit Tests:** JUnit 5 + Mockito (>80% coverage)
- **Integration Tests:** Testcontainers para DB/Redis
- **API Tests:** REST Assured para endpoints
- **UI Tests:** Selenium para flujos críticos
- **Performance Tests:** JMeter para load testing

Este modelo de diseño e implementación proporciona una base sólida y escalable para el sistema Papelio, implementando patrones de diseño apropiados y considerando todos los aspectos de calidad, seguridad y rendimiento necesarios para un sistema académico robusto.
