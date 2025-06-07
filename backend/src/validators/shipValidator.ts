// src/validators/shipValidator.ts
import type { Ship } from '../models/types';

/**
 * Represents a cell on the game board
 */
interface Cell {
  x: number;
  y: number;
}

/**
 * Ship configuration for validation
 */
interface ShipConfig {
  length: number;
  count: number;
  name: string;
}

/**
 * Expected ship configuration for the game
 */
export const SHIP_CONFIGURATION: ShipConfig[] = [
  { length: 5, count: 1, name: 'Carrier' },
  { length: 4, count: 1, name: 'Battleship' },
  { length: 3, count: 1, name: 'Cruiser' },
  { length: 3, count: 1, name: 'Submarine' },
  { length: 2, count: 1, name: 'Destroyer' },
];

/**
 * Custom error class for ship validation errors.
 * Used to differentiate ship validation errors from other types of errors.
 */
export class ShipValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ShipValidationError';
  }
}

/**
 * Validates that a ship's coordinates are within the board bounds (0-9).
 * Throws a ShipValidationError if the coordinates are out of bounds.
 * @param ship - The ship to validate.
 */
function validateCoordinates(ship: Ship): void {
  if (ship.posX < 0 || ship.posX > 9 || ship.posY < 0 || ship.posY > 9) {
    throw new ShipValidationError(`Ship coordinates out of bounds: (${ship.posX}, ${ship.posY})`);
  }
}


/**
 * Validates that a ship fits within the board boundaries based on its position, length, and orientation.
 * Throws a ShipValidationError if the ship extends beyond the board.
 * @param ship - The ship to validate.
 */
function validateShipFitsOnBoard(ship: Ship): void {
  if (ship.isHorizontal) {
    if (ship.posX + ship.length > 10) {
      throw new ShipValidationError(
        `Ship extends beyond board horizontally: position (${ship.posX}, ${ship.posY}), length ${ship.length}`
      );
    }
  } else {
    if (ship.posY + ship.length > 10) {
      throw new ShipValidationError(
        `Ship extends beyond board vertically: position (${ship.posX}, ${ship.posY}), length ${ship.length}`
      );
    }
  }
}

/**
 * Validates the ship count and length configuration against the predefined SHIP_CONFIGURATION.
 * Throws a ShipValidationError if the configuration is incorrect.
 * @param ships - An array of ships to validate.
 */
function validateShipConfiguration(ships: Ship[]): void {
  if (ships.length !== 5) {
    throw new ShipValidationError(`Expected 5 ships, received ${ships.length}`);
  }

  const shipCounts: { [key: number]: number } = {};
  
  ships.forEach(ship => {
    shipCounts[ship.length] = (shipCounts[ship.length] || 0) + 1;
  });

  SHIP_CONFIGURATION.forEach(config => {
    const actual = shipCounts[config.length] || 0;
    if (actual !== config.count) {
      throw new ShipValidationError(
        `Expected ${config.count} ${config.name}(s) of length ${config.length}, got ${actual}`
      );
    }
  });
}

/**
 * Gets all cells occupied by a ship.
 * Calculates each cell based on the ship's starting position, length, and orientation.
 * @param ship - The ship for which to get the occupied cells.
 * @returns An array of Cell objects representing the occupied cells.
 */
function getShipCells(ship: Ship): Cell[] {
  const cells: Cell[] = [];

  for (let i = 0; i < ship.length; i++) {
    const cellX = ship.posX + (ship.isHorizontal ? i : 0);
    const cellY = ship.posY + (ship.isHorizontal ? 0 : i);
    cells.push({ x: cellX, y: cellY });
  }

  return cells;
}

/**
 * Validates that no ships in the provided array overlap with each other.
 * It checks each cell occupied by each ship to ensure it's not already occupied by another ship.
 * Throws a ShipValidationError if an overlap is detected.
 * @param ships - An array of ships to validate.
 */
function validateNoOverlaps(ships: Ship[]): void {
  const occupiedCells = new Map<number, Set<number>>();

  ships.forEach((ship, index) => {
    const shipCells = getShipCells(ship);

    shipCells.forEach(cell => {
      const xCells = occupiedCells.get(cell.x);
      if (xCells && xCells.has(cell.y)) {
        throw new ShipValidationError(`Ship ${index + 1} overlaps with another ship at position (${cell.x},${cell.y})`);
      }
      
      if (!occupiedCells.has(cell.x)) {
        occupiedCells.set(cell.x, new Set<number>());
      }
      occupiedCells.get(cell.x)!.add(cell.y);
    });
  });
}

/**
 * Validates an array of ships according to all game rules.
 * This function orchestrates all individual and fleet-wide validation checks.
 * It first validates the structure of each ship, then performs individual checks like coordinates, length, and board fit.
 * Finally, it performs fleet-wide checks like configuration and overlaps.
 * Throws a ShipValidationError if any validation fails.
 * @param ships - An array of unknown objects, expected to be ships, to validate.
 * @returns An array of validated Ship objects if all validations pass.
 */
export function validateShips(ships: unknown[]): Ship[] {
  // Type validation
  if (!Array.isArray(ships)) {
    throw new ShipValidationError('Ships data must be an array');
  }

  const validatedShips: Ship[] = [];

  ships.forEach((ship, index) => {
    // Basic structural validation using type assertion
    // More specific checks (e.g. value ranges) are done by individual validation functions
    const potentialShip = ship as Ship;
    if (
      typeof potentialShip !== 'object' ||
      potentialShip === null ||
      typeof potentialShip.posX !== 'number' ||
      typeof potentialShip.posY !== 'number' ||
      typeof potentialShip.length !== 'number' ||
      typeof potentialShip.isHorizontal !== 'boolean'
    ) {
      throw new ShipValidationError(
        `Ship ${index + 1} has invalid structure. Expected: { posX: number, posY: number, length: number, isHorizontal: boolean }`
      );
    }
    validatedShips.push(potentialShip);
  });

  // Individual ship validations
  validatedShips.forEach((ship, index) => {
    try {
      validateCoordinates(ship);
      validateShipFitsOnBoard(ship);
    } catch (error) {
      if (error instanceof ShipValidationError) {
        throw new ShipValidationError(`Ship ${index + 1}: ${error.message}`);
      }
      throw error;
    }
  });

  // Fleet-wide validations
  validateShipConfiguration(validatedShips);
  validateNoOverlaps(validatedShips);
  return validatedShips;
}
