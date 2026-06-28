"""
generate_water_sunlight_data.py — SYNTHETIC dataset for the regression model.

>>> THIS DATA IS SYNTHETIC. <<<
Just like the classification dataset, there is no real labelled "how much water
/ sunlight" data yet, so we generate a physically-reasonable stand-in so the
regression pipeline runs end to end. Replace data/water_sunlight.csv with real
agronomy/field measurements when you have them and re-run train_regression.py.

Targets and the physical reasoning behind them
----------------------------------------------
water_liters_per_m2  (litres per m^2 per day):
  The plant loses more water — so needs more applied — when the soil is dry,
  the air is hot, and the air is dry:
    * LOW soil_moisture  -> already dry            -> apply MORE water
    * HIGH temperature   -> more evaporation       -> apply MORE water
    * LOW air_humidity   -> faster loss to the air -> apply MORE water
  We clip at 0 (you never apply negative water).

sunlight_hours  (recommended daily sun-exposure, hours) — ADVISORY:
  A field cannot switch the sun on, so this is a recommendation, not an
  actuated output. It only weakly depends on the sensors (real sunlight need is
  mostly a property of the crop), so we keep it near a baseline with a mild,
  clearly-illustrative dependence + noise, clipped to a sensible 2-12 h range.
"""

from __future__ import annotations

import os

import numpy as np
import pandas as pd

from utils import FEATURES, RANDOM_STATE, REGRESSION_TARGETS, WATER_SUNLIGHT_DATA_PATH

N_SAMPLES = 400


def generate(n_samples: int = N_SAMPLES, seed: int = RANDOM_STATE) -> pd.DataFrame:
    rng = np.random.default_rng(seed)

    # Same plausible sensor ranges as the classification dataset.
    soil_moisture = rng.uniform(5, 95, n_samples)    # %
    temperature = rng.uniform(15, 45, n_samples)     # °C
    air_humidity = rng.uniform(15, 95, n_samples)    # %

    # --- water_liters_per_m2 -------------------------------------------------
    # Linear combination matching the physics above (dry/hot/dry-air -> more).
    # 60 and 50 are "comfortable" reference points; coefficients set the
    # sensitivity. Soil moisture dominates, as in the classification model.
    water = (
        0.10 * (60.0 - soil_moisture)   # drier soil  -> more water
        + 0.15 * (temperature - 20.0)   # hotter      -> more water
        + 0.05 * (50.0 - air_humidity)  # drier air   -> more water
    )
    water = water + rng.normal(0, 0.4, n_samples)     # measurement noise
    water = np.clip(water, 0.0, None)                 # never negative

    # --- sunlight_hours (advisory) ------------------------------------------
    # Mostly a baseline (~6 h) with a deliberately mild dependence on conditions.
    sunlight = (
        6.0
        + 0.05 * (temperature - 25.0)
        - 0.02 * (air_humidity - 50.0)
    )
    sunlight = sunlight + rng.normal(0, 0.6, n_samples)
    sunlight = np.clip(sunlight, 2.0, 12.0)

    df = pd.DataFrame(
        {
            "soil_moisture": soil_moisture.round(1),
            "temperature": temperature.round(1),
            "air_humidity": air_humidity.round(1),
            "water_liters_per_m2": water.round(2),
            "sunlight_hours": sunlight.round(2),
        }
    )
    return df[FEATURES + REGRESSION_TARGETS]


def main() -> None:
    df = generate()
    os.makedirs(os.path.dirname(WATER_SUNLIGHT_DATA_PATH), exist_ok=True)
    df.to_csv(WATER_SUNLIGHT_DATA_PATH, index=False)
    print(f"[SYNTHETIC] Wrote {len(df)} rows to {WATER_SUNLIGHT_DATA_PATH}")
    print(df[REGRESSION_TARGETS].describe().round(2).to_string())


if __name__ == "__main__":
    main()
