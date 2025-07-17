package org.example.progetto2;

/**
 * Represents a pixel flagged as a thermal outlier in Query 2.
 * Each result contains the pixel's coordinates and the magnitude of its deviation.
 */
public class DeviationResult {

    // X-coordinate of the pixel in the tile
    public final int x;

    // Y-coordinate of the pixel in the tile
    public final int y;

    // Temperature deviation score compared to surrounding neighborhood
    public final double delta;

    //Constructs a new deviation result
    public DeviationResult(int x, int y, double delta) {
        this.x = x;
        this.y = y;
        this.delta = delta;
    }
}
