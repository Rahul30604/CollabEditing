# AI-Powered Collaborative Document Editor

A real-time collaborative document editing platform with AI-powered document Q&A, built with React and Spring Boot.

---

## Tech Stack

| Layer          | Technology                              |
| -------------- | --------------------------------------- |
| Frontend       | React 18, React Router, Material UI     |
| Backend        | Java 21, Spring Boot 3.2, Spring Security |
| Database       | MySQL 8.0 (Docker)                      |
| Authentication | JWT (access + refresh tokens)           |
| Build Tool     | Maven                                   |

---

## Project Structure

```
CollabEditor/
├── backend/                    # Spring Boot API
│   ├── pom.xml
│   └── src/main/java/com/collabeditor/
│       ├── config/             # Security config, exception handler
│       ├── controller/         # REST controllers
│       ├── dto/                # Request/Response DTOs
│       ├── entity/             # JPA entities
│       ├── repository/         # Spring Data JPA repos
│       ├── security/           # JWT filter, token provider
│       ├── service/            # Business logic
│       └── CollabEditorApplication.java
├── frontend/                   # React SPA
│   ├── package.json
│   ├── public/
│   └── src/
│       ├── components/         # Reusable UI components
│       ├── context/            # Auth context
│       ├── pages/              # Login, Register, Dashboard, Editor
│       ├── services/           # API service layer
│       └── utils/              # Theme config
├── docker-compose.yml          # MySQL service
└── README.md
```

---

## Prerequisites

- Java 21
- Maven 3.8+
- Node.js 18+
- Docker & Docker Compose

---

## Getting Started

### 1. Start MySQL

```bash
docker-compose up -d
```

This starts MySQL 8.0 on port 3306 with:
- Database: `collabeditor`
- User: `collabuser` / Password: `collabpass`

### 2. Start Backend

```bash
cd backend
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

### 3. Start Frontend

```bash
cd frontend
npm install
npm start
```

The app will be available at `http://localhost:3000`.

---

## API Endpoints

### Authentication

| Method | Endpoint            | Description       | Auth |
| ------ | ------------------- | ----------------- | ---- |
| POST   | /api/auth/register  | Register user     | No   |
| POST   | /api/auth/login     | Login             | No   |
| GET    | /api/auth/me        | Get current user  | Yes  |

### Documents

| Method | Endpoint              | Description         | Auth |
| ------ | --------------------- | ------------------- | ---- |
| POST   | /api/documents        | Create document     | Yes  |
| GET    | /api/documents        | List my documents   | Yes  |
| GET    | /api/documents/:id    | Get document        | Yes  |
| PUT    | /api/documents/:id    | Update document     | Yes  |
| DELETE | /api/documents/:id    | Delete document     | Yes  |

### Sharing

| Method | Endpoint                          | Description         | Auth |
| ------ | --------------------------------- | ------------------- | ---- |
| POST   | /api/documents/:id/share          | Share document      | Yes  |
| GET    | /api/documents/:id/share          | List permissions    | Yes  |
| DELETE | /api/documents/:id/share/:userId  | Remove permission   | Yes  |

---

## Role-Based Permissions

| Feature     | Owner | Editor | Viewer |
| ----------- | ----- | ------ | ------ |
| Edit        | Yes   | Yes    | No     |
| Delete      | Yes   | No     | No     |
| Share       | Yes   | No     | No     |
| View        | Yes   | Yes    | Yes    |
| AI Q&A      | Yes   | Yes    | Yes    |

---

## Development Phases

### Phase 1 (Current)
- User authentication (register, login, JWT)
- Document CRUD
- Role-based sharing (Owner, Editor, Viewer)
- React frontend with Material UI

### Phase 2
- Real-time collaboration (WebSocket)
- RabbitMQ event handling
- Key-based editing control
- Version history

### Phase 3
- AI integration (OpenAI/Gemini)
- Embedding generation
- ChromaDB vector storage
- Document Q&A, Summarization, Semantic Search

### Phase 4
- Performance optimization
- Redis caching
- Docker containerization
- AWS deployment
- Monitoring (Grafana)

---

## Environment Variables

Backend configuration is in `backend/src/main/resources/application.yml`. Key settings:

| Property                    | Default Value   | Description          |
| --------------------------- | --------------- | -------------------- |
| server.port                 | 8080            | Backend port         |
| spring.datasource.url      | localhost:3306  | MySQL connection     |
| app.jwt.expiration-ms      | 86400000 (24h) | Token expiry         |
| app.jwt.refresh-expiration-ms | 604800000 (7d) | Refresh token expiry |
