import csv

INPUT_FILE = "quality_results_raw.csv"
OUTPUT_FILE = "quality_conditions_summary.csv"

CONDITIONS = {
    "Dobro osvjetljenje": [
        "bocaBananeIzblizaSvjetlo.jpg",
        "bocaBananeIzdalekaNaStolici.jpg",
        "laptopMedoBocaBananeNaKaucuSvjetlo.jpg",
        "medoBananeBocaSvjetlo.jpg",
        "medoNaStoliciSvjetlo.jpg",
    ],

    "Slabo osvjetljenje": [
        "bocaBananeSlaboSvjetlo.jpg",
        "medoBananeBocaSlaboSvjetlo.jpg",
        "medoNaStoliciSlaboSvjetlo.jpg",
    ],

    "Udaljeniji objekti": [
        "bocaBananeIzdalekaNaStolici.jpg",
    ],

    "Vise objekata": [
        "bocaBananeIzblizaSvjetlo.jpg",
        "bocaBananeIzdalekaNaStolici.jpg",
        "bocaBananeSlaboSvjetlo.jpg",
        "laptopMedoBocaBananeNaKaucuSvjetlo.jpg",
        "medoBananeBocaSlaboSvjetlo.jpg",
        "medoBananeBocaSvjetlo.jpg",
    ],

    "Djelomicno zaklonjeni objekti": [
        "bocaBananeIzblizaSvjetlo.jpg",
        "bocaBananeIzdalekaNaStolici.jpg",
        "bocaBananeSlaboSvjetlo.jpg",
        "medoBananeBocaSlaboSvjetlo.jpg",
        "medoBananeBocaSvjetlo.jpg",
    ],
}

rows = []

with open(INPUT_FILE, newline="") as f:
    reader = csv.DictReader(f)
    rows = list(reader)

with open(OUTPUT_FILE, "w", newline="") as f:
    writer = csv.writer(f)

    writer.writerow([
        "imgsz",
        "condition",
        "images",
        "ground_truth",
        "tp",
        "fp",
        "fn",
        "precision",
        "recall"
    ])

    for imgsz in [320, 416]:
        size_rows = [
            row for row in rows
            if int(row["imgsz"]) == imgsz
        ]

        for condition, filenames in CONDITIONS.items():
            selected = [
                row for row in size_rows
                if row["image"] in filenames
            ]

            ground_truth = sum(
                int(row["ground_truth"])
                for row in selected
            )

            tp = sum(
                int(row["tp"])
                for row in selected
            )

            fp = sum(
                int(row["fp"])
                for row in selected
            )

            fn = sum(
                int(row["fn"])
                for row in selected
            )

            if tp + fp > 0:
                precision = tp / (tp + fp)
            else:
                precision = 0

            if tp + fn > 0:
                recall = tp / (tp + fn)
            else:
                recall = 0

            writer.writerow([
                imgsz,
                condition,
                len(selected),
                ground_truth,
                tp,
                fp,
                fn,
                round(precision, 4),
                round(recall, 4)
            ])

print("Rezultati po uvjetima:")
print()

with open(OUTPUT_FILE) as f:
    print(f.read())
