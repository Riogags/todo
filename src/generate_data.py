"""
generate_data.py — create a SYNTHETIC but physically reasonable dataset.

>>> THIS DATA IS SYNTHETIC. <<<
There is no real labelled field data yet, so we generate a stand-in so the whole
pipeline (train -> evaluate -> predict) runs end to end. Replace
data/irrigation.csv with real sensor logs when the hardware team has them and
re-run train.py — no code changes needed.

Physical reasoning behind the labels
-------------------------------------
The plant needs water when the soil is dry AND conditions dry it out fast:
  * LOW soil_moisture   -> the soil is already dry            -> irrigate
  * HIGH temperature    -> faster evaporation/transpiration   -> irrigate sooner
  * LOW air_humidity    -> faster water loss to the air       -> irrigate sooner

We encode that as a weighted "water-need" score, with soil_moisture dominating
(matching the stated importance soil_moisture >> temperature ~ air_humidity),
add a little noise so the classes aren't perfectly separable (real sensors are
noisy), then threshold the score to get the 0/1 label.
"""

from __future__ import annotations

import os

import numpy as np
import pandas as pd

from utils import DATA_PATH, FEATURES, RANDOM_STATE, TARGET

N_SAMPLES = 400


def generate(n_samples: int = N_SAMPLES, seed: int = RANDOM_STATE) -> pd.DataFrame:
    rng = np.random.default_rng(seed)

    # Draw plausible sensor ranges.
    soil_moisture = rng.uniform(5, 95, n_samples)    # %  (dry .. saturated)
    temperature = rng.uniform(15, 45, n_samples)     # °C (mild .. very hot)
    air_humidity = rng.uniform(15, 95, n_samples)    # %  (arid .. humid)

    # Standardise each feature to ~0-centred units so the weights below express
    # relative importance rather than being dominated by raw scale differences.
    def z(x):
        return (x - x.mean()) / x.std()

    # Water-need score. Signs follow the physics above:
    #   dry soil (low moisture)  -> +need   => negative weight on moisture
    #   hot (high temp)          -> +need   => positive weight on temperature
    #   dry air (low humidity)   -> +need   => negative weight on humidity
    # Magnitudes encode soil_moisture >> temperature ~ air_humidity.
    score = (
        -3.0 * z(soil_moisture)
        + 1.0 * z(temperature)
        - 1.0 * z(air_humidity)
    )

    # Add noise so the boundary is fuzzy (otherwise the model gets a trivial
    # 100% and the evaluation is meaningless).
    score = score + rng.normal(0, 0.8, n_samples)

    # Threshold at 0 (the median-ish split) -> roughly balanced classes.
    irrigate = (score > 0).astype(int)

    df = pd.DataFrame(
        {
            "soil_moisture": soil_moisture.round(1),
            "temperature": temperature.round(1),
            "air_humidity": air_humidity.round(1),
            TARGET: irrigate,
        }
    )
    return df[FEATURES + [TARGET]]


def main() -> None:
    df = generate()
    os.makedirs(os.path.dirname(DATA_PATH), exist_ok=True)
    df.to_csv(DATA_PATH, index=False)

    n_pos = int(df[TARGET].sum())
    n_neg = len(df) - n_pos
    print(f"[SYNTHETIC] Wrote {len(df)} rows to {DATA_PATH}")
    print(f"  irrigate=1 (Yes): {n_pos}")
    print(f"  irrigate=0 (No) : {n_neg}")


if __name__ == "__main__":
    main()
