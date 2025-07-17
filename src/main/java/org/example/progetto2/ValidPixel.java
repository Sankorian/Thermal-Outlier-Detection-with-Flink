package org.example.progetto2;

/**
 * Represents a valid pixel in the thermal image —
 * meaning: not empty, not saturated, and safe to analyze further.
 */
public class ValidPixel {

    // X-coordinate of the pixel within the tile image
    public final int x;

    // Y-coordinate of the pixel within the tile image
    public final int y;

    // Temperature value recorded at this pixel (16-bit from TIFF)
    public final int temp;

   //Creates a pixel object to be used in further queries.
    public ValidPixel(int x, int y, int temp) {
        this.x = x;
        this.y = y;
        this.temp = temp;
    }
}
