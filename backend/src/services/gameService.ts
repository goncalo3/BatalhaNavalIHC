// src/services/gameService.ts
import { WebSocket } from 'ws';
import type { Player, Ship } from '../models/types';
import { DEBUG } from '../config/env';
import { verifyToken } from '../middleware/auth';
import { UserModel } from '../db/models/User';
import { GameSessionModel } from '../db/models/GameSession';

// Game state variables
const players = new Map<string, Player>(); // Stores all active players by username
let waitingPlayer: Player | null = null;   // Holds the player waiting in the queue
let activeGames = 0;                       // Counts active games
let playersInQueue = 0;                    // Tracks players in the queue

/**
 * Helper function to send a WebSocket message with logging
 */
const sendMessage = (player: Player, message: any) => {
  const messageString = JSON.stringify(message);
  if (DEBUG) {
    console.log(`Sending message to ${player.username}: ${messageString}`);
  }
  player.ws.send(messageString);
};

/**
 * Function to send a broadcast message to all connected clients
 */
export const broadcast = (clients: Set<WebSocket>, data: any) => {
  const messageString = JSON.stringify(data);
  if (DEBUG) {
    console.log(`Broadcasting to ${clients.size} clients: ${messageString}`);
  }
  
  clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(messageString);
    }
  });
};

/**
 * Get the current game statistics
 */
export const getGameStats = () => ({
  playersInQueue,
  activeGames,
  totalConnectedPlayers: players.size,
});

/**
 * Handle WebSocket authentication
 */
export const handleAuthentication = async (player: Player, token: string) => {
  try {
    const payload = verifyToken(token);
    const user = await UserModel.findById(payload.userId);
    
    if (user) {
      // Check if username is already taken by another active player
      const existingPlayer = players.get(user.username);
      if (existingPlayer) {
        sendMessage(player, {
          type: 'authentication_failed',
          error: 'User already connected',
        });
        return;
      }

      player.userId = user.id;
      player.username = user.username;
      
      // Add player to the map
      players.set(user.username, player);
      
      sendMessage(player, {
        type: 'authentication_success',
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
        },
      });
      
      if (DEBUG) {
        console.log(`Player authenticated as ${user.username}`);
      }
    } else {
      sendMessage(player, {
        type: 'authentication_failed',
        error: 'User not found',
      });
    }
  } catch (error) {
    sendMessage(player, {
      type: 'authentication_failed',
      error: 'Invalid token',
    });
    
    if (DEBUG) {
      console.error(`Authentication failed:`, error);
    }
  }
};

/**
 * Manage the game queue by matching players
 */
export const handleJoinQueue = (player: Player, clients: Set<WebSocket>) => {
  playersInQueue++;
  broadcast(clients, { type: 'players_in_queue', count: playersInQueue }); // Notify all clients about queue count
  
  if (waitingPlayer) {
    // If there's another player waiting, start a new game
    startGame(player, waitingPlayer, clients);
    waitingPlayer = null;
  } else {
    // Otherwise, set this player as the waiting player
    waitingPlayer = player;
    if (DEBUG) console.log(`Player ${player.username} is waiting for an opponent.`);
  }
};

/**
 * Handle player leaving the queue
 */
export const handleLeaveQueue = (player: Player, clients: Set<WebSocket>) => {
  if (waitingPlayer === player) {
    waitingPlayer = null;
    playersInQueue--;
    broadcast(clients, { type: 'players_in_queue', count: playersInQueue });
    if (DEBUG) console.log(`Player ${player.username} left the queue.`);
  }
};

/**
 * Store ship data sent by a player
 */
