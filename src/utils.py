"""
utils.py — shared helpers for the Smart Irrigation ML component.

Everything that both training (train.py) and inference (predict.py) need lives
here so the two stay in sync: the feature order, where files are saved, how we
load data, and the human-readable label mapping.

Keeping the feature ORDER in one place is important: the scaler and the model
were both fitted on columns in exactly this order, so inference must feed values
in the same order or the predictions are silently wrong.
"""

from __future__ import annotations

import os

import numpy as np
import pandas as pd

# ---------------------------------------------------------------------------
# Reproducibility
# ---------------------------------------------------------------------------
# A single fixed seed used everywhere (data generation, train/test split, model
# solver). Fixing it means the whole pipeline produces identical numbers on
# every run, which is what we want for a viva/presentation.
RANDOM_STATE = 42

# ---------------------------------------------------------------------------
# Feature definition — the contract between this code and the hardware sensors
# ---------------------------------------------------------------------------
# Order matters. Do not reorder without retraining.
FEATURES = ["soil_moisture", "temperature", "air_humidity"]
TARGET = "irrigate"

# Human-readable mapping for the binary target.
# 1 -> "Yes" (turn the pump on), 0 -> "No" (leave it off).
LABELS = {0: "No", 1: "Yes"}

# ---------------------------------------------------------------------------
# Optional hard safety rule (see CLAUDE.md "threshold logic")
# ---------------------------------------------------------------------------
# If soil is already this wet (or wetter), never irrigate, no matter what the
# model says. This protects the crop/pump from over-watering if the model ever
# misbehaves. Disabled by default; predict() takes a flag to turn it on.
SAFETY_MOISTURE_CEILING = 60.0  # percent

# ---------------------------------------------------------------------------
# Paths — resolved relative to the project root so scripts work from anywhere
# ---------------------------------------------------------------------------
_THIS_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(_THIS_DIR)

DATA_PATH = os.path.join(PROJECT_ROOT, "data", "irrigation.csv")
MODEL_PATH = os.path.join(PROJECT_ROOT, "models", "model.joblib")
SCALER_PATH = os.path.join(PROJECT_ROOT, "models", "scaler.joblib")
CONFUSION_MATRIX_PATH = os.path.join(PROJECT_ROOT, "reports", "confusion_matrix.png")

# ---------------------------------------------------------------------------
# Second model (regression): estimate how much water + sunlight are required
# ---------------------------------------------------------------------------
# The irrigate decision above is CLASSIFICATION (Yes/No). Estimating amounts is
# a different, CONTINUOUS task, so it uses Linear Regression instead. Same three
# sensor inputs, two numeric outputs:
#   water_liters_per_m2 : recommended water to apply (litres per m^2 per day)
#   sunlight_hours      : advisory recommended daily sun-exposure (hours)
# NOTE: sunlight is advisory (a field can't actuate the sun); reframe as an
# actuated output if this is a greenhouse with grow-lights.
REGRESSION_TARGETS = ["water_liters_per_m2", "sunlight_hours"]
WATER_SUNLIGHT_DATA_PATH = os.path.join(PROJECT_ROOT, "data", "water_sunlight.csv")
REGRESSOR_PATH = os.path.join(PROJECT_ROOT, "models", "regressor.joblib")
REGRESSOR_SCALER_PATH = os.path.join(PROJECT_ROOT, "models", "regressor_scaler.joblib")


def load_data(path: str = DATA_PATH) -> pd.DataFrame:
    """Load the irrigation dataset from CSV and clean missing values.

    Steps:
      1. Read the CSV.
      2. Check the expected columns are present.
      3. Coerce feature columns to numeric (a bad sensor reading written as
         text becomes NaN rather than crashing).
      4. Fill any missing feature values with that column's MEDIAN. Median is
         robust to outliers (a single spurious 0 or 100 won't skew it), which
         suits noisy sensor data.
      5. Drop rows where the target label itself is missing — we can't train on
         a row with no answer.
    """
    df = pd.read_csv(path)

    missing_cols = [c for c in FEATURES + [TARGET] if c not in df.columns]
    if missing_cols:
        raise ValueError(
            f"Dataset at {path} is missing required columns: {missing_cols}. "
            f"Expected columns: {FEATURES + [TARGET]}"
        )

    # Force feature columns to numbers; unparseable entries -> NaN.
    for col in FEATURES:
        df[col] = pd.to_numeric(df[col], errors="coerce")

    # Impute missing sensor readings with the column median.
    for col in FEATURES:
        if df[col].isna().any():
            df[col] = df[col].fillna(df[col].median())

    # A row with no label is useless for supervised training.
    df = df.dropna(subset=[TARGET])
    df[TARGET] = df[TARGET].astype(int)

    return df


def features_to_array(soil_moisture: float, temperature: float, air_humidity: float) -> np.ndarray:
    """Pack three raw sensor readings into the 2-D array sklearn expects.

    Returned shape is (1, 3) with columns in FEATURES order, ready to be passed
    straight to scaler.transform(). Centralising this guarantees inference uses
    the same column order the model was trained on.
    """
    return np.array([[soil_moisture, temperature, air_humidity]], dtype=float)


def decode_label(value: int) -> str:
    """Turn a 0/1 model output into the 'No'/'Yes' string the hardware reads."""
    return LABELS[int(value)]


def load_regression_data(path: str = WATER_SUNLIGHT_DATA_PATH) -> pd.DataFrame:
    """Load the water/sunlight dataset for the regression model and clean it.

    Same cleaning idea as load_data(): coerce to numeric, impute missing sensor
    readings with the column median, and drop rows whose target amounts are
    missing (we can't train on a row with no answer).
    """
    df = pd.read_csv(path)

    required = FEATURES + REGRESSION_TARGETS
    missing_cols = [c for c in required if c not in df.columns]
    if missing_cols:
        raise ValueError(
            f"Dataset at {path} is missing required columns: {missing_cols}. "
            f"Expected columns: {required}"
        )

    for col in required:
        df[col] = pd.to_numeric(df[col], errors="coerce")

    for col in FEATURES:
        if df[col].isna().any():
            df[col] = df[col].fillna(df[col].median())

    df = df.dropna(subset=REGRESSION_TARGETS)
    return df
