# Инструкция по тестированию JWT фильтров ApiGateway (Windows)

## 1. Запуск инфраструктуры

```powershell
# Запускаем все контейнеры
docker-compose up -d

# Проверяем, что все контейнеры запущены
docker-compose ps
```

## 2. Проверка доступности сервисов

```powershell
# Проверяем Keycloak
curl http://localhost:8095/realms/my-realm/.well-known/openid_configuration

# Проверяем UserService (должен быть запущен отдельно)
curl http://localhost:8081/actuator/health

# Проверяем ApiGateway
curl http://localhost:8082/actuator/health
```

## 3. Получение JWT токена

### 3.1 Логин как admin (роль ADMIN)
```powershell
curl -X POST "http://localhost:8095/realms/my-realm/protocol/openid-connect/token" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=password&username=admin&password=admin&client_id=react-frontend"
```

### 3.2 Создание тестового пользователя с ролью USER
```powershell
# Сначала получаем админский токен
$adminToken = (curl -X POST "http://localhost:8095/realms/my-realm/protocol/openid-connect/token" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=password&username=admin&password=admin&client_id=react-frontend" | `
  ConvertFrom-Json).access_token

# Создаем пользователя
curl -X POST "http://localhost:8095/admin/realms/my-realm/users" `
  -H "Authorization: Bearer $adminToken" `
  -H "Content-Type: application/json" `
  -d '{
    "username": "testuser",
    "enabled": true,
    "email": "test@example.com",
    "credentials": [{
      "type": "password",
      "value": "testpass",
      "temporary": false
    }]
  }'

# Получаем токен для testuser
curl -X POST "http://localhost:8095/realms/my-realm/protocol/openid-connect/token" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=password&username=testuser&password=testpass&client_id=react-frontend"
```

## 4. Тестирование фильтров

### 4.1 Тест без токена (должен вернуть 401)
```powershell
curl -i http://localhost:8082/api/users/profile
# Ожидаемый результат: HTTP/1.1 401 Unauthorized
```

### 4.2 Тест с просроченным токеном (должен вернуть 401)
```powershell
# Используйте старый токен или подождите 15 минут (accessTokenLifespan: 900)
curl -i http://localhost:8082/api/users/profile `
  -H "Authorization: Bearer <EXPIRED_TOKEN>"
# Ожидаемый результат: HTTP/1.1 401 Unauthorized
```

### 4.3 Тест с валидным токеном (должен пропустить)
```powershell
# Сначала получаем токен
$response = curl -X POST "http://localhost:8095/realms/my-realm/protocol/openid-connect/token" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=password&username=admin&password=admin&client_id=react-frontend" | `
  ConvertFrom-Json

$token = $response.access_token

# Тестируем с токеном
curl -i http://localhost:8082/api/users/profile `
  -H "Authorization: Bearer $token"
# Ожидаемый результат: HTTP/1.1 200 OK (или ошибка от UserService, но фильтр пропустит)
```

### 4.4 Тест доступа к admin эндпоинтам

#### Доступ с ролью ADMIN (должен пропустить)
```powershell
curl -i http://localhost:8082/api/admin/users `
  -H "Authorization: Bearer $token"
# Ожидаемый результат: HTTP/1.1 200 OK или ошибка от сервиса
```

#### Доступ с ролью USER (должен вернуть 403)
```powershell
# Получаем токен обычного пользователя
$userResponse = curl -X POST "http://localhost:8095/realms/my-realm/protocol/openid-connect/token" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=password&username=testuser&password=testpass&client_id=react-frontend" | `
  ConvertFrom-Json

$userToken = $userResponse.access_token

curl -i http://localhost:8082/api/admin/users `
  -H "Authorization: Bearer $userToken"
# Ожидаемый результат: HTTP/1.1 403 Forbidden
```

## 5. Проверка Swagger (должен работать без токена)

```powershell
# Swagger UI
curl http://localhost:8082/swagger-ui.html

# OpenAPI docs
curl http://localhost:8082/api-docs
```

## 6. Проверка хэдеров в downstream запросах

Добавьте логирование в UserService, чтобы увидеть переданные хэдеры:

```java
// В контроллере UserService
@GetMapping("/profile")
public ResponseEntity<?> profile(@RequestHeader("X-User-Id") String userId,
                                @RequestHeader("X-Username") String username) {
    log.info("Received request from user: {} ({})", username, userId);
    // ... логика
}
```

## 7. Тестирование ролей

### Проверка извлечения ролей из токена
Используйте https://jwt.io для декодирования токена и проверки поля `roles`.

### Создание пользователя с ролью OWNER
```powershell
# Получаем ID пользователя
$userId = (curl -X GET "http://localhost:8095/admin/realms/my-realm/users?username=testuser" `
  -H "Authorization: Bearer $adminToken" | `
  ConvertFrom-Json)[0].id

# Получаем ID роли OWNER
$ownerRoleId = (curl -X GET "http://localhost:8095/admin/realms/my-realm/roles" `
  -H "Authorization: Bearer $adminToken" | `
  ConvertFrom-Json | Where-Object {$_.name -eq "OWNER"}).id

