import cv2
import os

IMAGE_DIR = "images"
LABEL_DIR = "labels"

CLASSES = {
    1: ("bottle", 39),
    2: ("banana", 46),
    3: ("chair", 56),
    4: ("laptop", 63),
    5: ("teddy bear", 77)
}

os.makedirs(LABEL_DIR, exist_ok=True)

images = sorted([
    f for f in os.listdir(IMAGE_DIR)
    if f.lower().endswith((".jpg", ".jpeg", ".png"))
])

for filename in images:
    image = cv2.imread(os.path.join(IMAGE_DIR, filename))

    if image is None:
        continue

    h, w = image.shape[:2]
    display = image.copy()
    annotations = []
    boxes = []

    current_class = 1
    drawing = False
    start_x, start_y = 0, 0

    window = f"Oznacavanje - {filename}"

    def redraw():
        global display
        display = image.copy()

        for x1, y1, x2, y2, class_number in boxes:
            name = CLASSES[class_number][0]
            cv2.rectangle(display, (x1, y1), (x2, y2), (255, 255, 255), 3)
            cv2.putText(
                display,
                name,
                (x1, max(30, y1 - 10)),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.8,
                (255, 255, 255),
                2
            )

        name = CLASSES[current_class][0]

        cv2.rectangle(display, (0, 0), (w, 75), (0, 0, 0), -1)

        cv2.putText(
            display,
            f"KLASA: {current_class} = {name}",
            (20, 30),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.8,
            (255, 255, 255),
            2
        )

        cv2.putText(
            display,
            "1 bottle | 2 banana | 3 chair | 4 laptop | 5 teddy bear | U undo | N next",
            (20, 62),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.55,
            (255, 255, 255),
            1
        )

    def mouse(event, x, y, flags, param):
        global drawing, start_x, start_y

        if event == cv2.EVENT_LBUTTONDOWN:
            drawing = True
            start_x, start_y = x, y

        elif event == cv2.EVENT_LBUTTONUP and drawing:
            drawing = False

            x1 = min(start_x, x)
            y1 = min(start_y, y)
            x2 = max(start_x, x)
            y2 = max(start_y, y)

            if x2 - x1 < 5 or y2 - y1 < 5:
                return

            class_id = CLASSES[current_class][1]

            xc = ((x1 + x2) / 2) / w
            yc = ((y1 + y2) / 2) / h
            bw = (x2 - x1) / w
            bh = (y2 - y1) / h

            boxes.append((x1, y1, x2, y2, current_class))
            annotations.append(
                f"{class_id} {xc:.6f} {yc:.6f} {bw:.6f} {bh:.6f}"
            )

            redraw()

    cv2.namedWindow(window, cv2.WINDOW_NORMAL)
    cv2.setMouseCallback(window, mouse)

    redraw()

    while True:
        cv2.imshow(window, display)
        key = cv2.waitKey(20) & 0xFF

        if key in [ord("1"), ord("2"), ord("3"), ord("4"), ord("5")]:
            current_class = int(chr(key))
            redraw()

        elif key in [ord("u"), ord("U")]:
            if annotations:
                annotations.pop()
                boxes.pop()
                redraw()

        elif key in [ord("n"), ord("N")]:
            break

    label_name = os.path.splitext(filename)[0] + ".txt"

    with open(os.path.join(LABEL_DIR, label_name), "w") as f:
        f.write("\n".join(annotations))

    cv2.destroyWindow(window)

cv2.destroyAllWindows()

print("Sve oznake su spremljene.")