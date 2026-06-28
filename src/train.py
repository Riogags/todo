"""
train.py — load, preprocess, train, evaluate, and save the irrigation model.

Run:  python src/train.py

Pipeline (each step is commented so it can be walked through in a viva):
  1. Load + clean the data.
  2. Split into train/test (80/20, fixed seed).
  3. Standardise features (StandardScaler) — fitted on train only.
  4. Train Logistic Regression.
  5. Evaluate: accuracy, precision, recall, F1, confusion matrix.
  6. Explain: print the learned coefficients.
  7. Persist model + scaler with joblib for the hardware team.
"""

from __future__ import annotations

import os

import joblib
import matplotlib

matplotlib.use("Agg")  # headless backend — works on a Pi / CI with no display
import matplotlib.pyplot as plt
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    ConfusionMatrixDisplay,
    accuracy_score,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
)
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

from utils import (
    CONFUSION_MATRIX_PATH,
    FEATURES,
    LABELS,
    MODEL_PATH,
    RANDOM_STATE,
    SCALER_PATH,
    TARGET,
    load_data,
)


def main() -> None:
    # ----------------------------------------------------------------------
    # 1. Load + clean data
    # ----------------------------------------------------------------------
    df = load_data()
    print(f"Loaded {len(df)} rows from the dataset.")
    print("Class balance (1=irrigate, 0=don't):")
    print(df[TARGET].value_counts().sort_index().to_string())
    print()

    X = df[FEATURES].values   # inputs: the three sensor readings
    y = df[TARGET].values     # output: 0/1 irrigate label

    # ----------------------------------------------------------------------
    # 2. Train/test split — 80/20.
    #    stratify=y keeps the same Yes/No ratio in both halves so the test
    #    metrics aren't skewed by an unlucky split.
    # ----------------------------------------------------------------------
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=RANDOM_STATE, stratify=y
    )

    # ----------------------------------------------------------------------
    # 3. Standardise features (StandardScaler): subtract mean, divide by std,
    #    so each feature is on a comparable 0-centred scale.
    #    IMPORTANT: fit the scaler on the TRAINING data only, then apply that
    #    same transform to the test data. Fitting on the test set too would
    #    leak information and give optimistic scores.
    # ----------------------------------------------------------------------
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    # ----------------------------------------------------------------------
    # 4. Train Logistic Regression.
    #    Logistic Regression (not linear regression) because the target is a
    #    category (irrigate yes/no), not a continuous number. It outputs a
    #    probability via the sigmoid and thresholds at 0.5.
    # ----------------------------------------------------------------------
    model = LogisticRegression(random_state=RANDOM_STATE)
    model.fit(X_train_scaled, y_train)

    # ----------------------------------------------------------------------
    # 5. Evaluate on the held-out test set.
    # ----------------------------------------------------------------------
    y_pred = model.predict(X_test_scaled)

    accuracy = accuracy_score(y_test, y_pred)
    precision = precision_score(y_test, y_pred, zero_division=0)
    recall = recall_score(y_test, y_pred, zero_division=0)
    f1 = f1_score(y_test, y_pred, zero_division=0)

    print("=== Evaluation on held-out test set ===")
    print(f"Accuracy : {accuracy:.3f}  (overall fraction correct)")
    print(f"Precision: {precision:.3f}  (of predicted 'irrigate', how many truly needed it)")
    print(f"Recall   : {recall:.3f}  (of those that needed water, how many we caught)")
    print(f"F1 score : {f1:.3f}  (harmonic mean of precision & recall)")
    print()

    cm = confusion_matrix(y_test, y_pred)
    print("Confusion matrix (rows = actual, cols = predicted):")
    print(f"                 pred No   pred Yes")
    print(f"   actual No      {cm[0, 0]:>5}     {cm[0, 1]:>5}")
    print(f"   actual Yes     {cm[1, 0]:>5}     {cm[1, 1]:>5}")
    print()

    # Save the confusion matrix as a plot for the report.
    os.makedirs(os.path.dirname(CONFUSION_MATRIX_PATH), exist_ok=True)
    disp = ConfusionMatrixDisplay(
        confusion_matrix=cm, display_labels=[LABELS[0], LABELS[1]]
    )
    disp.plot(cmap="Blues", colorbar=False)
    plt.title("Confusion Matrix — Irrigation Model")
    plt.tight_layout()
    plt.savefig(CONFUSION_MATRIX_PATH, dpi=150)
    plt.close()
    print(f"Saved confusion matrix plot -> {CONFUSION_MATRIX_PATH}")
    print()

    # ----------------------------------------------------------------------
    # 6. Explainability: the learned coefficients.
    #    Because features are standardised, the coefficient magnitudes are
    #    directly comparable — bigger |coef| = more influence on the decision.
    #    Sign tells direction:
    #      negative on soil_moisture -> wetter soil pushes toward "No"  (sensible)
    #      positive on temperature   -> hotter pushes toward "Yes"      (sensible)
    #      negative on air_humidity  -> drier air pushes toward "Yes"   (sensible)
    # ----------------------------------------------------------------------
    print("=== Model explanation (standardised coefficients) ===")
    for feature, coef in zip(FEATURES, model.coef_[0]):
        direction = "→ irrigate (Yes)" if coef > 0 else "→ don't (No)"
        print(f"  {feature:>13}: {coef:+.3f}   (higher value {direction})")
    print(f"  {'intercept':>13}: {model.intercept_[0]:+.3f}")
    print()

    # ----------------------------------------------------------------------
    # 7. Persist model + scaler so predict.py / the hardware host can load them.
    #    Both are saved: inference must scale incoming readings with the SAME
    #    scaler that was fitted here, or the model sees out-of-distribution input.
    # ----------------------------------------------------------------------
    os.makedirs(os.path.dirname(MODEL_PATH), exist_ok=True)
    joblib.dump(model, MODEL_PATH)
    joblib.dump(scaler, SCALER_PATH)
    print(f"Saved model  -> {MODEL_PATH}")
    print(f"Saved scaler -> {SCALER_PATH}")


if __name__ == "__main__":
    main()
