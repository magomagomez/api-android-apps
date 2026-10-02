# Movie Recommendation Engine

## 1. Objetivo del proyecto

Esta aplicación web recomienda películas de forma personalizada.

El sistema recibe un listado de películas proporcionado por el usuario y, a partir de diferentes fuentes de información cinematográfica, construye un perfil de gustos y genera un ranking personalizado.

El objetivo final es obtener un TOP 50 de películas que tengan la mayor probabilidad de gustar a ese usuario.

Cada recomendación debe incluir:

- Personal Match Score
- razones objetivas que justifican la recomendación
- explicación personalizada basada únicamente en datos reales

El sistema debe priorizar la compatibilidad con los gustos del usuario y no limitarse a recomendar las películas mejor valoradas globalmente.

---

## 2. Stack tecnológico

El backend utiliza:

- Java
- Spring Boot
- Maven
- Heroku

Utilizar Java 21 si es compatible con la configuración existente del proyecto.

Antes de introducir nuevas tecnologías o dependencias, comprobar si el proyecto ya tiene una solución adecuada.

Priorizar código sencillo, mantenible y fácil de probar.

No introducir dependencias innecesarias.

---

## 3. Regla fundamental: NO PERSISTENCIA

Este proyecto es completamente stateless respecto a los datos cinematográficos y personales.

NO utilizar:

- PostgreSQL
- MySQL
- MongoDB
- Redis como almacenamiento persistente
- JPA
- Hibernate
- Spring Data repositories
- bases de datos de cualquier tipo
- almacenamiento permanente de películas
- almacenamiento permanente de usuarios
- almacenamiento permanente de ratings
- almacenamiento permanente de listas
- almacenamiento permanente de perfiles
- almacenamiento permanente de resultados

Los datos solamente deben existir durante el procesamiento de una petición o job.

Cuando finaliza el procesamiento, los datos deben desaparecer.

No crear tablas, entidades JPA, repositories ni mecanismos equivalentes.

Si en el futuro se necesita procesamiento asíncrono, el estado puede mantenerse temporalmente en memoria mientras el job está ejecutándose, pero no debe persistirse.

La privacidad y el carácter stateless son requisitos arquitectónicos fundamentales y no deben modificarse salvo decisión explícita.

---

## 4. Arquitectura

Utilizar una arquitectura desacoplada basada en servicios y providers.

Estructura conceptual:

controller/
service/
provider/
model/
scoring/
ai/

Los nombres concretos pueden adaptarse a la estructura existente del proyecto.

Regla general:

Controller
→ Service
→ Provider
→ External API

Los Controllers NO deben llamar directamente a APIs externas.

Los Controllers NO deben contener lógica de negocio.

La lógica de negocio debe estar en Services.

El dominio no debe conocer detalles específicos de HTTP, HTML, JSON externo o APIs concretas.

---

## 5. Providers

Las fuentes externas deben estar desacopladas mediante interfaces.

Ejemplos:

- MovieDataProvider
- RatingProvider
- UserProfileProvider
- AwardsProvider
- FestivalProvider
- CriticProvider

No es necesario implementar todos estos providers inmediatamente.

Crear únicamente los que sean necesarios para la fase actual.

Los providers externos deben poder sustituirse y probarse mediante mocks.

La implementación de un provider concreto no debe contaminar el modelo de dominio.

Por ejemplo, el dominio no debería depender directamente de clases específicas de TMDB.

---

## 6. Fuentes externas

Siempre priorizar las fuentes en este orden:

1. API oficial
2. dataset oficial
3. exportación proporcionada por el usuario
4. scraping solamente cuando sea apropiado y esté permitido

No asumir que una web tiene una API pública disponible.

No diseñar el sistema dependiendo de scraping si existe una alternativa oficial o proporcionada por el usuario.

Las restricciones de uso de cada fuente deben respetarse.

---

## 7. Identificación de películas

La identificación inequívoca de una película es una fase fundamental.

No realizar simplemente búsquedas independientes del título en todas las fuentes.

Cuando sea posible utilizar:

- título
- año de estreno
- identificadores externos

para identificar la película.

Una película identificada debería tener preferentemente:

- TMDB ID
- IMDb ID
- título
- título original
- año
- director

Los identificadores comunes deben utilizarse posteriormente para cruzar información entre diferentes providers.

Una película ambigua NO debe marcarse como identificada automáticamente.

El sistema debe poder distinguir al menos entre:

- identified
- not found
- ambiguous

La identificación debe ser determinista y explicable.

---

## 8. Modelo de dominio

Los objetos principales previstos para el sistema completo incluyen:

- MovieIdentity
- Movie
- MovieRating
- Director
- Actor
- Genre
- Award
- Festival
- CriticReview
- MovieRelation
- UserMovie
- UserList
- UserProfile
- MovieAnalysis
- Recommendation

