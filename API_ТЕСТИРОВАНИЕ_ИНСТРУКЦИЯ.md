# 📋 Инструкция по тестированию API endpoints

## 🚀 Запуск приложения

1. **Порт приложения**: `8081`
2. **Swagger UI**: `http://localhost:8081/swagger-ui.html`
3. **API Docs**: `http://localhost:8081/api-docs`

## 📚 Доступные эндпоинты

### 🔐 User Profile API (`/api/v1/user-profiles`)

#### 1. Получить профиль пользователя
- **Метод**: `GET`
- **URL**: `http://localhost:8081/api/v1/user-profiles/get`
- **Headers**: 
  - `X-User-Id`: `keycloak_id_пользователя`
- **Пример в Postman**:
  ```json
  // Headers
  X-User-Id: "123e4567-e89b-12d3-a456-426614174000"
  ```

#### 2. Получить все профили пользователей
- **Метод**: `GET`
- **URL**: `http://localhost:8081/api/v1/user-profiles/all`
- **Query параметры** (опционально):
  - `page`: номер страницы (по умолчанию 1)
  - `size`: размер страницы (по умолчанию 20)
  - `sort`: поле сортировки (по умолчанию createdAt)
  - `direction`: направление сортировки (ASC/DESC)
- **Пример**: `http://localhost:8081/api/v1/user-profiles/all?page=0&size=10&sort=createdAt&direction=DESC`

#### 3. Создать новый профиль пользователя
- **Метод**: `POST`
- **URL**: `http://localhost:8081/api/v1/user-profiles/create`
- **Headers**: 
  - `Content-Type`: `application/json`
- **Body**:
  ```json
  {
    "username": "john_doe",
    "email": "user@example.com",
    "firstName": "Вася",
    "lastName": "Васин",
    "password": "SecurePass123!",
    "confirmPassword": "SecurePass123!"
  }
  ```

#### 4. Обновить профиль пользователя
- **Метод**: `PUT`
- **URL**: `http://localhost:8081/api/v1/user-profiles/update`
- **Headers**: 
  - `Content-Type`: `application/json`
  - `X-User-Id`: `keycloak_id_пользователя`
- **Body**:
  ```json
  {
    "username": "john_doe_updated",
    "email": "updated@example.com",
    "firstName": "Василий",
    "lastName": "Васильев",
    "password": "NewSecurePass123!",
    "confirmPassword": "NewSecurePass123!"
  }
  ```

#### 5. Удалить профиль пользователя
- **Метод**: `DELETE`
- **URL**: `http://localhost:8081/api/v1/user-profiles/delete`
- **Headers**: 
  - `X-User-Id`: `keycloak_id_пользователя`

### 👑 Admin Management API (`/api/admin`)

#### 1. Удалить пользователя по username
- **Метод**: `DELETE`
- **URL**: `http://localhost:8081/api/admin/users/{username}`
- **Query параметры**:
  - `adminUsername`: имя администратора
- **Пример**: `http://localhost:8081/api/admin/users/john_doe?adminUsername=admin`

#### 2. Повысить роль пользователя
- **Метод**: `PUT`
- **URL**: `http://localhost:8081/api/admin/users/{username}/promote`
- **Query параметры**:
  - `newRole`: новая роль (ADMIN, USER, MANAGER)
  - `adminUsername`: имя администратора
- **Пример**: `http://localhost:8081/api/admin/users/john_doe/promote?newRole=ADMIN&adminUsername=admin`

#### 3. Понизить роль пользователя
- **Метод**: `PUT`
- **URL**: `http://localhost:8081/api/admin/users/{username}/demote`
- **Query параметры**:
  - `newRole`: новая роль
  - `adminUsername`: имя администратора

#### 4. Изменить роль пользователя
- **Метод**: `PUT`
- **URL**: `http://localhost:8081/api/admin/users/{username}/role`
- **Query параметры**:
  - `newRole`: новая роль
  - `adminUsername`: имя администратора

#### 5. Получить роль пользователя
- **Метод**: `GET`
- **URL**: `http://localhost:8081/api/admin/users/{username}/role`

## 🔧 Настройка Postman

### 1. Создание коллекции
1. Откройте Postman
2. Создайте новую коллекцию "UserService API"
3. Добавьте переменные окружения:
   - `base_url`: `http://localhost:8081`
   - `user_id`: `your_keycloak_id`

### 2. Пример настроек запроса в Postman

#### GET запрос для получения профиля:
```
Method: GET
URL: {{base_url}}/api/v1/user-profiles/get
Headers:
  X-User-Id: {{user_id}}
```

#### POST запрос для создания профиля:
```
Method: POST
URL: {{base_url}}/api/v1/user-profiles/create
Headers:
  Content-Type: application/json
Body (raw, JSON):
{
  "username": "test_user",
  "email": "test@example.com",
  "firstName": "Тест",
  "lastName": "Тестов",
  "password": "TestPass123!",
  "confirmPassword": "TestPass123!"
}
```

## 📖 Swagger Documentation

### Доступ к Swagger UI:
1. Запустите приложение
2. Откройте браузер
3. Перейдите по адресу: `http://localhost:8081/swagger-ui.html`

### Возможности Swagger:
- 📋 Просмотр всех доступных эндпоинтов
- 📝 Интерактивная документация
- 🧪 Прямое тестирование API из браузера
- 📄 Просмотр схем запросов/ответов
- 🔍 Поиск по эндпоинтам

### Доступ к OpenAPI JSON:
- URL: `http://localhost:8081/api-docs`
- Используется для интеграции с другими инструментами

## ⚙️ Дополнительная информация

### Keycloak конфигурация:
- **Realm**: `my-realm`
- **Auth Server URL**: `http://localhost:8095`
- **Client ID**: `authservice-client`

### Валидация данных:
- **Email**: должен быть валидным email адресом
- **Password**: минимум 8 символов
- **Username**: не может быть пустым
- **First/Last Name**: обязательные поля

### HTTP статусы ответов:
- `200 OK` - успешное выполнение
- `201 Created` - ресурс создан
- `204 No Content` - успешное удаление
- `400 Bad Request` - ошибка валидации
- `403 Forbidden` - ошибка безопасности
- `404 Not Found` - ресурс не найден
- `500 Internal Server Error` - ошибка сервера

## 🐛 Отладка

### Логирование включено для:
- SQL запросов
- Liquibase миграций
- Spring Cloud Vault
- Корневого уровня (DEBUG)

### Полезные эндпоинты для отладки:
- `/actuator/health` - проверка здоровья приложения
- `/actuator/info` - информация о приложении
- `/actuator/metrics` - метрики приложения

## 📝 Тестовый сценарий

1. **Создание пользователя**: POST `/api/v1/user-profiles/create`
2. **Получение профиля**: GET `/api/v1/user-profiles/get`
3. **Получение всех профилей**: GET `/api/v1/user-profiles/all`
4. **Обновление профиля**: PUT `/api/v1/user-profiles/update`
5. **Управление ролями**: PUT `/api/admin/users/{username}/role`
6. **Удаление пользователя**: DELETE `/api/v1/user-profiles/delete`

---

**Готово! Теперь вы можете тестировать все эндпоинты вашего UserService через Postman и Swagger UI.** 🎉
