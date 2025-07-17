package org.example.progetto2;

// Flink stream execution and data handling
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.io.PrintWriter;
import java.util.*;

public class Main {

    // Buffers to store output results from Query 1 and Query 2
    // These are synchronized so multiple threads can write safely
    private static final List<Query1Output> query1Buffer = Collections.synchronizedList(new ArrayList<>());
    private static final List<Query2Output> query2Buffer = Collections.synchronizedList(new ArrayList<>());

    // Timestamps to track latency measurements
    private static long query1EndTime = -1;
    private static long query2StartTime = -1;
    private static long query2EndTime = -1;

    public static void main(String[] args) throws Exception {
        // REST API parameters for ingesting data from the LOCAL-CHALLENGER container
        String apiUrl = "http://localhost:8866";
        String benchId = "01K09JG0080VBVXEZAGQX0NAYM"; // Replace when needed to start a fresh benchmark

        // Start measuring total job time
        long startTime = System.currentTimeMillis();

        // Set up Flink execution environment and configure parallelism
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(16);  // Amount of parallel subtasks - set for every operator

        // Ingest the tile stream: each tile contains thermal data from a small portion of the printed layer
        DataStream<TileBatch> tiles = env.addSource(new TileStreamSource(apiUrl, benchId));

        // Query 1: count saturated pixels per tile
        DataStream<Query1Output> results = tiles.map(new Query1Processor());

        // Query 2: sliding-window outlier detection based on temperature deviation
        DataStream<Query2Output> outliers = results
                .keyBy(q -> q.printId + "_" + q.tileId) // Partition by tile location
                .process(new Query2Processor());        // Perform window-based deviation analysis
        outliers.print("Query 2 Output");               // Optional: printing result stream

        // Query 1 sink: collect results and store latency and throughput metrics
        results.addSink(new SinkFunction<Query1Output>() {
            @Override
            public void invoke(Query1Output value, Context context) {
                query1EndTime = System.currentTimeMillis(); // Record when last element processed
                query1Buffer.add(value);                   // Store the result
            }
        });

        // Query 2 sink: as above - tracks performance and collect results
        outliers.addSink(new SinkFunction<Query2Output>() {
            @Override
            public void invoke(Query2Output value, Context context) {
                if (query2StartTime == -1) {
                    query2StartTime = System.currentTimeMillis(); // Start when first outlier arrives
                }
                query2EndTime = System.currentTimeMillis();       // Update on each new output
                query2Buffer.add(value);                          // Collect result
            }
        });

        // Trigger job execution (Flink runs everything from here on)
        env.execute("L-PBF Monitoring: Query1 Only");

        // Post-processing: compute latency and throughput for Query 1
        long endTime = System.currentTimeMillis();
        long totalJobTime = endTime - startTime;
        long query1Latency = query1EndTime - startTime;
        double query1Throughput = query1Buffer.size() / (query1Latency / 1000.0);

        // Sort Query 1 results for final output (by sequence ID)
        Collections.sort(query1Buffer, Comparator.comparingInt(Query1Output::getSeqId));
        try (PrintWriter writer = new PrintWriter(new java.io.FileWriter("query1_results.json", true), true)) {
            // Optional: store individual results
            // for (Query1Output result: query1Buffer) {
            //     writer.println(result.toJson());
            // }

            // Save performance metrics
            writer.println(String.format(
                    "{\"latency_ms\": %d, \"throughput_tiles_per_sec\": %.2f}",
                    query1Latency, query1Throughput
            ));
        }

        // Process and store Query 2 results and performance metrics
        Collections.sort(query2Buffer, Comparator.comparingInt(q -> Integer.parseInt(q.seqId)));
        long query2Latency = query2EndTime - query2StartTime;
        double query2Throughput = query2Buffer.size() / (query2Latency / 1000.0);
        try (PrintWriter writer = new PrintWriter(new java.io.FileWriter("query2_results.json", true), true)) {
            // Optional: store individual results
            // for (Query2Output result: query2Buffer) {
            //     writer.println(result.toJson());
            // }

            // Save performance metrics for Query 2 and total time
            writer.println(String.format(
                    "{\"latency_ms\": %d, \"throughput_tiles_per_sec\": %.2f, \"total_job_time_ms\": %d}",
                    query2Latency, query2Throughput, totalJobTime
            ));
        }
    }
}
