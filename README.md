# ms-pedidos360-bff

Backend For Frontend de **Pedidos360** (DSY1107). Spring Boot 4.1.1 · Java 25 · puerto **8080**.
Único backend que ve el frontend; orquesta los microservicios (hoy: catálogo).

## Arquitectura
```
Angular (MSAL · Auth Code + PKCE)  ──Bearer JWT──►  BFF :8080
                                                   (firma, exp, issuer, audience, rol)
                                                     │  reenvía Authorization + X-Request-Id
                                                     ▼
                                                   Catalog :8081 (valida de nuevo) ─► PostgreSQL
```
Capas: `controller` → `service` → `client` (RestClient) · `config` (seguridad, JWT, CORS) · `exception` · `web` (X-Request-Id).

## Qué cubre de la evaluación
| Requisito | Implementación |
|---|---|
| BFF valida JWT igual que el API Manager | `JwtConfig`: firma (JWKS de Entra ID), `exp/nbf`, `issuer`, `audience` explícita |
| Solo permite consumir con token válido | `/api/**` exige `SCOPE_access_as_user`; públicos solo `/actuator/health` y Swagger |
| Autorización por rol | claim `roles` → `ROLE_Admin/Operador/Cliente`; con `ENFORCE_ROLES=true`, POST/PUT/DELETE exigen Admin u Operador |
| Códigos de error adecuados | 401 / 403 JSON `{timestamp,status,error,mensaje,path}`; 400, 404, 502, 503, 500 sin stacktrace |

## Endpoints
| Método | Ruta | Destino |
|---|---|---|
| GET | `/api/catalog/products` | catálogo |
| GET | `/api/catalog/products/{id}` | catálogo |
| POST | `/api/catalog/products` | catálogo (201) |
| GET | `/actuator/health`, `/swagger-ui.html`, `/v3/api-docs` | público |

Traducción de errores del catálogo: 400/401/403/404 se propagan · 5xx → **502** · caído/timeout → **503**.

## Variables de entorno
| Variable | Default |
|---|---|
| `ENTRA_ISSUER_URI` | `https://login.microsoftonline.com/0bfad962-b91d-465a-b769-8e71565efe7d/v2.0` |
| `ENTRA_API_CLIENT_ID` (audience) | `d80d5009-1b8a-49b2-a3da-08a8bbd00936` |
| `CATALOG_BASE_URL` | `http://localhost:8081` |
| `SERVICES_CONNECT_TIMEOUT` / `SERVICES_READ_TIMEOUT` | `2s` / `5s` |
| `ENFORCE_ROLES` | `false` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` |

## Ejecutar
```bash
cd ../ms-pedidos360-catalog && ./mvnw spring-boot:run   # 8081 (requiere PostgreSQL, ver su README)
cd ../ms-pedidos360-bff     && ./mvnw spring-boot:run   # 8080
./mvnw clean verify                                     # tests (no requieren catálogo ni Azure)
```

## Swagger (OpenAPI)
| | |
|---|---|
| URL | http://localhost:8080/swagger-ui.html |
| Spec JSON | `curl http://localhost:8080/v3/api-docs` |

Botón **Authorize** → pegar el `TOKEN` (sin `Bearer `) para probar los endpoints; sin token responden 401.

## Pruebas manuales (curl)

`TOKEN`: access token de Entra ID (scope `api://d80d5009-1b8a-49b2-a3da-08a8bbd00936/access_as_user`).
Iniciar sesión en el frontend → DevTools → Network → llamada a `/api/...` → header `Authorization` (lo que va tras `Bearer `).

### Local (`http://localhost:8080`)
```bash
BASE=http://localhost:8080
TOKEN="<access token>"

curl -i $BASE/actuator/health                                              # 200 público
curl -i $BASE/api/catalog/products                                         # 401 {"mensaje":"Token ausente, inválido o expirado",...}
curl -i -H "Authorization: Bearer abc.def.ghi" $BASE/api/catalog/products  # 401
curl -i -H "Authorization: Bearer $TOKEN" $BASE/api/catalog/products       # 200 lista JSON + header X-Request-Id
curl -i -H "Authorization: Bearer $TOKEN" $BASE/api/catalog/products/1     # 200
curl -i -H "Authorization: Bearer $TOKEN" $BASE/api/catalog/products/99999 # 404
curl -i -H "Authorization: Bearer $TOKEN" $BASE/api/catalog/products/abc   # 400 id inválido

curl -i -X POST $BASE/api/catalog/products \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Teclado","descripcion":"Mecanico USB","precio":49990,"stock":10}'  # 201 (repetir → 400 duplicado)

curl -i -X POST $BASE/api/catalog/products \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"","precio":-1,"stock":-5}'                                         # 400 con "detalles"

# Con el catálogo apagado:
curl -i -H "Authorization: Bearer $TOKEN" $BASE/api/catalog/products       # 503 "Servicio de catálogo no disponible"
```


## Agregar un microservicio
Propiedad `pedidos360.services.<ms>.base-url` → DTOs propios → `client/<Ms>Client` con `DownstreamRestClientFactory` (ya aporta timeouts, propagación de token/X-Request-Id y traducción de errores) → `service` + `controller` con el mismo path (`/api/<ms>/...`). Seguridad ya cubierta por `/api/**`.
