# ms-pedidos360-bff

Backend For Frontend de **Pedidos360** (DSY1107 – Desarrollo Cloud Native I). Spring Boot 4.1.1, Java 25, puerto **8080**.
Fase actual: dominio **Catalog**.

## Arquitectura

```
 Angular (MSAL, Entra ID, Auth Code + PKCE)
   scope api://d80d5009-1b8a-49b2-a3da-08a8bbd00936/access_as_user
        │  Authorization: Bearer <JWT>
        ▼
 AWS API Gateway  ── valida JWT ──►  ms-pedidos360-bff :8080   (valida JWT otra vez)
                                        │  controller → service → client (RestClient)
                                        │  propaga Authorization + X-Request-Id
                                        ▼
                                   ms-pedidos360-catalog :8081   (valida JWT otra vez)
                                   (futuros: pedidos, clientes, ...)
```

Capas: `controller` (contrato hacia el front, mismos paths/JSON que el microservicio) → `service` (composición futura)
→ `client` (RestClient + traducción de errores). `config` (seguridad, CORS, JWT), `exception`, `web` (filtro X-Request-Id).

### Seguridad
- JWT validado en el BFF: firma (JWKS del issuer), `exp`/`nbf`, `issuer` y `audience` (`JwtConfig`).
- `/api/**` exige authority `SCOPE_access_as_user`. `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**` son públicos.
- Claim `roles` → `ROLE_<rol>` (Admin, Operador, Cliente). Con `ENFORCE_ROLES=true`, POST/PUT/DELETE requieren Admin u Operador;
  GET solo requiere scope. Por defecto `false` (los App roles pueden no existir aún en Azure).
- Errores: 401 (sin token/inválido/expirado), 403 (sin scope/rol), en JSON `{timestamp, status, error, mensaje, path}`.
- El mismo token se reenvía al microservicio, que lo valida por su cuenta.

### Errores hacia el front
| Origen | Respuesta |
|---|---|
| Validación local (`@Valid`), body ilegible, id inválido | 400 (`detalles` en validación) |
| Catálogo 404 | 404 |
| Catálogo 400 | 400 con sus `detalles` |
| Catálogo 401 / 403 | 401 / 403 |
| Catálogo 5xx o respuesta ilegible | 502 "Servicio de catálogo no disponible" |
| Catálogo caído / timeout | 503 "Servicio de catálogo no disponible" |
| Cualquier otro | 500 "Error interno del servidor" (sin stacktrace) |

Cada petición lleva `X-Request-Id` (se acepta el entrante o se genera), está en el log (`%X{requestId}`), se reenvía al microservicio y vuelve en la respuesta.

## Variables de entorno
| Variable | Default | Uso |
|---|---|---|
| `ENTRA_ISSUER_URI` | `https://login.microsoftonline.com/0bfad962-b91d-465a-b769-8e71565efe7d/v2.0` | issuer del JWT |
| `ENTRA_API_CLIENT_ID` | `d80d5009-1b8a-49b2-a3da-08a8bbd00936` | audience esperada |
| `CATALOG_BASE_URL` | `http://localhost:8081` | base-url del catálogo |
| `SERVICES_CONNECT_TIMEOUT` | `2s` | timeout de conexión a microservicios |
| `SERVICES_READ_TIMEOUT` | `5s` | timeout de lectura |
| `ENFORCE_ROLES` | `false` | exigir Admin/Operador en escrituras |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | orígenes CORS (lista separada por comas, sin wildcard) |

## Levantar junto al catálogo
```bash
# 1) Catálogo (puerto 8081; requiere PostgreSQL, ver su README)
cd ../ms-pedidos360-catalog && ./mvnw spring-boot:run
# 2) BFF (puerto 8080)
cd ../ms-pedidos360-bff && ./mvnw spring-boot:run
# Angular: http://localhost:4200 apunta a http://localhost:8080/api/*
```
Swagger: http://localhost:8080/swagger-ui.html · Tests: `./mvnw clean verify` (no requieren catálogo ni Azure).

## Ejemplos curl
```bash
# Sin token -> 401
curl -i http://localhost:8080/api/catalog/products
# {"timestamp":"...","status":401,"error":"Unauthorized","mensaje":"Token ausente, inválido o expirado","path":"/api/catalog/products"}

# Con token de Entra ID (scope access_as_user) -> 200
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/catalog/products
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/catalog/products/1
curl -i -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"nombre":"Teclado","descripcion":"Mecánico","precio":49990,"stock":10}' \
     http://localhost:8080/api/catalog/products     # 201
```
Obtener `$TOKEN` (usuario con acceso a la app): por ejemplo `az account get-access-token --scope api://d80d5009-1b8a-49b2-a3da-08a8bbd00936/access_as_user --query accessToken -o tsv` (puede requerir que Azure CLI esté preautorizada en la app), o copiarlo desde las herramientas de red del navegador.

## Agregar un microservicio nuevo (ej. pedidos)
No se toca seguridad ni manejo de errores.
1. **Propiedad**: en `application.yaml` agregar `pedidos360.services.pedidos.base-url: ${PEDIDOS_BASE_URL:http://localhost:8082}`.
2. **DTOs** propios en `dto/` (no compartir clases con el microservicio).
3. **Client** `client/PedidosClient` (copiar `CatalogClient`): inyectar `DownstreamRestClientFactory` y la base-url, `factory.create("pedidos", baseUrl)`. El factory ya aporta timeouts, propagación de `Authorization`/`X-Request-Id` y traducción 400/401/403/5xx/timeout. Para un 404 de dominio, usar `.onStatus(...)` como en `buscarPorId`, con una excepción nueva mapeada a 404 en `GlobalExceptionHandler` (un método).
4. **Service** `service/PedidosService` y **controller** `controller/PedidosController` con los mismos paths que el microservicio (`/api/pedidos/...`).
5. Cubierto por la regla `/api/**` (scope y roles por método). Agregar tests siguiendo `CatalogEndpointsTest` (`FakeCatalogServer` sirve como plantilla de servidor simulado).
