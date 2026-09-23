# ms-pedidos360-bff

Backend For Frontend de **Pedidos360** (DSY1107). Spring Boot 4.1.1 · Java 25 · puerto **8080**.
Único backend que ve el frontend (a través del API Gateway); orquesta los microservicios (hoy: catálogo).

## Arquitectura
```
Angular (MSAL · Auth Code + PKCE)  ──Bearer JWT──►  AWS API Gateway (HTTP API, JWT Authorizer, CORS)
                                                          │  capa 1: valida issuer + audience
                                                          │  HTTPS + X-Origin-Verify (secreto)
                                                          ▼
                                     EC2 ─ nginx :443 /bff/api/* ─► BFF 127.0.0.1:8080
                                           (capa 2: origen Gateway, firma, exp, issuer, audience, rol)
                                             │  reenvía Authorization + X-Request-Id
                                             ▼
                                           Catalog :8081 (capa 3) ─► PostgreSQL (Docker)
```
Capas: `controller` → `service` → `client` (RestClient) · `config` (seguridad, JWT, CORS) · `exception` · `web` (X-Request-Id).

## Qué cubre de la evaluación
| Requisito | Implementación |
|---|---|
| BFF valida JWT igual que el API Manager | `JwtConfig`: firma (JWKS de Entra ID), `exp/nbf`, `issuer`, `audience` explícita |
| Solo accesible vía API Gateway | `GatewayOriginFilter`: `/api/**` exige header `X-Origin-Verify` = `GATEWAY_SECRET` (403 si falta); BFF escucha solo en `127.0.0.1`, puerto 8080 cerrado |
| Solo permite consumir con token válido | `/api/**` exige `SCOPE_access_as_user`; públicos solo `/actuator/health` y Swagger |
| Autorización por rol | claim `roles` → `ROLE_Admin/Operador/Cliente`; con `ENFORCE_ROLES=true`, POST/PUT/DELETE exigen Admin u Operador |
| Códigos de error adecuados | 401 / 403 JSON `{timestamp,status,error,mensaje,path}`; 400, 404, 502, 503, 500 sin stacktrace |
| API Gateway como intermediario | HTTP API `u8thxu2opa`, rutas GET/POST `/{proxy+}` → EC2:8080, JWT Authorizer (Entra ID), CORS solo para el origen del frontend |
| Despliegue en la nube | GitHub Actions: `verify` → SCP del jar → `systemctl restart pedidos360-bff` en EC2 |

## Endpoints
| Método | Ruta | Destino |
|---|---|---|
| GET | `/api/catalog/products` | catálogo |
| GET | `/api/catalog/products/{id}` | catálogo |
| POST | `/api/catalog/products` | catálogo (201) |
| GET | `/actuator/health`, `/swagger-ui.html`, `/v3/api-docs` | público en local; en AWS solo por túnel SSH |

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
| `GATEWAY_SECRET` | vacío = no se exige (local). En AWS: el mismo valor que agrega el Gateway |
| `SERVER_ADDRESS` | todas las interfaces. En AWS: `127.0.0.1` |

## Ejecutar
```bash
cd ../ms-pedidos360-catalog && ./mvnw spring-boot:run   # 8081 (requiere PostgreSQL, ver su README)
cd ../ms-pedidos360-bff     && ./mvnw spring-boot:run   # 8080
./mvnw clean verify                                     # tests (no requieren catálogo ni Azure)
```

## Swagger (OpenAPI)
| | Local | AWS |
|---|---|---|
| URL | http://localhost:8080/swagger-ui.html | no es público (8080 cerrado, nginx solo expone `/bff/api/*`); usar túnel SSH ↓ |
| Comando | `open http://localhost:8080/swagger-ui.html` | `ssh -i <llave>.pem -N -L 8080:localhost:8080 ec2-user@52.71.122.5` y luego `open http://localhost:8080/swagger-ui.html` |
| Spec JSON | `curl http://localhost:8080/v3/api-docs` | ídem, con el túnel abierto |

Botón **Authorize** → pegar el `TOKEN` (sin `Bearer `) para probar los endpoints; sin token responden 401. En AWS, los `/api/**` desde Swagger dan 403 (no llevan el header del Gateway): usarlo para ver el contrato.

## Pruebas manuales (curl)

`TOKEN`: access token de Entra ID (scope `api://d80d5009-1b8a-49b2-a3da-08a8bbd00936/access_as_user`).
Iniciar sesión en el frontend → DevTools → Network → llamada a `/api/...` → header `Authorization` (lo que va tras `Bearer `). **El mismo token sirve para local y AWS.**

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

### AWS (vía API Gateway)
Son **distintas** a las locales: cambia la URL, el rechazo lo hace el Gateway (el request no llega al BFF), `/actuator/health` no está publicado sin token, se agrega la prueba de CORS y el BFF no es alcanzable sin pasar por el Gateway (8080 y 8081 cerrados).

```bash
GW=https://u8thxu2opa.execute-api.us-east-1.amazonaws.com
FRONT=https://52-71-122-5.sslip.io
TOKEN="<access token>"

# Capa 1: el Gateway rechaza sin llegar al backend → 401 {"message":"Unauthorized"} + header apigw-requestid
curl -i $GW/api/catalog/products
curl -i -H "Authorization: Bearer abc.def.ghi" $GW/api/catalog/products   # 401, www-authenticate: error="invalid_token"

# Token válido: Gateway → BFF → Catalog → PostgreSQL
curl -i -H "Authorization: Bearer $TOKEN" $GW/api/catalog/products        # 200 lista JSON (+ X-Request-Id del BFF)
curl -i -H "Authorization: Bearer $TOKEN" $GW/api/catalog/products/1      # 200
curl -i -H "Authorization: Bearer $TOKEN" $GW/api/catalog/products/99999  # 404 (error JSON del BFF)
curl -i -X POST $GW/api/catalog/products \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Mouse","descripcion":"Inalambrico","precio":15990,"stock":20}'  # 201

# CORS: origen del frontend permitido / origen ajeno sin Access-Control-Allow-Origin
curl -i -X OPTIONS $GW/api/catalog/products -H "Origin: $FRONT" \
  -H "Access-Control-Request-Method: GET" -H "Access-Control-Request-Headers: authorization"   # 204 + access-control-allow-origin: $FRONT
curl -i -X OPTIONS $GW/api/catalog/products -H "Origin: https://evil.example.com" \
  -H "Access-Control-Request-Method: GET"                                                       # 204 sin cabeceras CORS → navegador bloquea
```
Intentos de saltarse el Gateway:
```bash
curl -i -m 5 http://52.71.122.5:8080/actuator/health                                    # timeout: puerto cerrado
curl -i -H "Authorization: Bearer $TOKEN" https://52-71-122-5.sslip.io/bff/api/catalog/products
# 403 {"mensaje":"Acceso permitido solo a través del API Gateway"} → ni con token válido se evita el Gateway
```

## Agregar un microservicio
Propiedad `pedidos360.services.<ms>.base-url` → DTOs propios → `client/<Ms>Client` con `DownstreamRestClientFactory` (ya aporta timeouts, propagación de token/X-Request-Id y traducción de errores) → `service` + `controller` con el mismo path (`/api/<ms>/...`). Seguridad ya cubierta por `/api/**`; en el Gateway lo cubre `/{proxy+}`.
