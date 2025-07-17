package org.example.progetto2;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query 2 Processor — identifies thermal outliers based on temperature deviation
 * using a sliding window across the last 3 layers (vertically stacked)
 */
public class Query2Processor extends KeyedProcessFunction<String, Query1Output, Query2Output> {

    // State to hold the sliding window of most recent layers (max size = 3)
    private transient ListState<Query1Output> layerWindow;

    //Called once before processing starts — so Flink can track window data.
    @Override
    public void open(Configuration parameters) {
        ListStateDescriptor<Query1Output> descriptor =
                new ListStateDescriptor<>("layerWindow", Query1Output.class);
        layerWindow = getRuntimeContext().getListState(descriptor);
    }

    /**
     * Called for each incoming tile (Query1Output). Maintains a window
     * of the last 3 layers per tileId and analyzes temperature deviations.
     */
    @Override
    public void processElement(Query1Output current, Context context, Collector<Query2Output> out) throws Exception {

        // Restore current state of the sliding window
        List<Query1Output> window = new ArrayList<>();
        for (Query1Output tile : layerWindow.get()) {
            window.add(tile);
        }

        // Add the new tile to the window
        window.add(current);

        // Keep only the last 3 layers
        if (window.size() > 3) {
            window.remove(0);
        }

        // Save updated window back to Flink state
        layerWindow.update(window);

        int depth = window.size(); // Used to compute dz distance in 3D space (vertical)
        List<DeviationResult> deviations = new ArrayList<>();

        // For each pixel in the newest layer
        for (ValidPixel p : current.validPixels) {

            List<Integer> innerTemps = new ArrayList<>(); // 0 ≤ d ≤ 2
            List<Integer> outerTemps = new ArrayList<>(); // 2 < d ≤ 4

            // Scan all pixels across all layers in the window
            for (int layerIndex = 0; layerIndex < depth; layerIndex++) {
                Query1Output tile = window.get(layerIndex);

                for (ValidPixel neighbor : tile.validPixels) {
                    int dx = Math.abs(neighbor.x - p.x);
                    int dy = Math.abs(neighbor.y - p.y);

                    // Vertical distance from latest layer (dz = 0 means same layer) - depth here: 3
                    int dz = depth - 1 - layerIndex;

                    // Combined distance in 3D space
                    int dManhattan = dx + dy + dz;

                    if (dManhattan <= 2) {
                        innerTemps.add(neighbor.temp);
                    } else if (dManhattan <= 4) {
                        outerTemps.add(neighbor.temp);
                    }
                }
            }

            // Only calculate deviation if both inner and outer neighborhoods are populated
            if (!innerTemps.isEmpty() && !outerTemps.isEmpty()) {
                double avgInner = innerTemps.stream().mapToInt(i -> i).average().orElse(0);
                double avgOuter = outerTemps.stream().mapToInt(i -> i).average().orElse(0);
                double delta = Math.abs(avgInner - avgOuter);

                // Threshold to qualify as an outlier
                if (delta > 6000) {
                    deviations.add(new DeviationResult(p.x, p.y, delta));
                }
            }
        }

        // Sort by deviation score and select top 5
        deviations.sort((a, b) -> Double.compare(b.delta, a.delta));
        List<DeviationResult> top5 = deviations.stream().limit(5).collect(Collectors.toList());

        // Emit the top 5 outlier points for this tile
        out.collect(new Query2Output(
                current.seqId,
                current.printId,
                current.tileId,
                top5
        ));
    }
}