# Присваиваем роль
curl -X POST "http://localhost:8095/admin/realms/my-realm/users/$userId/role-mappings/realm" `
  -H "Authorization: Bearer $adminToken" `
  -H "Content-Type: application/json" `
  -d "[{`"id`":`"$ownerRoleId`",`"name`":`"OWNER`"}]"
```

## 8. Отладка логов

```powershell
# Просмотр логов ApiGateway
docker logs -f api-gateway-container

# Просмотр логов Keycloak
docker logs -f keycloak

# Или в PowerShell
docker logs api-gateway-container -Tail 50
docker logs keycloak -Tail 50
```

## 9. Полный сценарий тестирования (PowerShell)

```powershell
# 1. Получаем токен админа
$adminResponse = curl -X POST "http://localhost:8095/realms/my-realm/protocol/openid-connect/token" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=password&username=admin&password=admin&client_id=react-frontend" | `
  ConvertFrom-Json

$adminToken = $adminResponse.access_token

# 2. Получаем токен обычного пользователя
$userResponse = curl -X POST "http://localhost:8095/realms/my-realm/protocol/openid-connect/token" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=password&username=testuser&password=testpass&client_id=react-frontend" | `
  ConvertFrom-Json

$userToken = $userResponse.access_token

# 3. Тестируем доступ
Write-Host "Testing without token:"
curl -i http://localhost:8082/api/users/profile

Write-Host "`nTesting with admin token to user endpoint:"
curl -i http://localhost:8082/api/users/profile -H "Authorization: Bearer $adminToken"

Write-Host "`nTesting with user token to admin endpoint:"
curl -i http://localhost:8082/api/admin/users -H "Authorization: Bearer $userToken"

Write-Host "`nTesting with admin token to admin endpoint:"
curl -i http://localhost:8082/api/admin/users -H "Authorization: Bearer $adminToken"
```

## 10. Альтернативный способ через Postman/Insomnia

Если curl не работает, используйте Postman:

1. **Создайте запрос** для получения токена:
   - Method: POST
   - URL: `http://localhost:8095/realms/my-realm/protocol/openid-connect/token`
   - Headers: `Content-Type: application/x-www-form-urlencoded`
   - Body: `grant_type=password&username=admin&password=admin&client_id=react-frontend`

2. **Скопируйте access_token** из ответа

3. **Тестируйте эндпоинты** с Authorization header:
   - Header: `Authorization: Bearer <скопированный_токен>`

## 11. Ожидаемые результаты

- **Без токена**: 401 Unauthorized
- **С неверным токеном**: 401 Unauthorized  
- **С просроченным токеном**: 401 Unauthorized
- **USER к /api/admin/*** : 403 Forbidden
- **ADMIN/OWNER к /api/admin/*** : 200 OK (или ошибка сервиса)
- **Любая роль к /api/users/*** : 200 OK (или ошибка сервиса)
- **Swagger эндпоинты**: 200 OK без токена

## 12. Возможные проблемы и решения (Windows)

### Проблема: curl не найден
**Решение**: Установите curl или используйте PowerShell Invoke-RestRequest:
```powershell
Invoke-RestMethod -Uri "http://localhost:8095/realms/my-realm/.well-known/openid_configuration"
```

### Проблема: "Срок действия JWT токена истек"
**Решение**: Проверьте синхронизацию времени:
```powershell
docker exec keycloak date
Get-Date
```

### Проблема: Кодировка в PowerShell
**Решение**: Используйте UTF-8:
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

### Проблема: JSON парсинг
**Решение**: Используйте встроенный PowerShell JSON парсер:
```powershell
$response = curl ... | ConvertFrom-Json
$token = $response.access_token
```

## 13. Полезные команды для Windows

```powershell
# Декодирование JWT токена (требует jq или онлайн декодер)
# Используйте https://jwt.io для декодирования

# Проверка конфигурации Keycloak realm
curl http://localhost:8095/realms/my-realm | ConvertFrom-Json

# Проверка JWKS (публичные ключи)
curl http://localhost:8095/realms/my-realm/protocol/openid-connect/certs | ConvertFrom-Json

# Проверка статуса контейнеров
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Перезапуск конкретного контейнера
docker restart keycloak
docker-compose restart api-gateway
```

## 14. Быстрый тест (One-liner)

```powershell
# Быстрая проверка с токеном админа
$token = (curl -X POST "http://localhost:8095/realms/my-realm/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=password&username=admin&password=admin&client_id=react-frontend" | ConvertFrom-Json).access_token; curl -i http://localhost:8082/api/users/profile -H "Authorization: Bearer $token"
```

## 15. Установка необходимых инструментов для Windows

Если у вас не установлены необходимые инструменты:

```powershell
# Установка Docker Desktop
# Скачайте с https://www.docker.com/products/docker-desktop

# Установка jq (опционально)
winget install jqlang.jq

# Проверка версий
docker --version
curl --version
```
