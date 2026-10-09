import asyncio
import json
import time

import cv2
import numpy as np
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from ultralytics import YOLO


app = FastAPI()

model = YOLO("yolov8n.pt")


def decode_image(contents: bytes):

    np_array = np.frombuffer(
        contents,
        dtype=np.uint8
    )

    return cv2.imdecode(
        np_array,
        cv2.IMREAD_COLOR
    )


def run_detection(image):

    height, width = image.shape[:2]

    start = time.perf_counter()

    result = model(
        image,
        imgsz=320,
        conf=0.50,
        iou=0.45,
        max_det=10,
        verbose=False
    )[0]

    inference_time_ms = (
        time.perf_counter() - start
    ) * 1000

    detections = []

    for box in result.boxes:

        class_id = int(
            box.cls.item()
        )

        x1, y1, x2, y2 = (
            box.xyxy[0].tolist()
        )

        detections.append(
            {
                "class": model.names[class_id],
                "confidence": round(
                    float(box.conf.item()),
                    4
                ),
                "box": {
                    "x1": float(x1),
                    "y1": float(y1),
                    "x2": float(x2),
                    "y2": float(y2)
                }
            }
        )

    return {
        "image_width": width,
        "image_height": height,
        "rotation": 0,
        "num_detections": len(detections),
        "inference_time_ms": round(
            inference_time_ms,
            2
        ),
        "detections": detections
    }


@app.get("/")
async def root():

    return {
        "status": "OK",
        "server": "timeout-test"
    }


@app.websocket("/ws/detect")
async def websocket_endpoint(
    websocket: WebSocket
):

    await websocket.accept()

    print()
    print("========================================")
    print("TIMEOUT TEST SERVER")
    print("WebSocket veza otvorena")
    print("Odgovor se šalje nakon 6 sekundi")
    print("========================================")
    print()

    try:

        while True:

            try:

                contents = (
                    await websocket.receive_bytes()
                )

            except WebSocketDisconnect:

                print(
                    "Klijent je zatvorio WebSocket vezu."
                )

                break

            except RuntimeError:

                print(
                    "WebSocket više nije povezan."
                )

                break

            print(
                "Primljen novi frame:",
                len(contents),
                "bajtova"
            )

            image = decode_image(contents)

            if image is None:

                print(
                    "Slika nije ispravno dekodirana."
                )

                continue

            result = run_detection(image)

            print(
                f"Frame: "
                f"{result['image_width']}x"
                f"{result['image_height']} | "
                f"Objekti: "
                f"{result['num_detections']} | "
                f"Inference: "
                f"{result['inference_time_ms']} ms"
            )

            print()
            print(
                "ČEKAM 6 SEKUNDI..."
            )

            await asyncio.sleep(6)

            print(
                "ŠALJEM ODGOVOR NAKON 6 SEKUNDI..."
            )

            try:

                await websocket.send_text(
                    json.dumps(result)
                )

                print(
                    "ODGOVOR POSLAN."
                )

            except Exception:

                print(
                    "ODGOVOR STARE VEZE NIJE MOGAO "
                    "BITI POSLAN - VEZA JE ZATVORENA."
                )

                break

    finally:

        print(
            "TIMEOUT TEST: WebSocket obrada završena."
        )
