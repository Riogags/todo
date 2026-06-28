"""
estimate.py — inference for the SECOND model (water + sunlight amounts).

Mirrors predict.py, but instead of a Yes/No decision it returns the two
estimated amounts. Use it directly:

    from estimate import estimate
    amounts = estimate(soil_moisture=18, temperature=34, air_humidity=30)
    # amounts == {"water_liters_per_m2": 5.1, "sunlight_hours": 6.4}

…or from the command line:

    python src/estimate.py 18 34 30
    # water_liters_per_m2: 5.10
    # sunlight_hours     : 6.40
"""

from __future__ import annotations

import argparse
import sys

import joblib

from utils import (
    REGRESSION_TARGETS,
    REGRESSOR_PATH,
    REGRESSOR_SCALER_PATH,
    features_to_array,
)

# Load the regressor + scaler once per process, then reuse.
_model = None
_scaler = None


def _load():
    global _model, _scaler
    if _model is None or _scaler is None:
        try:
            _model = joblib.load(REGRESSOR_PATH)
            _scaler = joblib.load(REGRESSOR_SCALER_PATH)
        except FileNotFoundError as exc:
            raise FileNotFoundError(
                "Regressor/scaler not found. Run `python src/train_regression.py` "
                "first to produce models/regressor.joblib and "
                "models/regressor_scaler.joblib."
            ) from exc
    return _model, _scaler


def estimate(soil_moisture: float, temperature: float, air_humidity: float) -> dict:
    """Estimate required water and sunlight from three live sensor readings.

    Returns a dict keyed by REGRESSION_TARGETS:
        {"water_liters_per_m2": <float>, "sunlight_hours": <float>}

    Water is clamped at 0 (you never apply negative water); sunlight is clamped
    to a sensible non-negative range. The readings are packed in the trained
    feature order and scaled with the SAME scaler used in training.
    """
    model, scaler = _load()

    X = features_to_array(soil_moisture, temperature, air_humidity)
    X_scaled = scaler.transform(X)
    preds = model.predict(X_scaled)[0]

    result = {target: float(value) for target, value in zip(REGRESSION_TARGETS, preds)}
    # Guard against tiny negative predictions near zero from the linear model.
    result["water_liters_per_m2"] = max(0.0, result["water_liters_per_m2"])
    result["sunlight_hours"] = max(0.0, result["sunlight_hours"])
    return result


def _parse_args(argv=None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Estimate required water and sunlight from three sensor readings."
    )
    parser.add_argument("soil_moisture", type=float, help="soil moisture (%%)")
    parser.add_argument("temperature", type=float, help="temperature (°C)")
    parser.add_argument("air_humidity", type=float, help="air humidity (%%)")
    return parser.parse_args(argv)


def main(argv=None) -> None:
    args = _parse_args(argv)
    amounts = estimate(args.soil_moisture, args.temperature, args.air_humidity)
    print(f"water_liters_per_m2: {amounts['water_liters_per_m2']:.2f}")
    print(f"sunlight_hours     : {amounts['sunlight_hours']:.2f}")


if __name__ == "__main__":
    main(sys.argv[1:])
