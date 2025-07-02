# Papelio Domain Model Documentation

## Ejercicio 1: Desarrollo del Modelo de Dominio y Diagrama de Clases Conceptuales

### 1. Diagrama de Clases Conceptuales

**Ubicación:** `domain-model-conceptual-classes.puml`

El diagrama UML representa todas las clases conceptuales del sistema Papelio, identificando:
- **Entidades principales:** User, Article, Author, SearchHistory, ArticleViewHistory, ArticleFavorite
- **Objetos de valor:** ExternalIds, SearchRequest, SearchResponse
- **Modelos externos:** SemanticScholarPaper, SemanticScholarResponse
- **Relaciones:** Asociación, composición y agregación entre entidades
- **Cardinalidades:** Especificadas en todas las relaciones

### 2. Modelo de Dominio

#### 2.1 Reglas de Negocio por Entidad

##### **User (Usuario)**
- **Propósito:** Representa usuarios del sistema (estudiantes, investigadores, administradores)
- **Reglas de negocio:**
  - Email debe ser único en el sistema
  - Contraseña debe ser encriptada con BCrypt
  - Roles soportados: USER, ADMIN
  - Nombre obligatorio con mínimo 2 caracteres
  - Email debe tener formato válido

##### **Article (Artículo)**
- **Propósito:** Representa artículos académicos obtenidos de APIs externas
- **Reglas de negocio:**
  - Datos no se persisten en BD local (obtenidos dinámicamente)
  - ID único proviene de Semantic Scholar API
  - Debe tener título y al menos un autor
  - Citation count puede ser nulo
  - URL debe ser válida si está presente

##### **SearchHistory (Historial de Búsquedas)**
- **Propósito:** Registra búsquedas realizadas por usuarios autenticados
- **Reglas de negocio:**
  - Solo se registran búsquedas de usuarios autenticados
  - Query no puede estar vacío
  - Fecha de búsqueda se asigna automáticamente
  - Se usa para análisis de tendencias y recomendaciones

##### **ArticleViewHistory (Historial de Visualizaciones)**
- **Propósito:** Rastrea artículos visualizados por usuarios
- **Reglas de negocio:**
  - Se permite múltiples vistas del mismo artículo
  - Fecha de vista se asigna automáticamente
  - Se usa para motor de recomendaciones
  - Título se almacena para rendimiento

##### **ArticleFavorite (Artículos Favoritos)**
- **Propósito:** Gestiona lista personal de artículos favoritos
- **Reglas de negocio:**
  - Un usuario no puede marcar el mismo artículo como favorito dos veces
  - Restricción única en (userEmail, articleId)
  - Fecha de favorito se asigna automáticamente
  - Los favoritos persisten entre sesiones

#### 2.2 Invariantes del Sistema

1. **Integridad referencial:** Todo registro en search_history, article_view_history y article_favorites debe referenciar un usuario válido
2. **Unicidad de email:** No pueden existir dos usuarios con el mismo email
3. **Unicidad de favoritos:** Un usuario no puede tener el mismo artículo marcado como favorito múltiples veces
4. **Datos obligatorios:** Todos los campos marcados como NOT NULL deben tener valores
5. **Consistencia temporal:** Las fechas no pueden ser futuras

#### 2.3 Restricciones y Condiciones

- **Autenticación:** Solo usuarios autenticados pueden realizar búsquedas guardadas, ver historial y gestionar favoritos
- **Autorización:** Solo administradores pueden acceder a funciones de gestión de usuarios
- **Validación:** Todas las entradas del usuario deben ser validadas antes del procesamiento
- **Cache:** Resultados de búsqueda se almacenan en caché por 30 minutos
- **Rate limiting:** Máximo 100 búsquedas por usuario por hora

### 3. Diccionario de Datos

#### 3.1 Entidad: User (usuarios)
- **Descripción:** Tabla principal de usuarios del sistema
- **Propósito:** Gestionar autenticación y autorización

| Atributo | Tipo de Dato | Restricciones | Descripción |
|----------|--------------|---------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Identificador único del usuario |
| email | VARCHAR(255) | UNIQUE, NOT NULL | Email del usuario (login) |
| password | VARCHAR(255) | NOT NULL | Contraseña encriptada con BCrypt |
| name | VARCHAR(255) | NOT NULL | Nombre completo del usuario |
| role | VARCHAR(50) | DEFAULT 'USER' | Rol del usuario (USER, ADMIN) |

**Relaciones:**
- Uno a muchos con search_history
- Uno a muchos con article_view_history  
- Uno a muchos con article_favorites

