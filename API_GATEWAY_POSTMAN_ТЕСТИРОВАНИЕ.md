# Инструкция по тестированию ApiGateway через Postman (регистрация → логин → запросы → удаление)

Тестируем без фронтенда, только через Postman.

Важно:
- Регистрация пользователя выполняется через UserService и **должна проходить без JWT**.
- Все остальные эндпоинты UserService через Gateway требуют `Authorization: Bearer <access_token>`.

## 1. Что должно быть запущено

- Keycloak: `http://localhost:8095`
- ApiGateway: `http://localhost:8082`
- UserService: `http://localhost:8081`

## 2. Подготовка окружения в Postman

1. Откройте Postman Desktop.
2. Создайте Environment: **ApiGateway Test**.
3. Добавьте переменные:

| Variable | Value |
|---|---|
| `keycloak_url` | `http://localhost:8095` |
| `gateway_url` | `http://localhost:8082` |
| `realm` | `my-realm` |
| `client_id` | `react-frontend` |
| `username` | `testuser01` |
| `password` | `TestPass123!` |
| `email` | `testuser01@mail.com` |
| `first_name` | `Test` |
| `last_name` | `User` |
| `access_token` | *(пусто)* |
| `user_id` | *(пусто)* |

4. Выберите Environment **ApiGateway Test** активным.

## 3. Коллекция запросов (делай в таком порядке)

Создай Collection: **ApiGateway flow (register-login-test-delete)**.

### 3.1 Запрос: Регистрация (создание профиля) через ApiGateway

Это создаст пользователя в Keycloak + профиль в БД UserService.

- Method: `POST`
- URL: `{{gateway_url}}/api/v1/user-profiles/create`
- Headers:
  - `Content-Type: application/json`
- Body → raw → JSON:

```json
{
  "username": "{{username}}",
  "email": "{{email}}",
  "firstName": "{{first_name}}",
  "lastName": "{{last_name}}",
  "password": "{{password}}",
  "confirmPassword": "{{password}}"
}
```

Tests:
```javascript
pm.test("Created", function () {
  pm.expect(pm.response.code).to.be.oneOf([201, 409]);
});

if (pm.response.code === 201) {
  const body = pm.response.json();
  // В вашем UserProfileDto нет поля keycloakId, поэтому user_id тут не сохраняем.
  console.log("User profile created:", body);
}
```

Сохрани как: `01 - Register (create profile)`

Ожидаемо:
- `201 Created` — пользователь создан
- `409/400` — если уже есть такой username/email (тогда поменяй переменные `username/email`)

### 3.2 Запрос: Логин в Keycloak (получить access token)

- Method: `POST`
- URL: `{{keycloak_url}}/realms/{{realm}}/protocol/openid-connect/token`
- Headers:
  - `Content-Type: application/x-www-form-urlencoded`
- Body → x-www-form-urlencoded:
  - `grant_type`: `password`
  - `client_id`: `{{client_id}}`
  - `username`: `{{username}}`
  - `password`: `{{password}}`

Tests (сохраняем токен):
```javascript
pm.test("Token received", function () {
  pm.response.to.have.status(200);
});

const body = pm.response.json();
pm.environment.set("access_token", body.access_token);
console.log("access_token saved");
```

Сохрани как: `02 - Login (get token)`

### 3.3 Запрос: Проверка, что без токена доступ закрыт

UserService читает `X-User-Id` из хедера, а его добавляет Gateway из JWT, поэтому без токена должно быть 401.

- Method: `GET`
- URL: `{{gateway_url}}/api/v1/user-profiles/get`
- Headers: *(пусто)*

Tests:
```javascript
pm.test("Unauthorized without token", function () {
  pm.response.to.have.status(401);
});
```

Сохрани как: `03 - Get profile (no token -> 401)`

### 3.4 Запрос: Получить свой профиль через Gateway (с токеном)

- Method: `GET`
- URL: `{{gateway_url}}/api/v1/user-profiles/get`
- Headers:
  - `Authorization`: `Bearer {{access_token}}`

Tests:
```javascript
pm.test("Not unauthorized", function () {
  pm.expect(pm.response.code).to.not.eql(401);
});

if (pm.response.code === 200) {
  console.log("Profile:", pm.response.json());
}
```

Сохрани как: `04 - Get profile (with token)`

### 3.5 Запрос: Удаление профиля (и пользователя в Keycloak) через Gateway

Удаление в UserService: `DELETE /api/v1/user-profiles/delete` и требует `X-User-Id`.

- Method: `DELETE`
- URL: `{{gateway_url}}/api/v1/user-profiles/delete`
- Headers:
  - `Authorization`: `Bearer {{access_token}}`

Tests:
```javascript
pm.test("Deleted", function () {
  pm.expect(pm.response.code).to.be.oneOf([204, 404]);
});
```

Сохрани как: `05 - Delete profile (with token)`

### 3.6 Запрос: Проверка что пользователь удален

Попробуй залогиниться теми же `username/password` еще раз:

- Повтори запрос `02 - Login (get token)`.

Ожидаемо:
- должен вернуться `400/401` (в зависимости от Keycloak), потому что пользователя уже нет.

## 4. Замечания

- В вашем `UserProfileDto` нет `keycloakId`, поэтому мы не можем сохранить `user_id` из ответа регистрации. Это нормально, потому что методы `get/delete/update` используют `X-User-Id`, а его прокидывает Gateway из JWT автоматически.
- Если регистрация через Gateway возвращает `401`, значит Gateway еще не перезапущен после правки фильтра или путь отличается.
