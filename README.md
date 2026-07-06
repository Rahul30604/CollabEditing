# AI-Powered CollabEditing

AI-Powered CollabEditing is a real-time collaborative document editing platform where multiple users can work on the same document while leveraging an integrated AI assistant for document understanding. The application combines collaborative editing, secure access control, and Retrieval-Augmented Generation (RAG) to provide contextual document Q&A, summarization, and semantic search.

Unlike traditional collaborative editors, the platform introduces a **key-based editing mechanism**, ensuring only one user can actively edit a document at a time, eliminating edit conflicts while maintaining real-time collaboration.

---

## Features

### Real-Time Collaboration

* Multiple users can collaborate on the same document with live synchronization.
* Changes are propagated instantly using RabbitMQ.

### AI-Powered Document Assistant

* Ask questions about uploaded documents using natural language.
* Generate document summaries.
* Perform semantic search to quickly locate relevant information.
* Retrieve context-aware responses using Retrieval-Augmented Generation (RAG).

### Intelligent Document Indexing

* Converts document content into vector embeddings.
* Stores embeddings in a vector database for efficient semantic retrieval.
* Automatically updates the document index whenever content changes.

### Key-Based Editing Control

* Only one user holds the editing key at a time.
* Users request the key from the current editor before making modifications.
* Prevents simultaneous write conflicts while preserving collaboration.

### User Authentication & Authorization

* JWT-based authentication.
* Secure login using email and password.
* Invite collaborators via email.
* Role-based permissions (Owner, Editor, Viewer).
* AI responses respect document access permissions.

### Version Management

* Maintains document history.
* Supports version tracking and rollback.

### Communication

* Real-time comments.
* In-app chat for collaborators.

### Scalable Backend

* Distributed architecture using Spring Boot and RabbitMQ.
* Modular backend design for scalability and maintainability.

---

## Tech Stack

| Layer           | Technology                         |
| --------------- | ---------------------------------- |
| Frontend        | React                              |
| Backend         | Spring Boot                        |
| Database        | MySQL                              |
| Message Broker  | RabbitMQ                           |
| Authentication  | JWT                                |
| AI              | OpenAI/Gemini API                  |
| RAG             | Vector Embeddings                  |
| Vector Database | ChromaDB / Pinecone (Configurable) |

---

## Architecture

```
                    Angular Client
                          │
                          ▼
                  Spring Boot Backend
          ┌───────────────┼───────────────┐
          │               │               │
          ▼               ▼               ▼
      MySQL          RabbitMQ       OpenAI/Gemini
          │                               │
          │                               ▼
          │                      Vector Database
          │                      (Embeddings)
          │
          ▼
   Version History &
   Role-Based Access
```

---

## Installation

### Clone the Repository

```bash
git clone https://github.com/Rahul30604/CollabEditing.git
cd CollabEditing
```

### Frontend

```bash
cd frontend
npm install
npm start
```

### Backend

```bash
cd ../backend
mvn clean install
mvn spring-boot:run
```

---

## Future Enhancements

* AI-powered document comparison
* AI-generated meeting notes
* Context-aware writing assistance
* Multi-document semantic search
* AI-assisted code and technical document review
* OCR support for scanned PDFs

---

## Contributing

Contributions are welcome!

1. Fork the repository.
2. Create a feature branch:

   ```bash
   git checkout -b feature-name
   ```
3. Commit your changes:

   ```bash
   git commit -m "Add feature"
   ```
4. Push to your branch:

   ```bash
   git push origin feature-name
   ```
5. Open a Pull Request.

---