export const handleShipsData = (player: Player, ships: unknown[]) => {
  try {
    // Cast ships to Ship type - TypeScript will validate the structure
    const validShips = ships as Ship[];
    
    // Initialize ship data with no hits (ensure hits array exists)
    player.ships = validShips.map(ship => ({ 
      ...ship, 
      hits: [] 
    }));
    
    if (DEBUG) console.log(`Player ${player.username} sent ships data`);
    
    // Send confirmation to player
    sendMessage(player, {
      type: 'ships_accepted'
    });
    
    if (player.opponent && player.opponent.ships.length > 0) {
      // Both players have set their ships, start the game with a turn
      if (player.isMyTurn) {
        sendMessage(player, { type: 'your_turn' });
        sendMessage(player.opponent, { type: 'opponent_turn' });
      } else {
        sendMessage(player.opponent, { type: 'your_turn' });
        sendMessage(player, { type: 'opponent_turn' });

      }
    }
  } catch (error) {
    let errorMessage = 'Invalid ship configuration';
    
    if (error instanceof Error) {
      errorMessage = error.message;
    }
    
    sendMessage(player, {
      type: 'ships_validation_error',
      error: errorMessage
    });
    
    if (DEBUG) {
      console.log(`Invalid ships data from ${player.username}: ${errorMessage}`);
    }
  }
};

/**
 * Handle attack actions and determine hit/miss
 */
export const handleAttack = async (player: Player, data: any) => {
  const opponent = player.opponent;
  if (!opponent) return;

  const { x, y } = data;
  let hit = false;
  let destroyedShip: Ship | null = null;

  // Check each ship for a hit
  for (const ship of opponent.ships) {
    for (let i = 0; i < ship.length; i++) {
      const cellX = ship.posX + (ship.isHorizontal ? i : 0);
      const cellY = ship.posY + (ship.isHorizontal ? 0 : i);
      if (cellX === x && cellY === y) {
        hit = true;
        ship.hits?.push({ x, y });
        if (ship.hits?.length === ship.length) {
          destroyedShip = ship; // Mark the ship as destroyed if all parts are hit
        }
        break;
      }
    }
    if (hit) break;
  }

  const resultMessage = { type: 'attack_result', x, y, result: hit ? 'hit' : 'miss' };
  sendMessage(player, resultMessage);
  sendMessage(opponent, { type: 'opponent_attack', x, y, result: hit ? 'hit' : 'miss' });
  
  if (DEBUG) console.log(`Attack result from ${player.username} to ${opponent.username}: ${hit ? 'hit' : 'miss'}`);
  
  // Notify both players if a ship was destroyed
  if (destroyedShip) {
    const shipDestroyedMessage = {
      type: 'ship_destroyed',
      ship: {
        id: opponent.ships.indexOf(destroyedShip),
        posX: destroyedShip.posX,
        posY: destroyedShip.posY,
        length: destroyedShip.length,
        isHorizontal: destroyedShip.isHorizontal,
      }
    };
    sendMessage(player, shipDestroyedMessage);
    if (DEBUG) console.log(`Ship destroyed by ${player.username}: ${JSON.stringify(shipDestroyedMessage)}`);
  }
  
  if (hit) {
    // Check if all ships are destroyed
    const allSunk = opponent.ships.every(ship => ship.hits?.length === ship.length);
    if (allSunk) {
      sendMessage(player, { type: 'you_win' });
      sendMessage(opponent, { type: 'you_lose' });
      if (DEBUG) console.log(`Game over: Player ${player.username} wins.`);
      await endGame(player, opponent, 'win');
    } else {
      sendMessage(player, { type: 'your_turn' });
      sendMessage(opponent, { type: 'opponent_turn' });
    }
  } else {
    // Pass the turn to the opponent
    player.isMyTurn = false;
    opponent.isMyTurn = true;
    sendMessage(opponent, { type: 'your_turn' });
    sendMessage(player, { type: 'opponent_turn' });
  }
};

/**
 * Handle players joining by a friend's username
 */
export const handleJoinFriend = (player: Player, friendUsername: string, clients: Set<WebSocket>) => {
  const friendPlayer = players.get(friendUsername);
  if (friendPlayer === player) {
    sendMessage(player, { type: 'friend_not_found'});
    if (DEBUG) console.log(`Player ${player.username} tried to play with themselves.`);
    return;
  }
  if (friendPlayer) {
    startGame(player, friendPlayer, clients);
  } else {
    sendMessage(player, { type: 'friend_not_found', message: 'Friend not found' });
    if (DEBUG) console.log(`Friend with username ${friendUsername} not found.`);
  }
};