#### 3.2 Entidad: SearchHistory (search_history)
- **Descripción:** Registro de búsquedas realizadas por usuarios
- **Propósito:** Habilitar historial personal y análisis de tendencias

| Atributo | Tipo de Dato | Restricciones | Descripción |
|----------|--------------|---------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Identificador único del registro |
| user_email | VARCHAR(255) | FK, NOT NULL | Email del usuario que realizó la búsqueda |
| search_query | VARCHAR(1000) | NOT NULL | Término de búsqueda |
| search_date | TIMESTAMP | NOT NULL, DEFAULT NOW() | Fecha y hora de la búsqueda |

**Relaciones:**
- Muchos a uno con User (user_email -> usuarios.email)

#### 3.3 Entidad: ArticleViewHistory (article_view_history)
- **Descripción:** Registro de artículos visualizados por usuarios
- **Propósito:** Motor de recomendaciones y estadísticas de uso

| Atributo | Tipo de Dato | Restricciones | Descripción |
|----------|--------------|---------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Identificador único del registro |
| user_email | VARCHAR(255) | FK, NOT NULL | Email del usuario |
| article_id | VARCHAR(255) | NOT NULL | ID del artículo (de API externa) |
| title | VARCHAR(1000) | NOT NULL | Título del artículo (cache) |
| view_date | TIMESTAMP | NOT NULL, DEFAULT NOW() | Fecha y hora de visualización |

**Relaciones:**
- Muchos a uno con User (user_email -> usuarios.email)
- Referencia virtual a Article (article_id)

#### 3.4 Entidad: ArticleFavorite (article_favorites)
- **Descripción:** Lista de artículos favoritos por usuario
- **Propósito:** Gestión de colección personal de artículos

| Atributo | Tipo de Dato | Restricciones | Descripción |
|----------|--------------|---------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Identificador único del registro |
| user_email | VARCHAR(255) | FK, NOT NULL | Email del usuario |
| article_id | VARCHAR(255) | NOT NULL | ID del artículo (de API externa) |
| title | VARCHAR(1000) | NOT NULL | Título del artículo (cache) |
| favorite_date | TIMESTAMP | NOT NULL, DEFAULT NOW() | Fecha cuando se marcó como favorito |

**Restricciones especiales:**
- UNIQUE(user_email, article_id) - Previene duplicados

**Relaciones:**
- Muchos a uno con User (user_email -> usuarios.email)
- Referencia virtual a Article (article_id)

#### 3.5 Modelo Externo: Article (No persistido)
- **Descripción:** Representa artículos académicos de APIs externas
- **Propósito:** DTO para transferencia de datos de artículos

| Atributo | Tipo de Dato | Origen | Descripción |
|----------|--------------|--------|-------------|
| id | String | API | ID único del artículo |
| title | String | API | Título del artículo |
| abstract | String | API | Resumen del artículo |
| doi | String | API | Digital Object Identifier |
| authors | List\<String\> | API | Lista de autores |
| journal | String | API | Revista de publicación |
| publicationDate | LocalDate | API | Fecha de publicación |
| documentType | String | API | Tipo de documento |
| language | String | API | Idioma del artículo |
| citationCount | Integer | API | Número de citas |
| url | String | API | URL del artículo |

### 4. Diagrama Entidad-Relación

**Ubicación:** `entity-relationship-diagram.puml`

El diagrama E-R muestra:
- **Tablas principales:** usuarios, search_history, article_view_history, article_favorites
- **Claves primarias:** Todas las tablas tienen ID auto-incremental
- **Claves foráneas:** user_email referencia usuarios.email
- **Índices:** Optimizados para consultas frecuentes
- **Restricciones:** UNIQUE, NOT NULL, CHECK constraints

#### 4.1 Notas Importantes del Diseño

1. **Artículos no persistidos:** Los artículos no se almacenan en la base de datos local, se obtienen dinámicamente de APIs externas (Semantic Scholar, CrossRef)

2. **Referencias por email:** Se usa email como clave foránea en lugar de ID numérico para simplificar consultas y mantener consistencia con Spring Security

3. **Desnormalización controlada:** Se almacena el título del artículo en las tablas de historial y favoritos para mejorar el rendimiento de consultas

4. **Índices estratégicos:** Se crean índices en columnas frecuentemente consultadas (user_email, dates, article_id)

5. **Escalabilidad:** El diseño permite particionar por fecha o usuario para grandes volúmenes de datos

Esta documentación completa cubre todos los aspectos solicitados en el Ejercicio 1, proporcionando una base sólida para el desarrollo y mantenimiento del sistema Papelio.
