// src/db/connection.ts
import mysql from 'mysql2/promise';
import { DB_CONFIG, DEBUG } from '../config/env';

let connection: mysql.Connection | null = null;

/**
 * Initialize database connection
 */
export const initializeDatabase = async (): Promise<mysql.Connection> => {
  try {
    connection = await mysql.createConnection({
      host: DB_CONFIG.host,
      port: DB_CONFIG.port,
      user: DB_CONFIG.user,
      password: DB_CONFIG.password,
      database: DB_CONFIG.database,
    });

    if (DEBUG) {
      console.log('✅ Database connected successfully');
    }

    return connection;
  } catch (error) {
    console.error('❌ Database connection failed:', error);
    throw error;
  }
};

/**
 * Get the current database connection
 */
export const getConnection = (): mysql.Connection => {
  if (!connection) {
    throw new Error('Database not initialized. Call initializeDatabase() first.');
  }
  return connection;
};

/**
 * Close database connection
 */
export const closeConnection = async (): Promise<void> => {
  if (connection) {
    await connection.end();
    connection = null;
    if (DEBUG) {
      console.log('Database connection closed');
    }
  }
};

/**
 * Execute a query with error handling
 */
export const executeQuery = async <T>(
  query: string,
  params: any[] = []
): Promise<T> => {
  const conn = getConnection();
  try {
    const [rows] = await conn.execute(query, params);
    return rows as T;
  } catch (error) {
    if (DEBUG) {
      console.error('Query execution failed:', query, params, error);
    }
    throw error;
  }
};
