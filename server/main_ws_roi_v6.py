import json
import time
import traceback

import cv2
import numpy as np
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from ultralytics import YOLO


app = FastAPI()

# YOLOv8 Nano
model = YOLO("yolov8n.pt")


def decode_image(contents: bytes):
   
    np_array = np.frombuffer(contents, dtype=np.uint8)
    image = cv2.imdecode(np_array, cv2.IMREAD_COLOR)
    return image


def run_detection(image):
   

    original_height, original_width = image.shape[:2]

    
    rotated_image = cv2.rotate(
        image,
        cv2.ROTATE_90_CLOCKWISE
    )

    rotated_height, rotated_width = rotated_image.shape[:2]

    start_time = time.time()

    results = model(
        rotated_image,
        imgsz=320,
        conf=0.50,
        iou=0.45,
        max_det=10,
        verbose=False
    )

    inference_time_ms = round(
        (time.time() - start_time) * 1000,
        2
    )

    detections = []
    for result in results:
        for box in result.boxes:

            class_id = int(box.cls[0])
            class_name = model.names[class_id]
            confidence = float(box.conf[0])

            # YOLO bbox na rotiranoj slici
            rx1, ry1, rx2, ry2 = box.xyxy[0].tolist()

            # Ogranicavanje na dimenzije rotirane slike
            rx1 = max(0.0, min(rx1, float(rotated_width)))
            ry1 = max(0.0, min(ry1, float(rotated_height)))
            rx2 = max(0.0, min(rx2, float(rotated_width)))
            ry2 = max(0.0, min(ry2, float(rotated_height)))

            # Vracanje bbox koordinata iz 90° CLOCKWISE
            # rotirane slike u originalni CameraX frame
            x1 = ry1
            y1 = original_height - rx2
            x2 = ry2
            y2 = original_height - rx1

            print("Klasa:", class_name)
            print("Originalna slika:", original_width, "x", original_height)
            print("Rotirana slika:", rotated_width, "x", rotated_height)
            print("BBox na rotiranoj slici:", rx1, ry1, rx2, ry2)
            print("BBox nakon povratne transformacije:", x1, y1, x2, y2)
            # Ogranicavanje na originalni frame
            x1 = max(0.0, min(x1, float(original_width)))
            y1 = max(0.0, min(y1, float(original_height)))
            x2 = max(0.0, min(x2, float(original_width)))
            y2 = max(0.0, min(y2, float(original_height)))

            detections.append(
                {
                    "class": class_name,
                    "confidence": round(confidence, 4),
                    "box": {
                        "x1": round(x1, 2),
                        "y1": round(y1, 2),
                        "x2": round(x2, 2),
                        "y2": round(y2, 2)
                    }
                }
            )
    return {
        "image_width": original_width,
        "image_height": original_height,
        "rotation": 0,
        "num_detections": len(detections),
        "inference_time_ms": inference_time_ms,
        "detections": detections
    }


@app.get("/")
def root():
    return {
        "status": "ok",
        "model": "YOLOv8n",
        "backend": "main_ws_roi_v6"
    }
@app.websocket("/ws/detect")
async def websocket_detect(websocket: WebSocket):

    await websocket.accept()

    print("WebSocket veza otvorena")

    try:
        while True:

            contents = await websocket.receive_bytes()

            image = decode_image(contents)

            if image is None:
                await websocket.send_text(
                    json.dumps(
                        {
                            "error": "Slika nije ispravno dekodirana"
                        }
                    )
                )
                continue

            result = run_detection(image)

            print(
                f"Frame: "
                f"{result['image_width']}x{result['image_height']} | "
                f"Objekti: {result['num_detections']} | "
                f"Inference: {result['inference_time_ms']} ms"
            )

            await websocket.send_text(
                json.dumps(result)
            )

    except WebSocketDisconnect:
        print("WebSocket veza zatvorena")

    except Exception as exception:

        print("Greška:")
        traceback.print_exc()

        try:
            await websocket.send_text(
                json.dumps(
                    {
                        "error": str(exception)
                    }
                )
            )
        except Exception:
            pass
