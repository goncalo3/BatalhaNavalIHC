# Battle Ships Game

## Features

- Real-time multiplayer gameplay with WebSockets
- Game matchmaking system
- Ships placement and attack mechanics
- Game state tracking
- JWT-based authentication system
- Database integration for user accounts and game statistics

## 🔐 Authentication System

### JWT Token Authentication
- Tokens expire in 24 hours (configurable via `JWT_EXPIRES_IN` environment variable).
- Include token in WebSocket connection: `ws://domain.tld?token=<jwt_token>`
- Include token in HTTP requests: `Authorization: Bearer <jwt_token>`

## API Documentation

### HTTP Endpoints

The backend exposes the following REST API endpoints. Note: Protected routes require a valid JWT token in the `Authorization: Bearer <token>` header.

#### Health Check
- **Endpoint**: `GET /health`
- **Description**: Retrieves the health status of the server.
- **Response**: JSON object indicating service status.
  ```json
  {
    "status": "healthy",
    "timestamp": "YYYY-MM-DDTHH:mm:ss.sssZ",
    "service": "battleship-backend"
  }
  ```

#### Authentication
- **Endpoint**: `POST /auth/register`
- **Description**: Registers a new user.
- **Request Body**:
  ```json
  {
    "username": "newuser",
    "email": "user@example.com",
    "password": "password123"
  }
  ```
- **Response**: Success message or error.

- **Endpoint**: `POST /auth/login`
- **Description**: Logs in an existing user.
- **Request Body**:
  ```json
  {
    "username": "testuser1",
    "password": "password123"
  }
  ```
- **Response**: JSON object containing the JWT token.
  ```json
  {
    "token": "your_jwt_token_here"
  }
  ```

- **Endpoint**: `GET /auth/profile` (Protected)
- **Description**: Retrieves the profile of the authenticated user.
- **Response**: JSON object with user details (e.g., id, username, email).

- **Endpoint**: `PUT /auth/profile` (Protected)
- **Description**: Updates the profile of the authenticated user.
- **Request Body**:
  ```json
  {
    "username": "newusername", // Optional
    "email": "newemail@example.com" // Optional
  }
  ```
- **Response**: JSON object with updated user details.

#### Game Statistics & Information
- **Endpoint**: `GET /stats`
- **Description**: Retrieves public game statistics.
- **Response**: JSON object containing:
  - `playersInQueue`: Number of players waiting for an opponent
  - `activeGames`: Number of active game sessions

- **Endpoint**: `GET /leaderboard?limit=10`
- **Description**: Retrieves the top players leaderboard. `limit` is an optional query parameter.
- **Response**: JSON array of leaderboard entries.


### WebSocket API

The game uses WebSocket communication for real-time gameplay.


#### Server to Client Messages

When connected to the WebSocket server, you may receive the following message types:

| Message Type           | Description                                  | Payload                                                                                                |
|------------------------|----------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `player_id`            | Sent when a player connects                  | `{ id: string, authenticated?: boolean, username?: string }` - Unique ID, auth status, and username if authenticated. |
| `players_in_queue`     | Updates about queue status                   | `{ count: number }` - Current number of players in queue                                               |
| `active_games`         | Updates about active games                   | `{ count: number }` - Current number of active games                                                   |
| `start_game`           | Notifies that a game is starting             | No additional payload                                                                                  |
| `ships_accepted`       | Confirmation that ship placement is valid    | No additional payload                                                                                  |
| `ships_validation_error` | Ship placement validation failed           | `{ error: string }` - Description of the validation error                                              |
| `your_turn`            | Indicates it's the player's turn             | No additional payload                                                                                  |
| `attack_result`        | Result of the player's attack                | `{ x: number, y: number, result: 'hit' or 'miss' }`                                                    |
| `opponent_attack`      | Notification of opponent's attack            | `{ x: number, y: number, result: 'hit' or 'miss' }`                                                    |
| `ship_destroyed`       | Notification that a ship was destroyed       | `{ ship: { id: number, posX: number, posY: number, length: number, isHorizontal: boolean } }`          |
| `you_win`              | Notification that the player won             | No additional payload                                                                                  |
| `you_lose`             | Notification that the player lost            | No additional payload                                                                                  |
| `opponent_disconnected`| Notification that the opponent left the game | No additional payload                                                                                  |
| `friend_not_found`     | Notification that a friend ID was not found  | No additional payload                                                                                  |

#### Client to Server Messages

The client can send the following message types to the server:

| Message Type | Description                            | Payload                                                |
|------------- |----------------------------------------|------------------------------------------------------- |
| `authenticate` | Authenticate the WebSocket session     | `{ token: string }` - JWT token                        |
| `join_queue` | Request to join the matchmaking queue  | No additional payload                                  |
| `leave_queue`| Request to leave the matchmaking queue | No additional payload                                  |
| `ships_data` | Send ship placement information        | `{ ships: Ship[] }` - Array of ship objects            |
| `attack`     | Make an attack on the opponent's board | `{ x: number, y: number }` - Coordinates of the attack |
| `join_friend`| Join a specific friend's game          | `{ friend_id: string }` - ID of the friend to join     |

#### Data Structures

