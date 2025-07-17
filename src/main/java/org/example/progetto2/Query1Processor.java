package org.example.progetto2;

import org.apache.flink.api.common.functions.MapFunction;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Flink operator for Query 1:
 * Processes the TIFF image inside each tile to detect saturated pixels and extract valid thermal points.
 */
public class Query1Processor implements MapFunction<TileBatch, Query1Output> {

    // Threshold for filtering out "empty" pixels — anything below this is ignored
    private static final int EMPTY_THRESHOLD = 5000;

    // Temperature value above which a pixel is considered saturated (possibly shows defect)
    private static final int SATURATION_THRESHOLD = 65000;

    /**
     * Main mapping function — processes each incoming TileBatch
     * and produces a structured Query1Output for further analysis.
     */
    @Override
    public Query1Output map(TileBatch batch) throws Exception {

        // Convert the raw TIFF image bytes into an actual image object
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(batch.tiffImage));

        int width = image.getWidth();    // Image width (pixels)
        int height = image.getHeight();  // Image height (pixels)

        List<ValidPixel> validPixels = new ArrayList<>(); // Store all pixels with good thermal data for future analysis
        int saturated = 0;               // Count how many pixels are saturated

        // Raster gives direct access to pixel values in the image
        Raster raster = image.getRaster();

        // Scan each pixel in the 2D image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int temp = raster.getSample(x, y, 0);  // Read temperature value from TIFF image

                if (temp > SATURATION_THRESHOLD) {
                    // Temperature too high — considered saturated (likely defect)
                    saturated++;
                } else if (temp >= EMPTY_THRESHOLD) {
                    // Temperature in a usable range — add to valid list
                    validPixels.add(new ValidPixel(x, y, temp));
                }
                // Pixels below EMPTY_THRESHOLD are ignored — treated as empty space
            }
        }

        // Using the batch ID as a unique sequence identifier for this tile to match requested query 1 output
        String seqId = String.valueOf(batch.batchId);

        // Build the result object with metadata and extracted pixel values
        return new Query1Output(
                seqId,            // Unique sequence ID
                batch.printId,    // Print job identifier
                batch.tileId,     // Tile position in layer
                batch.batchId,    // Original batch ID
                saturated,        // Number of saturated pixels detected
                validPixels       // List of temperature-valid pixels for future processing
        );
    }
}
