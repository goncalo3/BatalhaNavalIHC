package pt.goncalo3.batalhanaval;

/**
 * Represents a ship in the Battleship game
 */
public class Ship {
    private int id;
    private int posX;
    private int posY;
    private int length;
    private boolean isHorizontal;
    private boolean[] hits;  // Track which segments are hit

    /**
     * Create a new ship
     * @param id Unique identifier for the ship
     * @param posX X-coordinate of the ship's starting position
     * @param posY Y-coordinate of the ship's starting position
     * @param length Length of the ship
     * @param isHorizontal Whether the ship is placed horizontally
     */
    public Ship(int id, int posX, int posY, int length, boolean isHorizontal) {
        this.id = id;
        this.posX = posX;
        this.posY = posY;
        this.length = length;
        this.isHorizontal = isHorizontal;
        this.hits = new boolean[length];
    }

    /**
     * Check if the ship is at the given coordinates
     * @param x X-coordinate to check
     * @param y Y-coordinate to check
     * @return True if the ship occupies the coordinates
     */
    public boolean isAt(int x, int y) {
        if (isHorizontal) {
            return y == posY && x >= posX && x < posX + length;
        } else {
            return x == posX && y >= posY && y < posY + length;
        }
    }

    /**
     * Register a hit on the ship at the given coordinates
     * @param x X-coordinate of the hit
     * @param y Y-coordinate of the hit
     * @return True if the hit was registered, false if coordinates don't match
     */
    public boolean hit(int x, int y) {
        if (!isAt(x, y)) {
            return false;
        }

        int segment;
        if (isHorizontal) {
            segment = x - posX;
        } else {
            segment = y - posY;
        }

        if (segment >= 0 && segment < length) {
            hits[segment] = true;
            return true;
        }

        return false;
    }

    /**
     * Check if the ship is destroyed (all segments hit)
     * @return True if all segments are hit
     */
    public boolean isDestroyed() {
        for (boolean hit : hits) {
            if (!hit) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the ship's ID
     * @return The ship's ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get the ship's X-coordinate
     * @return The ship's X-coordinate
     */
    public int getPosX() {
        return posX;
    }

    /**
     * Get the ship's Y-coordinate
     * @return The ship's Y-coordinate
     */
    public int getPosY() {
        return posY;
    }

    /**
     * Get the ship's length
     * @return The ship's length
     */
    public int getLength() {
        return length;
    }

    /**
     * Check if the ship is placed horizontally
     * @return True if horizontal, false if vertical
     */
    public boolean isHorizontal() {
        return isHorizontal;
    }

    /**
     * Get the hit status array
     * @return Array showing which segments are hit
     */
    public boolean[] getHits() {
        return hits;
    }
}