**Ship Object**
```typescript
{
  posX: number;         // X-coordinate of ship's starting position
  posY: number;         // Y-coordinate of ship's starting position
  length: number;       // Length of the ship
  isHorizontal: boolean; // Orientation of the ship: true if horizontal, false if vertical
  hits: { x: number; y: number }[] | null; // Track hits on the ship by recording positions

}
```
**Player Object**
```typescript
interface Player {
  ws: WebSocket;             // Player's WebSocket connection
  opponent: Player | null;   // Opponent player, null if not in a game
  isMyTurn: boolean;         // Flag to indicate if it's this player's turn
  ships: Ship[];             // Array of ships placed by this player
  id: string;                // Unique WebSocket session ID for the player
  userId?: number;           // Database user ID (if authenticated)
  username?: string;         // Username (if authenticated)
  gameSessionId?: number;    // Database game session ID
  gameStartTime?: Date;      // Game start time
}
```

#### Game Flow

1. **Connection**: Client connects to server with a WebSocket
2. **Matchmaking**: Client sends `join_queue` to enter matchmaking
3. **Game Start**: When two players are matched, both receive `start_game` notification
4. **Ship Placement**: Both players send `ships_data` with their ship placements
5. **Gameplay**: Players take turns making attacks with `attack` messages
   - After each attack, both players receive appropriate notifications
   - Ships that are fully hit are marked as destroyed
6. **Game End**: When all ships of one player are destroyed, the game ends
   - Winner receives `you_win`
   - Loser receives `you_lose`

#### Ship Validation Rules

When sending `ships_data`, the ship placement must follow these rules:

**Ship Configuration (exactly 5 ships required):**
- 1 Carrier (length 5)
- 1 Battleship (length 4)
- 1 Cruiser (length 3)
- 1 Submarine (length 3)
- 1 Destroyer (length 2)

**Placement Rules:**
- Ships must be placed within the 10x10 game board (coordinates 0-9)
- Ships cannot overlap with each other
- Ships can be placed horizontally or vertically only
- Each ship must fit entirely within the board boundaries

If ship validation fails, the server will respond with `ships_validation_error` containing a descriptive error message. Valid ship placements will receive a `ships_accepted` confirmation.

## 🗄️ Database Schema

### Users Table
- `id` - Primary key
- `username` - Unique username
- `email` - Unique email
- `password_hash` - Bcrypt hashed password
- `created_at` - Registration timestamp
- `updated_at` - Last update timestamp

### Game Sessions Table
- `id` - Primary key
- `player1_id` - First player (foreign key to Users table)
- `player2_id` - Second player (foreign key to Users table)
- `winner_id` - Winner (foreign key to Users table, nullable)
- `loser_id` - Loser (foreign key to Users table, nullable)
- `game_duration_seconds` - Game duration in seconds
- `started_at` - Game start timestamp
- `ended_at` - Game end timestamp
- `game_status` - Enum: 'active', 'completed', 'abandoned'

### User Stats View
(This is typically a view or calculated dynamically)
Automatically calculated statistics for each user:
- `wins` - Number of wins
- `losses` - Number of losses
- `total_games` - Total completed games
- `win_percentage` - Win rate percentage
- `avg_win_duration` - Average duration of won games

## 🔧 Environment Variables
```env
# Server Configuration
PORT=3000                 # The port the server will run on
DEBUG=true                # Enable/disable debug logging

# Database Configuration (ensure these are set for database functionality)
DB_HOST=localhost         # Database host
DB_PORT=3306              # Database port
DB_USER=root              # Database username
DB_PASSWORD=your_password # Database password
DB_NAME=battleship_game   # Database name

# JWT Configuration (ensure these are set for authentication)
JWT_SECRET=your_jwt_secret_key_here # Secret key for signing JWTs
JWT_EXPIRES_IN=24h                  # JWT expiration time (e.g., 1h, 7d)
```

## 🛡️ Security Features

- **Password Hashing**: Passwords are hashed using bcrypt (typically 10 rounds or more) before storing in the database.
- **JWT Tokens**: Secure JSON Web Tokens are used for session management and API authentication, with configurable expiration times.
- **SQL Injection Prevention**: Use of parameterized queries or ORMs that inherently protect against SQL injection when interacting with the database.
- **Authentication Required**: Sensitive operations and user-specific data access require valid JWT authentication.

## 📊 Game Statistics Features

- **Game History Tracking**: A history of completed games is kept.
- **Leaderboard System**: A public leaderboard shows top players based on wins and losses.
- **Comprehensive Game Session Logging**: Details of each game (players, duration, outcome) are logged in the database.

## Project Structure

```
backend/
├── src/
│   ├── config/      # Configuration files
│   ├── controllers/ # Route and WebSocket controllers
│   ├── models/      # Data models and types
│   └── services/    # Business logic
└── index.ts         # Application entry point
frontend/
└── batalhanaval/
    └── src/
        ├── main/java/
        │   ├── module-info.java
        │   └── batalhanaval/ # Java source files for game logic and UI controllers
        │       ├── BattleshipApplication.java
        │       ├── BattleshipController.java
        │       ├── Game.java
        │       ├── HomeController.java
        │       ├── Leaderboard.java
        │       ├── LeaderboardController.java
        │       ├── LeaderboardEntry.java
        │       ├── LoginController.java
        │       ├── ServerConfig.java
        │       ├── Ship.java
        │       ├── ShipPlacementController.java
        │       ├── User.java
        │       └── WaitingController.java
        └── main/resources/
            └── batalhanaval/ # Static assets
                ├── enemyAvatar.png
                ├── playerAvatar.png
                ├── css/      # CSS stylesheets
                │   ├── auth-styles.css
                │   ├── game-styles.css
                │   ├── game2.css
                │   ├── home.css
                │   └── stats.css
                └── fxml/     # FXML files for UI layout
                    ├── home-view.fxml
                    ├── leaderboard-view.fxml
                    ├── login-view.fxml
                    ├── playing.fxml
                    ├── ship-placement-view.fxml
                    └── waiting-view.fxml
```
