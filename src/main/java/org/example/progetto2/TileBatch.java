package org.example.progetto2;

/**
 * Represents a single tile of a printed layer in the L-PBF process.
 * Each tile contains the temperature data as a TIFF image.
 */
public class TileBatch {

    // Sequential ID for tracking the batch across the input stream
    public int batchId;

    // Identifier for the object being printed (for the time the stream was tested it remained constant)
    public String printId;

    // ID of the tile within the current layer (ranges from 0 to 15)
    public int tileId;

    // Binary data for the tile, encoded as a 16-bit TIFF image
    public byte[] tiffImage;

    //Constructor for creating a new TileBatch object.
    public TileBatch(int batchId, String printId, int tileId, byte[] tiffImage) {
        this.batchId = batchId;
        this.printId = printId;
        this.tileId = tileId;
        this.tiffImage = tiffImage;
    }
}
