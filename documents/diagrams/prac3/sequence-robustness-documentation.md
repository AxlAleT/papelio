# Ejercicio 2: Diagramas de Secuencia y Robustez - Documentación

## Resumen de Diagramas Creados

Este documento complementa los diagramas PlantUML creados para el **Ejercicio 2: Desarrollo de Diagramas de Secuencia y Robustez** del sistema Papelio.

### 📋 Archivos de Diagramas Creados

#### 1. **Diagramas de Secuencia**
- **`sequence-basic-article-search.puml`** - Diagrama detallado del caso de uso principal
- **`sequence-additional-use-cases.puml`** - Diagramas simplificados para otros casos de uso

#### 2. **Diagramas de Robustez**
- **`robustness-basic-article-search.puml`** - Robustez del caso de uso principal
- **`robustness-additional-use-cases.puml`** - Robustez para casos de uso adicionales

#### 3. **Modelo de Interfaz y Navegación**
- **`interface-navigation-model.puml`** - Diseño de interfaces y navegación
- **`role-based-navigation-flow.puml`** - Flujos de navegación por roles

---

## 🎯 1. Diagramas de Secuencia

### 1.1 Caso de Uso Principal: Basic Article Search

**Archivo:** `sequence-basic-article-search.puml`

**Actores identificados:**
- User (Student/Researcher)
- Controllers (SearchController, SearchRestController)
- Services (SemanticScholarService, SearchHistoryService, ArticleViewHistoryService)
- External API (Semantic Scholar)
- Database y Cache

**Flujos principales cubiertos:**
1. **Search Request Flow** - Búsqueda básica de artículos
2. **Article View Details Flow** - Visualización de detalles de artículo
3. **Caching Strategy** - Manejo de caché para optimización
4. **Error Handling** - Gestión de errores de API externa
5. **History Tracking** - Registro de búsquedas y visualizaciones

**Mensajes y secuencia temporal:**
- Validación de entrada con `@Valid`
- Verificación de caché antes de llamadas API
- Mapeo de datos con `SemanticScholarMapper`
- Guardado asíncrono de historial
- Manejo de respuestas vacías y errores

### 1.2 Casos de Uso Adicionales

**Archivo:** `sequence-additional-use-cases.puml`

**Casos de uso cubiertos:**
1. **User Authentication** - Autenticación con Spring Security
2. **Favorites Management** - Gestión de artículos favoritos
3. **Recommendations Engine** - Motor de recomendaciones
4. **Search History Tracking** - Seguimiento de historial
5. **Admin User Management** - Gestión administrativa de usuarios

---

## 🏗️ 2. Diagramas de Robustez

### 2.1 Clasificación de Objetos

#### **Objetos de Interfaz (Boundary Objects)**
- **Web UI Components:** Search Forms, Results Pages, Modals, Dashboards
- **REST API Endpoints:** JSON interfaces para AJAX calls
- **External APIs:** Semantic Scholar API integration

#### **Objetos de Control (Control Objects)**
- **MVC Controllers:** SearchController, UserController, AdminController
- **REST Controllers:** SearchRestController, UserRestController
- **Business Services:** SemanticScholarService, UserService, SearchHistoryService
- **Security Components:** AuthenticationManager, UserDetailsService
- **Utility Services:** CacheManager, ValidationService, Mappers

#### **Objetos de Entidad (Entity Objects)**
- **Domain Entities:** User, SearchHistory, ArticleFavorite, ArticleViewHistory
- **Data Transfer Objects:** SearchRequestDTO, SearchResponseDTO, ArticleDTO
- **External Models:** SemanticScholarPaper, SemanticScholarResponse
- **Data Stores:** Database, Cache

### 2.2 Conexiones entre Objetos

**Reglas establecidas:**
- **Interface → Control:** Solo objetos de interfaz pueden iniciar interacciones con control
- **Control → Entity:** Objetos de control manipulan entidades y DTOs
- **Control → Control:** Servicios pueden colaborar entre sí
- **Entity → Entity:** Relaciones de dominio permitidas
- **No Interface → Entity:** Interfaz no accede directamente a entidades

