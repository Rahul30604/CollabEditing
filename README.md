# CollabEditing

CollabEditing is a real-time collaborative document editing application where multiple users can work on the same document. Unlike traditional collaborative tools, CollabEditing introduces a unique key-based editing mechanism to manage user access.

## Features
- **Real-Time Collaboration**: Users can edit documents simultaneously, with changes reflected instantly.
- **Key-Based Editing Control**: Only one user at a time holds the editing key. To edit, a user must receive the key from the current holder.
- **User Authentication**: Secure login using email and password.
- **User Management**: Users can be invited via email to collaborate on documents.
- **Role-Based Access**: Assign different roles like viewer, editor, or owner.
- **Synchronization via RabbitMQ**: Ensures efficient communication and document updates.
- **Version Control**: Tracks changes and maintains document history.
- **Commenting and Chat**: Users can leave comments on documents and chat in real-time.
- **Document Locking Mechanism**: Prevents conflicts by allowing only one active editor at a time.
- **Scalable Architecture**: Built using a distributed system approach similar to Google Docs but on a basic scale.
- **User-Friendly Interface**: Provides an intuitive and seamless editing experience.

## Installation
1. Clone the repository:
   ```sh
   git clone https://github.com/Rahul30604/CollabEditing.git
   ```
2. Navigate to the project directory:
   ```sh
   cd CollabEditing
   ```
3. Install frontend dependencies:
   ```sh
   cd frontend
   npm install
   ```
4. Start the frontend:
   ```sh
   npm start
   ```
5. Install backend dependencies:
   ```sh
   cd ../backend
   mvn install
   ```
6. Start the backend:
   ```sh
   mvn spring-boot:run
   ```

## Technologies Used
- **Frontend**: Angular
- **Backend**: Spring Boot
- **Message Broker**: RabbitMQ
- **Database**: MySQL
- **Authentication**: JWT-based authentication system

## Contribution
Contributions are welcome! Follow these steps:
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature-name`).
3. Commit your changes (`git commit -m 'Add feature'`).
4. Push to the branch (`git push origin feature-name`).
5. Open a Pull Request.

## Contact
For queries and support, reach out via email at [your-email@example.com](mailto:your-email@example.com).

