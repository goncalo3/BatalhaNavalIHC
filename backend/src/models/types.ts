// src/models/types.ts
import { WebSocket } from 'ws';

/**
 * Represents a ship in the game
 */
export interface Ship {
  posX: number;              // X-coordinate of ship's starting position
  posY: number;              // Y-coordinate of ship's starting position
  length: number;            // Length of the ship
  isHorizontal: boolean;     // Orientation of the ship: true if horizontal, false if vertical
  hits: { x: number; y: number }[] | null; // Track hits on the ship by recording positions
}

/**
 * Represents a player in the game
 */
export interface Player {
  ws: WebSocket;             // Player's WebSocket connection
  opponent: Player | null;   // Opponent player, null if not in a game
  isMyTurn: boolean;         // Flag to indicate if it's this player's turn
  ships: Ship[];             // Array of ships placed by this player
  userId: number;            // Database user ID (required - no guest users)
  username: string;          // Username from database
  gameSessionId?: number;    // Database game session ID
  gameStartTime?: Date;      // When the current game started
}
