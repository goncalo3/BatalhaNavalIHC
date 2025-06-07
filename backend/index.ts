import express from 'express';
import { createServer } from 'http';
import { setupWebSocketServer } from './src/controllers/websocketController';
import { initializeDatabase } from './src/db/connection';
import { httpLogger } from './src/middleware/logging';
import { authenticateToken } from './src/middleware/auth';
import * as authController from './src/controllers/authController';
import * as gameController from './src/controllers/gameController';
import { PORT, DEBUG } from './src/config/env';

// Create the application
const startServer = async () => {
  try {
    // Initialize database connection
    try {
      await initializeDatabase();
      if (DEBUG) {
        console.log('✅ Database initialized successfully');
      }
    } catch (error) {
      console.error('❌ Failed to initialize database:', error);
      process.exit(1);
    }

    // Create Express application
    const app = express();
    
    // Add logging middleware (only in debug mode)
    if (DEBUG) {
      app.use(httpLogger);
    }
    
    // Create HTTP server
    const server = createServer(app);

    // Set up router
    const router = express.Router();

    // Enable JSON parsing
    router.use(express.json());

    // Health check endpoint
    router.get('/health', (req, res) => {
      res.json({ 
        status: 'healthy', 
        timestamp: new Date().toISOString(),
        service: 'battleship-backend'
      });
    });

    // Public routes
    router.post('/auth/register', authController.register);
    router.post('/auth/login', authController.login);
    
    // Public game statistics
    router.get('/stats', gameController.getGameStats);
    router.get('/leaderboard', gameController.getLeaderboard);

    // Protected routes (require authentication)
    router.get('/auth/profile', authenticateToken, authController.getProfile);
    router.put('/auth/profile', authenticateToken, authController.updateProfile);
    

    // Set up API routes
    app.use('/', router);

    // Set up WebSocket server
    setupWebSocketServer(server);

    // Start the server
    server.listen(PORT, () => {
      if (DEBUG) {
        console.log(`
╔══════════════════════════════════════════════════╗
║   Server started in DEBUG mode on port ${PORT}   ║
╚══════════════════════════════════════════════════╝
   
📊 Database: Connected and ready
`);
      } else {
        console.log(`
╔══════════════════════════════════════════════════╗
║          Server started on port ${PORT}          ║
╚══════════════════════════════════════════════════╝
`);
      }
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
};

startServer();