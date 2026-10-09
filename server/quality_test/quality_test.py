import os
import csv
import cv2
from ultralytics import YOLO

IMAGE_DIR = "images"
LABEL_DIR = "labels"
MODEL_PATH = os.path.expanduser("~/diplomski-object-detection/yolov8n.pt")

SIZES = [320, 416]
CONF = 0.50
IOU_THRESHOLD = 0.50
MAX_DET = 10

TARGET_CLASSES = {39, 46, 56, 57,  63, 77}

model = YOLO(MODEL_PATH)


def load_ground_truth(label_path, width, height):
    boxes = []

    with open(label_path, "r") as f:
        for line in f:
            parts = line.strip().split()

            if len(parts) != 5:
                continue

            class_id = int(parts[0])
            xc, yc, bw, bh = map(float, parts[1:])

            x1 = (xc - bw / 2) * width
            y1 = (yc - bh / 2) * height
            x2 = (xc + bw / 2) * width
            y2 = (yc + bh / 2) * height

            boxes.append({
                "class_id": class_id,
                "box": [x1, y1, x2, y2]
            })

    return boxes


def calculate_iou(box1, box2):
    x1 = max(box1[0], box2[0])
    y1 = max(box1[1], box2[1])
    x2 = min(box1[2], box2[2])
    y2 = min(box1[3], box2[3])

    intersection = max(0, x2 - x1) * max(0, y2 - y1)

    area1 = max(0, box1[2] - box1[0]) * max(0, box1[3] - box1[1])
    area2 = max(0, box2[2] - box2[0]) * max(0, box2[3] - box2[1])

    union = area1 + area2 - intersection

    if union == 0:
        return 0

    return intersection / union


def evaluate(ground_truth, predictions):
    matched_gt = set()
    matched_pred = set()

    matches = []

    possible_matches = []

    for p_idx, pred in enumerate(predictions):
        for g_idx, gt in enumerate(ground_truth):
            if pred["class_id"] != gt["class_id"]:
                continue

            iou = calculate_iou(pred["box"], gt["box"])

            if iou >= IOU_THRESHOLD:
                possible_matches.append((iou, p_idx, g_idx))

    possible_matches.sort(reverse=True)

    for iou, p_idx, g_idx in possible_matches:
        if p_idx in matched_pred or g_idx in matched_gt:
            continue

        matched_pred.add(p_idx)
        matched_gt.add(g_idx)
        matches.append(iou)

    tp = len(matches)
    fp = len(predictions) - tp
    fn = len(ground_truth) - tp

    return tp, fp, fn


raw_results = []

images = sorted([
    f for f in os.listdir(IMAGE_DIR)
    if f.lower().endswith((".jpg", ".jpeg", ".png"))
])

for imgsz in SIZES:
    print(f"\nTESTIRANJE imgsz={imgsz}")

    for filename in images:
        image_path = os.path.join(IMAGE_DIR, filename)
        label_path = os.path.join(
            LABEL_DIR,
            os.path.splitext(filename)[0] + ".txt"
        )

        image = cv2.imread(image_path)

        if image is None:
            print("Ne mogu otvoriti:", filename)
            continue

        height, width = image.shape[:2]

        ground_truth = load_ground_truth(
            label_path,
            width,
            height
        )

        result = model(
            image,
            imgsz=imgsz,
            conf=CONF,
            iou=0.45,
            max_det=MAX_DET,
            verbose=False
        )[0]

        predictions = []

        for box in result.boxes:
            class_id = int(box.cls.item())

            if class_id not in TARGET_CLASSES:
                continue

            x1, y1, x2, y2 = box.xyxy[0].tolist()

            predictions.append({
                "class_id": class_id,
                "box": [x1, y1, x2, y2]
            })

        tp, fp, fn = evaluate(
            ground_truth,
            predictions
        )

        precision = tp / (tp + fp) if tp + fp > 0 else 0
        recall = tp / (tp + fn) if tp + fn > 0 else 0

        raw_results.append({
            "imgsz": imgsz,
            "image": filename,
            "ground_truth": len(ground_truth),
            "predictions": len(predictions),
            "tp": tp,
            "fp": fp,
            "fn": fn,
            "precision": precision,
            "recall": recall
        })

        print(
            filename,
            "| GT:", len(ground_truth),
            "| DET:", len(predictions),
            "| TP:", tp,
            "| FP:", fp,
            "| FN:", fn
        )


with open("quality_results_raw.csv", "w", newline="") as f:
    writer = csv.DictWriter(
        f,
        fieldnames=raw_results[0].keys()
    )

    writer.writeheader()
    writer.writerows(raw_results)


with open("quality_summary.csv", "w", newline="") as f:
    fieldnames = [
        "imgsz",
        "images",
        "ground_truth",
        "predictions",
        "tp",
        "fp",
        "fn",
        "precision",
        "recall"
    ]

    writer = csv.DictWriter(
        f,
        fieldnames=fieldnames
    )

    writer.writeheader()

    for imgsz in SIZES:
        rows = [
            r for r in raw_results
            if r["imgsz"] == imgsz
        ]

        gt = sum(r["ground_truth"] for r in rows)
        predictions = sum(r["predictions"] for r in rows)
        tp = sum(r["tp"] for r in rows)
        fp = sum(r["fp"] for r in rows)
        fn = sum(r["fn"] for r in rows)

        precision = tp / (tp + fp) if tp + fp > 0 else 0
        recall = tp / (tp + fn) if tp + fn > 0 else 0

        writer.writerow({
            "imgsz": imgsz,
            "images": len(rows),
            "ground_truth": gt,
            "predictions": predictions,
            "tp": tp,
            "fp": fp,
            "fn": fn,
            "precision": round(precision, 4),
            "recall": round(recall, 4)
        })

        print("\n--------------------------")
        print("imgsz =", imgsz)
        print("Slika =", len(rows))
        print("Rucno oznacenih objekata =", gt)
        print("Detekcija =", predictions)
        print("TP =", tp)
        print("FP =", fp)
        print("FN =", fn)
        print("Precision =", round(precision, 4))
        print("Recall =", round(recall, 4))


print("\nRezultati spremljeni u:")
print("quality_results_raw.csv")
print("quality_summary.csv")
