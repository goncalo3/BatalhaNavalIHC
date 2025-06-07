// src/controllers/websocketController.ts
import { WebSocketServer, WebSocket } from 'ws';
import { Server } from 'http';
import { parse } from 'url';
import type { Player } from '../models/types';
import { DEBUG } from '../config/env';
import { verifyToken } from '../middleware/auth';
import { UserModel } from '../db/models/User';
import * as gameService from '../services/gameService';


export const setupWebSocketServer = (server: Server): WebSocketServer => {
  const wss = new WebSocketServer({ server });

  // Handle new WebSocket connections
  wss.on('connection', async (ws: WebSocket, request) => {
    let player: Player | null = null;

    // Authenticate the user from query parameters or headers
    try {
      const url = parse(request.url || '', true);
      const token = url.query.token as string || 
                   request.headers.authorization?.split(' ')[1];

      if (!token) {
        const errorMessage = { 
          type: 'connection_error', 
          error: 'Authentication required' 
        };
        ws.send(JSON.stringify(errorMessage));
        ws.close();
        return;
      }

      const payload = verifyToken(token);
      const user = await UserModel.findById(payload.userId);
      
      if (!user) {
        const errorMessage = { 
          type: 'connection_error', 
          error: 'User not found' 
        };
        ws.send(JSON.stringify(errorMessage));
        ws.close();
        return;
      }
      
      // Check if user is already connected
      if (gameService.players.has(user.username)) {
        const errorMessage = { 
          type: 'connection_error', 
          error: 'User already connected' 
        };
        ws.send(JSON.stringify(errorMessage));
        ws.close();
        return;
      }
      
      player = {
        ws,
        opponent: null,
        isMyTurn: false,
        ships: [],
        userId: user.id,
        username: user.username,
      } as Player;
      
      if (DEBUG) {
        console.log(`Authenticated player connected: ${user.username}`);
      }

    } catch (error) {
      const errorMessage = { 
        type: 'connection_error', 
        error: 'Invalid authentication token' 
      };
      ws.send(JSON.stringify(errorMessage));
      ws.close();
      return;
    }

    // Add player to the players map using username as key
    gameService.players.set(player.username, player);
    
    // Send connection success to the client
    const successMessage = { 
      type: 'connection_success',
      username: player.username 
    };
    ws.send(JSON.stringify(successMessage));

    // Handle incoming messages from this player
    ws.on('message', (message: string) => {
      try {
        const data = JSON.parse(message.toString());
        if (DEBUG) console.log(`Received from ${player.username}: ${message}`);

        // Handle different types of messages
        switch (data.type) {
          case 'join_queue':
            gameService.handleJoinQueue(player, wss.clients);
            break;
          case 'leave_queue':
            gameService.handleLeaveQueue(player, wss.clients);
            break;
          case 'ships_data':
            gameService.handleShipsData(player, data.ships);
            break;
          case 'attack':
            gameService.handleAttack(player, data);
            break;
          case 'join_friend':
            gameService.handleJoinFriend(player, data.friend_username, wss.clients);
            break;
          case 'authenticate':
            // Authentication is handled at connection time, not after
            const authErrorMessage = {
              type: 'error',
              message: 'Already authenticated'
            };
            ws.send(JSON.stringify(authErrorMessage));
            break;
          default:
            if (DEBUG) console.log(`Unknown message type: ${data.type}`);
        }
      } catch (error) {
        if (DEBUG) console.error(`Error parsing message from ${player.username}:`, error);
      }
    });

    // Handle WebSocket close and error events
    ws.on('close', () => gameService.handleDisconnect(player, wss.clients));
    ws.on('error', error => {
      if (DEBUG) console.log(`WebSocket error for ${player.username}: ${error}`);
    });
  });

  return wss;
};