Estos modelos son conceptuales y no deben implementarse todos de golpe.

No crear modelos futuros innecesarios durante una fase que no los necesita.

Utilizar records cuando sean apropiados para objetos inmutables o DTOs.

Los objetos cinematográficos son temporales y nunca deben convertirse en entidades persistentes.

---

## 9. Grafo cinematográfico

El sistema debe poder representar relaciones entre películas y elementos cinematográficos.

Posibles relaciones:

- director
- actor
- género
- temática
- país
- década
- saga
- secuela
- precuela
- remake
- franquicia
- colaboradores
- películas similares

No es necesario implementar todo esto en la primera fase.

La arquitectura debe permitir añadir estas relaciones posteriormente sin tener que reescribir el núcleo.

---

## 10. Perfil cinéfilo del usuario

En fases posteriores el sistema analizará los datos cinematográficos proporcionados por el usuario.

Especialmente:

- películas vistas
- ratings
- listas
- directores
- actores
- géneros
- países
- décadas
- colaboradores
- relaciones entre películas

El sistema debe poder calcular afinidades como:

- directorAffinity
- genreAffinity
- actorAffinity
- countryAffinity
- decadeAffinity
- sequelAffinity
- franchiseAffinity
- similarMovieAffinity

No guardar permanentemente ningún dato del perfil.

---

## 11. Letterboxd

Letterboxd será una fuente futura de información del usuario.

Priorizar:

- datos/exportaciones proporcionados por el propio usuario
- APIs oficiales si están disponibles y autorizadas

No asumir que el scraping automatizado de Letterboxd está permitido.

No implementar Letterboxd durante la primera fase.

Cuando se implemente, el provider debe estar desacoplado del resto del sistema.

---

## 12. Ratings y fuentes de calidad

En fases posteriores se podrán incorporar:

- IMDb
- Rotten Tomatoes
- Metacritic
- FilmAffinity
- otras fuentes de crítica
- festivales
- premios

Los ratings deben conservar, cuando sea posible:

- fuente
- valor original
- escala original
- valor normalizado
- número de votos
- fecha
- confianza

Nunca mezclar directamente escalas diferentes.

Por ejemplo:

IMDb 8.0/10 y una puntuación 80/100 no deben tratarse como valores sin conocer su escala y semántica.

---

## 13. Premios, festivales y crítica

El sistema debe estar preparado para incorporar posteriormente:

- Oscars
- Cannes
- Venice
- otros festivales relevantes
- premios
- nominaciones
- selecciones
- recepción de festivales cuando exista información fiable
- crítica de publicaciones
- Rotten Tomatoes
- Metacritic

No implementar estas fuentes durante la primera vertical.

---

## 14. Recommendation Engine

El ranking final debe ser determinista, explicable y reproducible.

Separar siempre:

QUALITY SCORE

de

PERSONAL MATCH SCORE

Una película puede tener una calidad global muy alta y, aun así, no ser una buena recomendación para un usuario concreto.

El TOP 50 debe basarse principalmente en PERSONAL MATCH SCORE.

El LLM NO decide qué películas entran en el TOP 50.

El ranking debe ser calculado por código mediante señales y pesos configurables.

---

## 15. Señales de recomendación

El sistema debe poder utilizar señales como:

- directorAffinity
- genreAffinity
- actorAffinity
- countryAffinity
- decadeAffinity
- sequelAffinity
- franchiseAffinity
- similarMovieAffinity
- collaboratorAffinity
- criticalScore
- ratingScore
- awardsScore
- festivalScore
- dataConfidence

Ejemplo conceptual:

directorAffinity = 0.95
genreAffinity = 0.88
actorAffinity = 0.71
similarMovieAffinity = 0.93
criticalScore = 0.90
awardsScore = 0.85

Las señales deben estar normalizadas cuando sea apropiado.

---

## 16. Pesos iniciales

Los pesos iniciales previstos para el Personal Match Score son:

- 30% compatibilidad con gustos del usuario
- 20% valoración crítica
- 15% valoración de usuarios
- 15% premios y festivales
- 10% similitud con películas que gustan al usuario
- 10% calidad/confianza de los datos

Estos pesos son provisionales y deben ser configurables.

No hardcodear la lógica de forma que sea imposible cambiar los pesos posteriormente.

La fórmula definitiva se diseñará cuando exista suficiente información real para construir el sistema de scoring.

No implementar el scoring definitivo durante la primera vertical.

---

## 17. Transparencia de las recomendaciones

Cada recomendación debe conservar temporalmente las señales utilizadas para calcularla.

También debe conservar razones estructuradas.

Ejemplo:

{
  "directorAffinity": 0.95,
  "genreAffinity": 0.88,
  "actorAffinity": 0.71,
  "similarMovieAffinity": 0.93,
  "criticalScore": 0.90,
  "awardsScore": 0.85,
  "reasons": [
    "mismo director que The Handmaiden",
    "mismo director que Oldboy",
    "el usuario ha valorado muy positivamente películas del director",
    "género con alta afinidad",
    "excelente recepción crítica"
  ]
}

