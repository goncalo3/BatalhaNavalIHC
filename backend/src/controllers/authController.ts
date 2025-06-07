// src/controllers/authController.ts
import type { Request, Response } from 'express';
import { UserModel, type CreateUserData } from '../db/models/User';
import { generateToken } from '../middleware/auth';
import { DEBUG } from '../config/env';

/**
 * Register a new user
 */
export const register = async (req: Request, res: Response): Promise<void> => {
  try {
    const { username, email, password } = req.body;

    // Validate input
    if (!username || !email || !password) {
      res.status(400).json({ 
        error: 'Username, email, and password are required' 
      });
      return;
    }

    // Validate password strength
    if (password.length < 6) {
      res.status(400).json({ 
        error: 'Password must be at least 6 characters long' 
      });
      return;
    }

    // Check if user already exists
    const existingUser = await UserModel.findByUsername(username);
    if (existingUser) {
      res.status(409).json({ error: 'Username already exists' });
      return;
    }

    const existingEmail = await UserModel.findByEmail(email);
    if (existingEmail) {
      res.status(409).json({ error: 'Email already registered' });
      return;
    }

    // Create user
    const userData: CreateUserData = { username, email, password };
    const user = await UserModel.create(userData);

    // Generate token
    const token = generateToken({
      id: user.id,
      username: user.username,
      email: user.email,
    });

    if (DEBUG) {
      console.log(`New user registered: ${username} (${email})`);
    }

    res.status(201).json({
      message: 'User registered successfully',
      token,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
      },
    });
  } catch (error) {
    if (DEBUG) {
      console.error('Registration error:', error);
    }
    res.status(500).json({ error: 'Internal server error' });
  }
};

/**
 * Login user
 */
export const login = async (req: Request, res: Response): Promise<void> => {
  try {

    const { username, password } = req.body;

    if (!username || !password) {
      res.status(400).json({ 
        error: 'Username and password are required' 
      });
      return;
    }

    // Find user by username or email
    let user = await UserModel.findByUsername(username);
    if (!user) {
      user = await UserModel.findByEmail(username);
    }

    if (!user) {
      res.status(401).json({ error: 'Invalid credentials' });
      return;
    }

    // Verify password
    const isValidPassword = await UserModel.verifyPassword(password, user.password_hash);
    if (!isValidPassword) {
      res.status(401).json({ error: 'Invalid credentials' });
      return;
    }

    // Generate token
    const token = generateToken({
      id: user.id,
      username: user.username,
      email: user.email,
    });

    if (DEBUG) {
      console.log(`User logged in: ${user.username}`);
    }

    res.json({
      message: 'Login successful',
      token,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
      },
    });
  } catch (error) {
    if (DEBUG) {
      console.error('Login error:', error);
    }
    res.status(500).json({ error: 'Internal server error' });
  }
};

/**
 * Get current user profile
 */
export const getProfile = async (req: Request, res: Response): Promise<void> => {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Authentication required' });
      return;
    }

    const stats = await UserModel.getUserStats(req.user.id);
    
    res.json({
      user: req.user,
      stats: stats || {
        wins: 0,
        losses: 0,
        total_games: 0,
        win_percentage: 0,
        avg_win_duration: null,
      },
    });
  } catch (error) {
    if (DEBUG) {
      console.error('Profile error:', error);
    }
    res.status(500).json({ error: 'Internal server error' });
  }
};

/**
 * Update user profile
 */
export const updateProfile = async (req: Request, res: Response): Promise<void> => {
  try {
    if (!req.user) {
      res.status(401).json({ error: 'Authentication required' });
      return;
    }

    const { username, email } = req.body;
    const updates: any = {};

    if (username) updates.username = username;
    if (email) updates.email = email;

    if (Object.keys(updates).length === 0) {
      res.status(400).json({ error: 'No updates provided' });
      return;
    }

    // Check for conflicts
    if (username) {
      const existingUser = await UserModel.findByUsername(username);
      if (existingUser && existingUser.id !== req.user.id) {
        res.status(409).json({ error: 'Username already exists' });
        return;
      }
    }

    if (email) {
      const existingEmail = await UserModel.findByEmail(email);
      if (existingEmail && existingEmail.id !== req.user.id) {
        res.status(409).json({ error: 'Email already registered' });
        return;
      }
    }

    const updatedUser = await UserModel.updateProfile(req.user.id, updates);
    
    res.json({
      message: 'Profile updated successfully',
      user: {
        id: updatedUser!.id,
        username: updatedUser!.username,
        email: updatedUser!.email,
      },
    });
  } catch (error) {
    if (DEBUG) {
      console.error('Profile update error:', error);
    }
    res.status(500).json({ error: 'Internal server error' });
  }
};
