"""
train_regression.py — train the SECOND model: Linear Regression for amounts.

Run:  python src/train_regression.py

This model estimates two continuous numbers from the same three sensor inputs:
  * water_liters_per_m2  (how much water to apply)
  * sunlight_hours       (advisory recommended sun exposure)

Why Linear Regression here (and Logistic for the irrigate decision)?
  - irrigate is a CATEGORY (Yes/No)         -> classification -> LogisticRegression
  - water/sunlight are NUMBERS (amounts)    -> regression     -> LinearRegression
Using the right tool for each target is the point.

LinearRegression natively supports MULTIPLE targets at once (y has two columns),
so a single fitted model predicts both numbers.
"""

from __future__ import annotations

import os

import joblib
import matplotlib

matplotlib.use("Agg")  # headless backend — no display needed
import matplotlib.pyplot as plt
import numpy as np
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

from utils import (
    FEATURES,
    PROJECT_ROOT,
    RANDOM_STATE,
    REGRESSION_TARGETS,
    REGRESSOR_PATH,
    REGRESSOR_SCALER_PATH,
    load_regression_data,
)

PARITY_PLOT_PATH = os.path.join(PROJECT_ROOT, "reports", "regression_parity.png")


def main() -> None:
    # 1. Load + clean data.
    df = load_regression_data()
    print(f"Loaded {len(df)} rows from the water/sunlight dataset.")
    print()

    X = df[FEATURES].values               # three sensor inputs
    y = df[REGRESSION_TARGETS].values     # two numeric outputs (n_rows, 2)

    # 2. Train/test split — 80/20, fixed seed.
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=RANDOM_STATE
    )

    # 3. Standardise features (fit on train only). For plain OLS this does not
    #    change prediction quality, but it puts the coefficients on a comparable
    #    scale so we can read off which sensor matters most — useful for the viva.
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    # 4. Train one multi-output Linear Regression.
    model = LinearRegression()
    model.fit(X_train_scaled, y_train)

    # 5. Evaluate with REGRESSION metrics (per target, since the units differ).
    #    R^2  : fraction of variance explained (1.0 = perfect, 0 = no better
    #           than predicting the mean).
    #    MAE  : average absolute error, in the target's own units.
    #    RMSE : root mean squared error, punishes big misses more than MAE.
    y_pred = model.predict(X_test_scaled)

    print("=== Evaluation on held-out test set (per target) ===")
    for i, target in enumerate(REGRESSION_TARGETS):
        r2 = r2_score(y_test[:, i], y_pred[:, i])
        mae = mean_absolute_error(y_test[:, i], y_pred[:, i])
        rmse = np.sqrt(mean_squared_error(y_test[:, i], y_pred[:, i]))
        print(f"  {target}:")
        print(f"      R^2 : {r2:.3f}")
        print(f"      MAE : {mae:.3f}")
        print(f"      RMSE: {rmse:.3f}")
    print()

    # 6. Explainability: coefficients per target (features are standardised, so
    #    magnitudes are directly comparable; sign shows direction of effect).
    print("=== Model explanation (standardised coefficients) ===")
    for i, target in enumerate(REGRESSION_TARGETS):
        print(f"  {target} (intercept {model.intercept_[i]:+.3f}):")
        for feature, coef in zip(FEATURES, model.coef_[i]):
            print(f"      {feature:>13}: {coef:+.3f}")
    print()

    # 7. Parity plots (predicted vs actual) — points on the diagonal = good.
    os.makedirs(os.path.dirname(PARITY_PLOT_PATH), exist_ok=True)
    fig, axes = plt.subplots(1, len(REGRESSION_TARGETS), figsize=(10, 4))
    for i, (ax, target) in enumerate(zip(axes, REGRESSION_TARGETS)):
        ax.scatter(y_test[:, i], y_pred[:, i], alpha=0.5, edgecolor="none")
        lo = min(y_test[:, i].min(), y_pred[:, i].min())
        hi = max(y_test[:, i].max(), y_pred[:, i].max())
        ax.plot([lo, hi], [lo, hi], "r--", linewidth=1)  # perfect-prediction line
        ax.set_xlabel("actual")
        ax.set_ylabel("predicted")
        ax.set_title(target)
    fig.suptitle("Regression: predicted vs actual")
    fig.tight_layout()
    fig.savefig(PARITY_PLOT_PATH, dpi=150)
    plt.close(fig)
    print(f"Saved parity plot -> {PARITY_PLOT_PATH}")
    print()

    # 8. Persist model + its scaler for the estimator/inference side.
    os.makedirs(os.path.dirname(REGRESSOR_PATH), exist_ok=True)
    joblib.dump(model, REGRESSOR_PATH)
    joblib.dump(scaler, REGRESSOR_SCALER_PATH)
    print(f"Saved regressor -> {REGRESSOR_PATH}")
    print(f"Saved scaler    -> {REGRESSOR_SCALER_PATH}")


if __name__ == "__main__":
    main()
