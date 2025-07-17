# Thermal-Outlier-Detection-with-Flink

This project follows query 1 and query 2 of the DEBS GRAND CHALLENGE 2025.

Both queries are executed and printed to JSON-files in one Flink Pipeline.

The Datastream is accessed via the DEBS REST-API. The initialization of the Benchmark-Stream is not included in the code.

It can be easily done via the commands:

curl -X POST http://localhost:8866/api/create ^
  -H "Content-Type: application/json" ^
  -d "{\"apitoken\":\"polimi-deib\",\"name\":\"unoptimized\",\"test\":true,\"max_batches\":50,\"queries\":[0]}"

The URL has to be adjusted depending on where API is hosted.
The value max_batches should be adjusted according to requested Datastream size (here set to 50).

And then with the retrieved benchmark-ID:

curl -X POST http://localhost:8866/api/start/<insert_benchmark-ID_here>

This benchmark-ID has to be copied in the Main.java in the corresponding field before the program is executed.
Each run requires a fresh benchmark-ID.
