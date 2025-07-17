package org.example.progetto2;

import java.util.List;

/**
 * Represents the processed result of Query 1,
 * summarizing a tile’s saturated pixel count and valid temperature readings.
 */
public class Query1Output {

    // Unique identifier for this tile’s sequence in the incoming data stream
    public String seqId;

    // Identifier for the entire print job
    public String printId;

    // Position of the tile within the current layer (from 0 to 15)
    public int tileId;

    // Original batch number
    public int batchId;

    // Number of saturated pixels in this tile
    public int saturated;

    // List of all valid pixels (above empty threshold but below saturation limit)
    public List<ValidPixel> validPixels;

    /**
     * Converts this output to JSON format for file export or integration with the benchmark tools.
     * Only basic metadata and saturation count are included.
     */
    public String toJson() {
        return String.format(
                "{\"seq_id\": %s, \"print_id\": \"%s\", \"tile_id\": %d, \"saturated\": %d}",
                seqId, printId, tileId, saturated
        );
    }

    //Returns the sequence ID as an integer for sorting purposes.
    public int getSeqId() {
        return Integer.parseInt(seqId);
    }

    //Full constructor to initialize all fields.
    public Query1Output(String seqId, String printId, int tileId, int batchId, int saturated, List<ValidPixel> validPixels) {
        this.seqId = seqId;
        this.printId = printId;
        this.tileId = tileId;
        this.batchId = batchId;
        this.saturated = saturated;
        this.validPixels = validPixels;
    }

    //Returns a readable string version — useful for logging or debugging
    @Override
    public String toString() {
        return String.format("batch=%d (seq=%s), print=%s, tile=%d, saturated=%d, valid_pixels=%d",
                batchId, seqId, printId, tileId, saturated, validPixels.size());
    }
}
