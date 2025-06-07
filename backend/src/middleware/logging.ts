import type { Request, Response, NextFunction } from 'express';
import { DEBUG } from '../config/env';

/**
 * HTTP Request/Response Logging Middleware
 */
export const httpLogger = (req: Request, res: Response, next: NextFunction) => {
  if (!DEBUG) {
    return next();
  }

  // Define emojis for different response statuses
  const statusEmojis: { [key: number]: string } = {
    2: '🟢',  // Successful (2xx)
    3: '🟠',  // Redirection (3xx)
    4: '🟡',  // Client Error (4xx)
    5: '🔴',  // Server Error (5xx)
  };

  // Get emoji based on the status code
  const getEmoji = (statusCode: number) => {
    const statusCategory = Math.floor(statusCode / 100); // Get the first digit of the status code
    return statusEmojis[statusCategory] || '⚪'; // Default to white if no category found
  };

  // Log incoming request
  console.log(`🔵 [REQUEST] ${req.method} ${req.originalUrl}`);

  // Capture the original response methods
  const originalSend = res.send;
  const originalJson = res.json;
  const originalEnd = res.end;

  // Override res.send, res.json, and res.end to capture response
  let responseBody: any = null;
  res.send = function(data: any) {
    responseBody = data;
    return originalSend.call(this, data);
  };
  res.json = function(data: any) {
    responseBody = data;
    return originalJson.call(this, data);
  };
  res.end = function(chunk?: any, encoding?: any) {
    if (chunk && !responseBody) {
      responseBody = chunk;
    }
    return originalEnd.call(this, chunk, encoding);
  };

  // Log response when finished
  res.on('finish', () => {
    const emoji = getEmoji(res.statusCode);
    const statusMessage = res.statusCode ? res.statusCode.toString() : 'Unknown';
    console.log(`**${emoji} [RESPONSE] ${req.method} ${req.originalUrl} ${statusMessage}**`);
  });

  next();
};
