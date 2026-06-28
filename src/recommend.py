"""
recommend.py — run BOTH models on one set of sensor readings.

This is the single, combined entry point. You give it the three sensor values
once, and it runs:
  * Model 1 (predict.py)  -> the irrigate Yes/No decision  (classification)
  * Model 2 (estimate.py) -> the water + sunlight amounts   (regression)
and returns everything together.

Use it directly:

    from recommend import recommend
    r = recommend(soil_moisture=18, temperature=34, air_humidity=30)
    # r == {
    #   "irrigate": "Yes",
    #   "irrigate_probability": 0.999,
    #   "water_liters_per_m2": 6.67,
    #   "sunlight_hours": 6.70,
    # }

…or from the command line:

    python src/recommend.py 18 34 30
"""

from __future__ import annotations

import argparse
import sys

# Reuse the two single-model helpers — recommend() is just a thin wrapper that
# calls both, so there is one place that defines each model's behaviour.
from estimate import estimate
from predict import predict, predict_proba


def recommend(
    soil_moisture: float,
    temperature: float,
    air_humidity: float,
    use_safety_rule: bool = False,
) -> dict:
    """Run both models on one set of readings and return a combined result.

    Parameters are the three sensor values; use_safety_rule is passed through to
    Model 1 (never irrigate when soil is already wet, if enabled).

    Returns a dict:
        {
          "irrigate": "Yes"/"No",              # Model 1 decision
          "irrigate_probability": <float>,     # Model 1 confidence (P of Yes)
          "water_liters_per_m2": <float>,      # Model 2 estimate
          "sunlight_hours": <float>,           # Model 2 advisory estimate
        }
    """
    decision = predict(
        soil_moisture, temperature, air_humidity, use_safety_rule=use_safety_rule
    )
    probability = predict_proba(soil_moisture, temperature, air_humidity)
    amounts = estimate(soil_moisture, temperature, air_humidity)

    return {
        "irrigate": decision,
        "irrigate_probability": probability,
        "water_liters_per_m2": amounts["water_liters_per_m2"],
        "sunlight_hours": amounts["sunlight_hours"],
    }


def _parse_args(argv=None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run both models (irrigate decision + water/sunlight estimate) "
        "on one set of sensor readings."
    )
    parser.add_argument("soil_moisture", type=float, help="soil moisture (%%)")
    parser.add_argument("temperature", type=float, help="temperature (°C)")
    parser.add_argument("air_humidity", type=float, help="air humidity (%%)")
    parser.add_argument(
        "--safety",
        action="store_true",
        help="enable the hard safety rule (never irrigate if soil is already wet)",
    )
    return parser.parse_args(argv)


def main(argv=None) -> None:
    args = _parse_args(argv)
    r = recommend(
        args.soil_moisture,
        args.temperature,
        args.air_humidity,
        use_safety_rule=args.safety,
    )
    print("=== Irrigation recommendation ===")
    print(f"  Irrigate?          : {r['irrigate']}  "
          f"(P={r['irrigate_probability']:.3f})")
    print(f"  Water to apply     : {r['water_liters_per_m2']:.2f} L/m^2")
    print(f"  Sunlight (advisory): {r['sunlight_hours']:.2f} h")


if __name__ == "__main__":
    main(sys.argv[1:])
