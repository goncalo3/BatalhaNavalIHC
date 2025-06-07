// src/middleware/auth.ts
import jwt from 'jsonwebtoken';
import type { Request, Response, NextFunction } from 'express';
import { JWT_CONFIG, DEBUG } from '../config/env';
import { UserModel } from '../db/models/User';

// Extend Express Request interface to include user
declare global {
  namespace Express {
    interface Request {
      user?: {
        id: number;
        username: string;
        email: string;
      };
    }
  }
}

export interface JWTPayload {
  userId: number;
  username: string;
  email: string;
}

/**
 * Generate JWT token for a user
 */
export const generateToken = (user: { id: number; username: string; email: string }): string => {
  const payload: JWTPayload = {
    userId: user.id,
    username: user.username,
    email: user.email,
  };

  return jwt.sign(payload, JWT_CONFIG.secret, {
    expiresIn: JWT_CONFIG.expiresIn,
  } as jwt.SignOptions);
};

/**
 * Verify JWT token and extract user information
 */
export const verifyToken = (token: string): JWTPayload => {
  try {
    return jwt.verify(token, JWT_CONFIG.secret) as JWTPayload;
  } catch (error) {
    throw new Error('Invalid token');
  }
};

/**
 * Middleware to authenticate requests using JWT
 */
export const authenticateToken = async (
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> => {
  try {
    const authHeader = req.headers.authorization;
    const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

    if (!token) {
      res.status(401).json({ error: 'Access token required' });
      return;
    }

    const payload = verifyToken(token);
    
    // Verify user still exists in database
    const user = await UserModel.findById(payload.userId);
    if (!user) {
      res.status(401).json({ error: 'User not found' });
      return;
    }

    // Attach user info to request
    req.user = {
      id: user.id,
      username: user.username,
      email: user.email,
    };

    next();
  } catch (error) {
    if (DEBUG) {
      console.error('Authentication error:', error);
    }
    res.status(403).json({ error: 'Invalid or expired token' });
  }
};
