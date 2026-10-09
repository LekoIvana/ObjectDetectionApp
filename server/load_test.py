import argparse
import asyncio
import csv
import json
import statistics
import time
from pathlib import Path

import websockets


async def virtual_client(client_id, uri, image_bytes, duration, results):
    latencies = []
    inference_times = []
    errors = 0
    frames = 0

    try:
        async with websockets.connect(
            uri,
            open_timeout=10,
            ping_interval=20,
            ping_timeout=20,
            max_size=None
        ) as websocket:

            start_test = time.perf_counter()
            request_id = 0

            while time.perf_counter() - start_test < duration:
                request_id += 1

                start_frame = time.perf_counter()

                await websocket.send(image_bytes)
                response = await websocket.recv()

                end_frame = time.perf_counter()

                latency_ms = (end_frame - start_frame) * 1000

                try:
                    data = json.loads(response)

                    if "error" in data:
                        errors += 1
                        continue

                    inference_ms = float(
                        data.get("inference_time_ms", 0)
                    )

                    latencies.append(
                        (request_id, latency_ms)
                    )

                    inference_times.append(
                        (request_id, inference_ms)
                    )

                    frames += 1

                except Exception:
                    errors += 1

    except Exception as exception:
        print(f"Klijent {client_id}: greška: {exception}")
        errors += 1

    results[client_id] = {
        "frames": frames,
        "errors": errors,
        "latencies": latencies,
        "inference_times": inference_times
    }


def percentile(values, percentile_value):
    if not values:
        return 0.0

    ordered = sorted(values)
    index = int(
        round(
            (percentile_value / 100)
            * (len(ordered) - 1)
        )
    )

    return ordered[index]


async def run_test(clients, duration, uri, image_path):
    image_bytes = Path(image_path).read_bytes()

    print()
    print("POČETAK TESTA")
    print(f"Broj klijenata: {clients}")
    print(f"Trajanje: {duration} s")
    print(f"Slika: {image_path}")
    print(f"WebSocket: {uri}")
    print(f"Veličina JPEG-a: {len(image_bytes)} bytes")
    print()

    results = {}

    test_start = time.perf_counter()

    tasks = [
        asyncio.create_task(
            virtual_client(
                client_id,
                uri,
                image_bytes,
                duration,
                results
            )
        )
        for client_id in range(1, clients + 1)
    ]

    await asyncio.gather(*tasks)

    total_duration = time.perf_counter() - test_start

    all_latencies = []
    all_inference_times = []
    individual_latencies = []

    total_frames = 0
    total_errors = 0

    for client_id, result in results.items():
        total_frames += result["frames"]
        total_errors += result["errors"]

        for request_id, latency in result["latencies"]:
            all_latencies.append(latency)

            individual_latencies.append([
                clients,
                client_id,
                request_id,
                round(latency, 2)
            ])

        for request_id, inference in result["inference_times"]:
            all_inference_times.append(inference)

    if all_latencies:
        avg_latency = statistics.mean(all_latencies)
        median_latency = statistics.median(all_latencies)
        min_latency = min(all_latencies)
        max_latency = max(all_latencies)
        p95_latency = percentile(all_latencies, 95)
    else:
        avg_latency = 0
        median_latency = 0
        min_latency = 0
        max_latency = 0
        p95_latency = 0

    if all_inference_times:
        avg_inference = statistics.mean(all_inference_times)
    else:
        avg_inference = 0

    throughput = (
        total_frames / total_duration
        if total_duration > 0
        else 0
    )

    print("REZULTATI")
    print(f"Klijenti: {clients}")
    print(f"Obrađeni frameovi: {total_frames}")
    print(f"Greške: {total_errors}")
    print(f"Stvarno trajanje testa: {total_duration:.2f} s")
    print(f"Throughput: {throughput:.3f} frame/s")
    print(f"Prosječna latencija: {avg_latency:.2f} ms")
    print(f"Medijan latencije: {median_latency:.2f} ms")
    print(f"P95 latencija: {p95_latency:.2f} ms")
    print(f"Minimalna latencija: {min_latency:.2f} ms")
    print(f"Maksimalna latencija: {max_latency:.2f} ms")
    print(f"Prosječni inference: {avg_inference:.2f} ms")

    output_file = "load_test_results.csv"

    file_exists = Path(output_file).exists()

    with open(output_file, "a", newline="") as file:
        writer = csv.writer(file)

        if not file_exists:
            writer.writerow([
                "clients",
                "duration_s",
                "total_frames",
                "errors",
                "throughput_fps",
                "avg_latency_ms",
                "median_latency_ms",
                "p95_latency_ms",
                "min_latency_ms",
                "max_latency_ms",
                "avg_inference_ms"
            ])

        writer.writerow([
            clients,
            round(total_duration, 2),
            total_frames,
            total_errors,
            round(throughput, 3),
            round(avg_latency, 2),
            round(median_latency, 2),
            round(p95_latency, 2),
            round(min_latency, 2),
            round(max_latency, 2),
            round(avg_inference, 2)
        ])

    latency_file = "load_test_latencies.csv"

    latency_file_exists = Path(latency_file).exists()

    with open(latency_file, "a", newline="") as file:
        writer = csv.writer(file)

        if not latency_file_exists:
            writer.writerow([
                "clients",
                "client_id",
                "request_id",
                "latency_ms"
            ])

        writer.writerows(individual_latencies)

    print()
    print(f"Sažeti rezultat spremljen u {output_file}")
    print(f"Pojedinačne latencije spremljene u {latency_file}")


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--clients",
        type=int,
        required=True
    )

    parser.add_argument(
        "--duration",
        type=int,
        default=60
    )

    parser.add_argument(
        "--uri",
        default="ws://127.0.0.1:8000/ws/detect"
    )

    parser.add_argument(
        "--image",
        default="bus.jpg"
    )

    args = parser.parse_args()

    asyncio.run(
        run_test(
            args.clients,
            args.duration,
            args.uri,
            args.image
        )
    )


if __name__ == "__main__":
    main()
