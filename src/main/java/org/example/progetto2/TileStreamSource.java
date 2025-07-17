package org.example.progetto2;

import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.msgpack.value.MapValue;
import org.msgpack.value.ValueFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Flink source operator that connects to the LOCAL-CHALLENGER API
 * and streams individual tile batches encoded in MessagePack format.
 */
public class TileStreamSource extends RichSourceFunction<TileBatch> {

    private final String apiUrl;   // Base API endpoint (here "http://localhost:8866")
    private final String benchId;  // Benchmark session ID
    private volatile boolean running = true;  // Flag to control execution lifecycle - immediately visible to all threads

    //Constructor takes API base URL and benchmark identifier.
    public TileStreamSource(String apiUrl, String benchId) {
        this.apiUrl = apiUrl;
        this.benchId = benchId;
    }

    /**
     * Main streaming loop — continuously fetches and emits tile batches.
     * Each batch contains print metadata and a TIFF image.
     */
    @Override
    public void run(SourceContext<TileBatch> ctx) throws Exception {
        while (running) {

            // Construct full API URL for next batch
            URL url = new URL(apiUrl + "/api/next_batch/" + benchId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Request data in binary MessagePack format
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/msgpack");

            // Handle HTTP status codes from the server
            int statusCode = connection.getResponseCode();

            if (statusCode == 404) {
                // Benchmark has ended — stop pulling data
                break;
            } else if (statusCode != 200) {
                // Temporary failure — wait briefly and retry
                Thread.sleep(100);
                continue;
            }

            // Read the binary MessagePack payload
            InputStream in = connection.getInputStream();
            byte[] rawBytes = in.readAllBytes();

            if (rawBytes.length == 0) {
                System.out.println("Empty response payload");
                continue;
            }

            in.close();

            // Decode binary payload using MessagePack
            MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(rawBytes);
            Value value = unpacker.unpackValue();
            MapValue map = value.asMapValue();
            Map<Value, Value> fields = map.map();

            // Extract individual fields from decoded map
            int batchId = fields.get(string("batch_id")).asIntegerValue().toInt();
            String printId = fields.get(string("print_id")).asStringValue().asString();
            int tileId = fields.get(string("tile_id")).asIntegerValue().toInt();
            byte[] tifData = fields.get(string("tif")).asBinaryValue().asByteArray();

            // Build tile object and emit it downstream
            TileBatch batch = new TileBatch(batchId, printId, tileId, tifData);
            ctx.collect(batch);

            // Optional pacing
            // Thread.sleep(10);
        }
    }

    //When the Flink job is canceled, stop the streaming loop.
    @Override
    public void cancel() {
        running = false;
    }

    //Shortcut method to create MessagePack string keys.
    private Value string(String s) {
        return ValueFactory.newString(s);
    }
}