---

## 🖥️ 3. Modelo de Interfaz y Navegación

### 3.1 Componentes de Interfaz Identificados

#### **Layout Components**
- **Header Navigation:** Logo, menú principal, perfil de usuario
- **Search Interface:** Formularios, filtros, resultados, paginación
- **Article Components:** Cards, modales, botones de favoritos
- **User Dashboard:** Estadísticas, widgets, paneles de actividad
- **Admin Panel:** Gestión de usuarios, analytics, configuración

#### **Responsive Design**
- **Mobile:** Layout colapsable, navegación táctil
- **Tablet:** Contenido lado a lado, botones touch-friendly
- **Desktop:** Layout completo con todos los paneles visibles
- **Accessibility:** ARIA labels, navegación por teclado

### 3.2 Flujos de Navegación

#### **Por Rol de Usuario:**

**Anonymous Users:**
- Landing Page → Search → Results → Login/Register

**Authenticated Users:**
- Dashboard → Enhanced Search → Results with Actions → Article Details
- Dashboard → Favorites → Search History → Profile Settings

**Administrators:**
- Admin Dashboard → User Management → System Analytics → Audit Logs

#### **Transiciones de Estado:**
- **Authentication Success:** Login → User/Admin Dashboard
- **Session Timeout:** Any Page → Login
- **Authorization Failure:** Protected Page → Access Denied
- **Logout:** Any Authenticated Page → Landing Page

---

## 🔒 4. Aspectos de Seguridad en los Diagramas

### 4.1 Controles de Acceso
- **@PreAuthorize("hasRole('ADMIN')")** para funciones administrativas
- **SecurityContextHolder** para obtener usuario actual
- **CSRF Protection** en formularios y APIs
- **Session Management** con timeout automático

### 4.2 Validación y Sanitización
- **@Valid** para validación de DTOs
- **Spring Validation** para reglas de negocio
- **Input Sanitization** antes del procesamiento
- **Rate Limiting** para prevenir abuso

---

## ⚡ 5. Patrones de Diseño Aplicados

### 5.1 Patrones Arquitectónicos
- **MVC Pattern** - Separación de responsabilidades
- **Service Layer Pattern** - Lógica de negocio encapsulada
- **Repository Pattern** - Abstracción de acceso a datos
- **DTO Pattern** - Transferencia segura de datos

### 5.2 Patrones de Integración
- **Cache-Aside Pattern** - Gestión de caché
- **Circuit Breaker** - Resilencia ante fallos de API
- **Retry Pattern** - Reintentos automáticos
- **Facade Pattern** - Simplificación de APIs externas

---

## 📊 6. Métricas y Consideraciones de Rendimiento

### 6.1 Optimizaciones Identificadas
- **Caching Strategy:** 30 minutos para búsquedas, 1 hora para artículos
- **Pagination:** Límite de 100 resultados por página
- **Lazy Loading:** Carga diferida de contenido relacionado
- **Connection Pooling:** Gestión eficiente de conexiones DB

### 6.2 Escalabilidad
- **Stateless Services:** Para escalabilidad horizontal
- **Database Indexing:** Índices en columnas de búsqueda frecuente
- **API Rate Limiting:** 100 requests/hour por usuario
- **Session Management:** Redis para sesiones distribuidas

---

## 🧪 7. Consideraciones de Testing

### 7.1 Puntos de Testing Identificados
- **Unit Tests:** Para servicios y mappers
- **Integration Tests:** Para flujos completos de casos de uso
- **API Tests:** Para endpoints REST
- **UI Tests:** Para flujos de navegación

### 7.2 Mocking Strategies
- **External API Mocking:** Para Semantic Scholar API
- **Database Mocking:** Para tests unitarios
- **Security Context Mocking:** Para tests de autorización

---

Este conjunto de diagramas proporciona una visión completa de la arquitectura del sistema Papelio desde la perspectiva de interacciones, robustez y navegación, cumpliendo con todos los requisitos del Ejercicio 2.
