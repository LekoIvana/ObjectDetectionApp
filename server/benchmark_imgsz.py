import csv
import statistics
import time
from pathlib import Path

import cv2
from ultralytics import YOLO


MODEL_PATH = "yolov8n.pt"
IMAGE_DIR = Path("benchmark_images")
SIZES = [320, 416]
REPETITIONS = 3
PASSES = 13
WARMUP_PASSES = 1

model = YOLO(MODEL_PATH)

image_paths = sorted(
    list(IMAGE_DIR.glob("*.jpg")) +
    list(IMAGE_DIR.glob("*.jpeg")) +
    list(IMAGE_DIR.glob("*.png"))
)

if not image_paths:
    raise FileNotFoundError("Nema slika u benchmark_images.")

images = []

for image_path in image_paths:
    image = cv2.imread(str(image_path))

    if image is None:
        raise ValueError(f"Slika se ne može učitati: {image_path}")

    rotated_image = cv2.rotate(
        image,
        cv2.ROTATE_90_CLOCKWISE
    )

    images.append((image_path.name, rotated_image))


def percentile(values, p):
    ordered = sorted(values)
    position = (len(ordered) - 1) * p / 100
    lower = int(position)
    upper = min(lower + 1, len(ordered) - 1)
    fraction = position - lower
    return ordered[lower] + (ordered[upper] - ordered[lower]) * fraction


raw_rows = []
summary_rows = []

print(f"Broj ulaznih slika: {len(images)}")
print(f"Mjerenja po ponavljanju: {len(images) * PASSES}")

for imgsz in SIZES:
    for repetition in range(1, REPETITIONS + 1):

        print(f"\nimgsz={imgsz}, ponavljanje={repetition}")
        print("Warm-up...")

        for _ in range(WARMUP_PASSES):
            for _, image in images:
                model(
                    image,
                    imgsz=imgsz,
                    conf=0.50,
                    iou=0.45,
                    max_det=10,
                    verbose=False
                )

        times = []
        measurement = 0

        test_start = time.perf_counter()

        for pass_number in range(1, PASSES + 1):
            for image_name, image in images:

                measurement += 1
                start = time.perf_counter()

                model(
                    image,
                    imgsz=imgsz,
                    conf=0.50,
                    iou=0.45,
                    max_det=10,
                    verbose=False
                )

                inference_ms = (time.perf_counter() - start) * 1000
                times.append(inference_ms)

                raw_rows.append([
                    imgsz,
                    repetition,
                    pass_number,
                    measurement,
                    image_name,
                    inference_ms
                ])

        duration = time.perf_counter() - test_start

        mean_value = statistics.mean(times)
        median_value = statistics.median(times)
        p95_value = percentile(times, 95)
        std_value = statistics.stdev(times)
        min_value = min(times)
        max_value = max(times)

        summary_rows.append([
            imgsz,
            repetition,
            len(times),
            duration,
            mean_value,
            median_value,
            p95_value,
            std_value,
            min_value,
            max_value
        ])

        print(f"Broj mjerenja: {len(times)}")
        print(f"Trajanje: {duration:.2f} s")
        print(f"Prosjek: {mean_value:.2f} ms")
        print(f"Medijan: {median_value:.2f} ms")
        print(f"P95: {p95_value:.2f} ms")
        print(f"Standardna devijacija: {std_value:.2f} ms")
        print(f"Min: {min_value:.2f} ms")
        print(f"Max: {max_value:.2f} ms")

with open("benchmark_imgsz_raw.csv", "w", newline="") as file:
    writer = csv.writer(file)
    writer.writerow([
        "imgsz",
        "repetition",
        "pass",
        "measurement",
        "image",
        "inference_ms"
    ])
    writer.writerows(raw_rows)

with open("benchmark_imgsz_summary.csv", "w", newline="") as file:
    writer = csv.writer(file)
    writer.writerow([
        "imgsz",
        "repetition",
        "measurements",
        "duration_s",
        "mean_ms",
        "median_ms",
        "p95_ms",
        "std_ms",
        "min_ms",
        "max_ms"
    ])
    writer.writerows(summary_rows)

print("\nRezultati spremljeni u:")
print("benchmark_imgsz_raw.csv")
print("benchmark_imgsz_summary.csv")