/**
 * Start a new game between two players
 */
export const startGame = async (player1: Player, player2: Player, clients: Set<WebSocket>) => {
  player1.opponent = player2;
  player2.opponent = player1;
  
  const playerStarts = Math.random() < 0.5;
  player1.isMyTurn = playerStarts;
  player2.isMyTurn = !playerStarts;
  
  // Record game start time
  player1.gameStartTime = new Date();
  player2.gameStartTime = new Date();

  // Create database game session if both players are authenticated
  if (player1.userId && player2.userId) {
    try {
      const gameSession = await GameSessionModel.create({
        player1_id: player1.userId,
        player2_id: player2.userId,
      });
      
      player1.gameSessionId = gameSession.id;
      player2.gameSessionId = gameSession.id;
      
      if (DEBUG) {
        console.log(`Game session ${gameSession.id} created for ${player1.username} vs ${player2.username}`);
      }
    } catch (error) {
      if (DEBUG) {
        console.error('Failed to create game session:', error);
      }
    }
  }

  const startGameMessage = { type: 'start_game' };
  sendMessage(player1, startGameMessage);
  sendMessage(player2, startGameMessage);
  
  playersInQueue -= 2;
  activeGames++;
  broadcast(clients, { type: 'players_in_queue', count: playersInQueue });
  broadcast(clients, { type: 'active_games', count: activeGames });
  
  if (DEBUG) console.log(`Game started between ${player1.username} and ${player2.username}.`);
};

/**
 * Handle player disconnections
 */
export const handleDisconnect = async (player: Player, clients: Set<WebSocket>) => {
  if (DEBUG) console.log(`Player ${player.username} disconnected.`);
  
  if (waitingPlayer === player) {
    waitingPlayer = null;
    playersInQueue--;
    broadcast(clients, { type: 'players_in_queue', count: playersInQueue });
  }

  if (player.opponent) {
    sendMessage(player.opponent, { type: 'opponent_disconnected' });
    if (DEBUG) console.log(`Player ${player.opponent.username} wins due to opponent disconnect.`);
    await endGame(player.opponent, player, 'disconnect');
  }

  players.delete(player.username);
};

/**
 * End a game between two players
 */
export const endGame = async (
  winner: Player, 
  loser: Player, 
  endType: 'win' | 'disconnect' | 'abandon' = 'win'
) => {
  // Calculate game duration
  const gameStartTime = winner.gameStartTime || loser.gameStartTime;
  const gameDuration = gameStartTime 
    ? Math.floor((Date.now() - gameStartTime.getTime()) / 1000)
    : null;

  // Update database if both players are authenticated and game session exists
  if (winner.userId && loser.userId && (winner.gameSessionId || loser.gameSessionId)) {
    try {
      const gameSessionId = winner.gameSessionId || loser.gameSessionId!;
      
      if (endType === 'win' && gameDuration) {
        await GameSessionModel.completeGame(gameSessionId, {
          winner_id: winner.userId,
          loser_id: loser.userId,
          game_duration_seconds: gameDuration,
        });
        
        if (DEBUG) {
          console.log(`Game session ${gameSessionId} completed: ${winner.username} beats ${loser.username} in ${gameDuration}s`);
        }
      } else {
        await GameSessionModel.abandonGame(gameSessionId);
        
        if (DEBUG) {
          console.log(`Game session ${gameSessionId} abandoned due to ${endType}`);
        }
      }
    } catch (error) {
      if (DEBUG) {
        console.error('Failed to update game session:', error);
      }
    }
  }

  // Clean up player state
  winner.opponent = null;
  loser.opponent = null;
  winner.gameSessionId = undefined;
  loser.gameSessionId = undefined;
  winner.gameStartTime = undefined;
  loser.gameStartTime = undefined;
  
  activeGames--;
};

// Export the players map so it can be accessed from other modules
export { players };
