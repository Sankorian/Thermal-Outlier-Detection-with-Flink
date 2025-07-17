package org.example.progetto2;

import java.util.List;

/**
 * Represents the result of Query 2 — containing the top 5 outlier pixels
 * for a given tile, based on local temperature deviation across a sliding window of layers.
 */
public class Query2Output {

    // Unique sequence ID for the tile in the input stream
    public final String seqId;

    // Identifier for the print job
    public final String printId;

    // Index of the tile within the current layer (0 to 15)
    public final int tileId;

    // List of up to 5 deviation results (outlier points) from the tile
    public final List<DeviationResult> topOutliers;

    //Constructor to initialize all metadata and detected outliers.
    public Query2Output(String seqId, String printId, int tileId, List<DeviationResult> topOutliers) {
        this.seqId = seqId;
        this.printId = printId;
        this.tileId = tileId;
        this.topOutliers = topOutliers;
    }

    //When printing this object, using its JSON representation.
    @Override
    public String toString() {
        return toJson();
    }

    /**
     * Converts the Query2Output to a JSON string, matching the format required by the benchmark.
     * Includes coordinates and deviation scores of each outlier point.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("{\"seq_id\": \"%s\", \"print_id\": \"%s\", \"tile_id\": %d", seqId, printId, tileId));

        int i = 1;
        for (DeviationResult dr : topOutliers) {
            sb.append(String.format(", \"P%d\": [%d, %d], \"δP%d\": %.2f", i, dr.x, dr.y, i, dr.delta));
            i++;
        }

        sb.append("}");
        return sb.toString();
    }
}