Las razones deben derivarse de datos reales.

Nunca inventar una relación para justificar una recomendación.

---

## 18. LLM

El LLM solamente se utilizará para generar explicaciones en lenguaje natural.

El LLM NO debe:

- decidir el TOP 50
- calcular arbitrariamente scores
- inventar ratings
- inventar premios
- inventar relaciones entre películas
- inventar información del usuario

El flujo debe ser:

datos externos
→ análisis
→ señales
→ scoring determinista
→ ranking
→ razones estructuradas
→ LLM
→ explicación

El LLM recibirá datos ya calculados y deberá explicar únicamente esos datos.

---

## 19. Primera vertical funcional

La primera funcionalidad que debe implementarse es únicamente:

CSV
→ Movie Identification
→ TMDB
→ objetos temporales
→ JSON

Endpoint previsto:

POST /api/movies/import

La respuesta debe incluir como mínimo:

- total
- identified
- notFound
- ambiguous
- movies

Ejemplo conceptual:

{
  "total": 5,
  "identified": 4,
  "notFound": 0,
  "ambiguous": 1,
  "movies": [...]
}

---

## 20. TMDB

TMDB será el primer provider real.

La API key debe obtenerse mediante variable de entorno:

TMDB_API_KEY

Nunca:

- escribir la API key en código
- subir secretos al repositorio
- incluir secretos en tests
- incluir secretos en respuestas JSON
- incluir secretos en logs

La búsqueda de TMDB debe utilizar título y año cuando el año esté disponible.

Después de identificar una película, obtener los datos necesarios para construir el objeto Movie temporal.

Como mínimo, preparar:

- TMDB ID
- IMDb ID
- title
- originalTitle
- releaseDate
- runtime
- overview
- genres
- director
- actors
- countries
- poster

No todos los campos tienen que estar disponibles para todas las películas.

---

## 21. CSV

La primera entrada del sistema será un CSV con títulos de películas.

El parser debe ser robusto ante:

- cabecera
- líneas vacías
- espacios innecesarios
- títulos con caracteres especiales

Si el formato del CSV necesita una convención concreta, documentarla.

No almacenar el CSV.

Procesarlo únicamente durante la petición/job.

---

## 22. Procesamiento

No diseñar una única petición gigantesca que obligue a realizar cientos o miles de llamadas externas sin control.

Para la primera vertical se puede utilizar procesamiento síncrono si el tamaño del CSV es razonable.

La arquitectura debe permitir evolucionar posteriormente a jobs asíncronos.

Posible diseño futuro:

POST /api/movies/import
→ crea job temporal
→ responde 202

GET /api/movies/{jobId}
→ devuelve progreso

No implementar este sistema asíncrono todavía salvo que sea necesario para el código existente.

---

## 23. Tests

Cada pieza importante debe poder probarse sin depender de servicios externos reales.

Los providers externos deben mockearse en tests.

Como mínimo, la primera vertical debe cubrir:

- CSV válido
- CSV vacío
- líneas vacías
- película identificada
- película no encontrada
- película ambigua
- uso del año
- transformación de respuesta TMDB al modelo interno
- errores del provider

Los tests no deben requerir una API key real.

---

## 24. Desarrollo incremental

Este proyecto debe desarrollarse por fases.

NO intentar implementar todo el sistema de una sola vez.

Cada fase debe:

1. implementar una funcionalidad concreta
2. mantener la arquitectura limpia
3. añadir tests
4. ejecutar los tests
5. ejecutar el build
6. corregir errores
7. dejar el proyecto funcionando

No implementar funcionalidades futuras simplemente porque están descritas en este documento.

Este archivo describe la arquitectura y las decisiones permanentes del proyecto.

Las instrucciones específicas de cada fase tendrán prioridad para decidir qué funcionalidad implementar ahora, siempre que no contradigan estas reglas fundamentales.

---

## 25. Regla para cambios

Antes de introducir un cambio arquitectónico importante, comprobar si ya existe una decisión en este archivo.

No modificar una decisión fundamental silenciosamente.

Especialmente no modificar sin autorización:

- ausencia de persistencia
- arquitectura stateless
- separación por providers
- scoring determinista
- separación Quality Score / Personal Match Score
- papel limitado del LLM
- privacidad de los datos del usuario

---

## 26. Principio general

Construir primero un sistema pequeño que funcione correctamente.

Después ampliar progresivamente:

CSV
→ identificación
→ TMDB
→ enriquecimiento
→ otras fuentes
→ perfil de usuario
→ relaciones cinematográficas
→ scoring
→ ranking
→ explicaciones LLM

Cada etapa debe funcionar antes de añadir la siguiente.