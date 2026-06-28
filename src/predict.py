"""
predict.py — load the saved model and decide whether to irrigate.

This is the deployment / inference entry point. The hardware-side host reads its
three sensors and either:

  (a) imports the helper directly:

          from predict import predict
          decision = predict(soil_moisture=18, temperature=34, air_humidity=30)
          # decision == "Yes"  -> turn pump on
          # decision == "No"   -> leave pump off

  (b) calls this file as a command line tool (e.g. over a serial/host bridge):

          python src/predict.py 18 34 30
          # prints: Yes

The model + scaler are loaded once (lazily, then cached) so repeated calls in a
control loop don't re-read the files every time.
"""

from __future__ import annotations

import argparse
import sys

import joblib

from utils import (
    MODEL_PATH,
    SAFETY_MOISTURE_CEILING,
    SCALER_PATH,
    decode_label,
    features_to_array,
)

# Module-level cache so we load the joblib files only once per process.
_model = None
_scaler = None


def _load():
    """Load model + scaler from disk on first use, then reuse them."""
    global _model, _scaler
    if _model is None or _scaler is None:
        try:
            _model = joblib.load(MODEL_PATH)
            _scaler = joblib.load(SCALER_PATH)
        except FileNotFoundError as exc:
            raise FileNotFoundError(
                "Model/scaler not found. Run `python src/train.py` first to "
                "produce models/model.joblib and models/scaler.joblib."
            ) from exc
    return _model, _scaler


def predict(
    soil_moisture: float,
    temperature: float,
    air_humidity: float,
    use_safety_rule: bool = False,
) -> str:
    """Decide whether to irrigate from three live sensor readings.

    Parameters
    ----------
    soil_moisture : float   soil moisture in %  (most important feature)
    temperature   : float   air temperature in °C
    air_humidity  : float   relative air humidity in %
    use_safety_rule : bool  if True, never return "Yes" when the soil is already
                            at/above SAFETY_MOISTURE_CEILING, regardless of the
                            model. This is the optional hard safety override.

    Returns
    -------
    "Yes" -> irrigate (turn pump on)
    "No"  -> don't irrigate

    The three readings are packed in the trained feature order, scaled with the
    SAME StandardScaler used in training, then classified by the model.
    """
    # Optional hard safety rule: wet soil must never be watered.
    if use_safety_rule and soil_moisture >= SAFETY_MOISTURE_CEILING:
        return "No"

    model, scaler = _load()

    X = features_to_array(soil_moisture, temperature, air_humidity)
    X_scaled = scaler.transform(X)
    prediction = model.predict(X_scaled)[0]
    return decode_label(prediction)


def predict_proba(soil_moisture: float, temperature: float, air_humidity: float) -> float:
    """Return the model's probability that irrigation is needed (class 1).

    Handy for the report/viva and for hardware that wants a confidence value
    rather than a hard Yes/No.
    """
    model, scaler = _load()
    X = features_to_array(soil_moisture, temperature, air_humidity)
    X_scaled = scaler.transform(X)
    return float(model.predict_proba(X_scaled)[0][1])


def _parse_args(argv=None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Irrigation decision from three sensor readings."
    )
    parser.add_argument("soil_moisture", type=float, help="soil moisture (%%)")
    parser.add_argument("temperature", type=float, help="temperature (°C)")
    parser.add_argument("air_humidity", type=float, help="air humidity (%%)")
    parser.add_argument(
        "--safety",
        action="store_true",
        help=f"enable hard safety rule (never irrigate if soil_moisture >= "
        f"{SAFETY_MOISTURE_CEILING}%%)",
    )
    parser.add_argument(
        "--proba",
        action="store_true",
        help="also print the model's irrigation probability",
    )
    return parser.parse_args(argv)


def main(argv=None) -> None:
    args = _parse_args(argv)
    decision = predict(
        args.soil_moisture,
        args.temperature,
        args.air_humidity,
        use_safety_rule=args.safety,
    )
    if args.proba:
        p = predict_proba(args.soil_moisture, args.temperature, args.air_humidity)
        print(f"{decision}  (P(irrigate)={p:.3f})")
    else:
        print(decision)


if __name__ == "__main__":
    main(sys.argv[1:])
