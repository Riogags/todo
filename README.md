# Smart Irrigation System — Machine Learning Component

This repository is the **machine-learning (ML) part** of a smart irrigation
system built for a university Electrical/Electronic Engineering project. The
full system also has a hardware prototype (sensors + microcontroller + a water
pump); this repo is the "brain" that looks at sensor readings and decides what
to do.

This document explains **everything** in the project in plain language — what
each model does, why it was built that way, how to run it, and what every number
means. It is written so you can use it to revise for a viva/presentation.

---

## 1. The big picture

The system reads **three sensors** and answers **two questions**:

| Sensor reading | Unit | Where it comes from |
|----------------|------|---------------------|
| `soil_moisture` | % | capacitive/resistive soil moisture sensor |
| `temperature`   | °C | DHT11/DHT22 sensor |
| `air_humidity`  | % | DHT11/DHT22 sensor (same chip as temperature) |

From those three numbers, the project runs **two machine-learning models**:

| | **Model 1** | **Model 2** |
|---|---|---|
| Question it answers | *Should* I irrigate? | *How much* water & sunlight? |
| Output | `Yes` / `No` | `water_liters_per_m2`, `sunlight_hours` |
| Kind of ML task | **Classification** (a category) | **Regression** (a number) |
| Algorithm | **Logistic** Regression | **Linear** Regression |

> **Key idea to remember:** the two models use *different* algorithms on purpose.
> A Yes/No answer is a **category**, so it needs classification (logistic
> regression). An amount of water is a **number**, so it needs regression (linear
> regression). Choosing the right tool for each question is a deliberate design
> decision — not an inconsistency.

---

## 2. Why these three features?

The hardware team can cheaply measure all three, and each one physically affects
how fast the soil dries out:

- **Soil moisture** — the most important feature. It directly tells you how dry
  the soil already is.
- **Temperature** — hotter air means faster evaporation and transpiration, so
  the plant loses water quicker.
- **Air humidity** — drier air pulls water out of the soil and plant faster, so
  low humidity means you need to water sooner.

Temperature and humidity come from a **single DHT11/DHT22 chip**, so adding both
costs the hardware team almost nothing.

Roughly, the importance order is: **soil_moisture ≫ temperature ≈ air_humidity**.
You can actually *see* this in the trained models' coefficients (Section 6).

---

## 3. Model 1 — the irrigate decision (Logistic Regression)

**Goal:** given the three readings, output `Yes` (turn the pump on) or `No`
(leave it off).

### Why logistic regression?
The answer is a **category** (`Yes` or `No`), not a number. Logistic regression
is the standard, explainable model for two-class (binary) decisions. It produces
a **probability** between 0 and 1 (using the sigmoid function) and then says
`Yes` if that probability is above 0.5, otherwise `No`.

We encode the label as `1` = irrigate, `0` = don't.

### How it is trained (`src/train.py`)
1. **Load + clean** the dataset (`data/irrigation.csv`). Bad/missing sensor
   values are filled with the column **median** (robust to outliers); rows with
   no label are dropped.
2. **Split** the data 80% train / 20% test, with a fixed random seed so results
   are reproducible. `stratify` keeps the Yes/No ratio the same in both halves.
3. **Standardise** the features with `StandardScaler` (subtract the mean, divide
   by the standard deviation). This is fitted on the **training data only** — if
   we used the test data to fit the scaler, information would "leak" and the
   scores would look better than they really are.
4. **Train** `LogisticRegression` on the scaled training data.
5. **Evaluate** on the held-out 20% test set (Section 5 explains the metrics).
6. **Save** the model and the scaler with `joblib` so they can be reused without
   retraining.

### How to use it
```bash
python src/predict.py 18 34 30        # -> Yes
python src/predict.py 52 26 75        # -> No
python src/predict.py 18 34 30 --proba   # -> Yes  (P(irrigate)=0.999)
```

### The optional safety rule
By default the decision is **model-only**. There is also an optional hard safety
rule you can switch on with `--safety`: *never irrigate if the soil is already
wet* (soil_moisture ≥ 60%), no matter what the model says. This protects the
crop and pump from over-watering if the model ever misbehaves:
```bash
python src/predict.py 80 40 20 --safety   # -> No (soil already wet)
```

---

## 4. Model 2 — the water/sunlight estimator (Linear Regression)

**Goal:** given the same three readings, estimate two **amounts**:

- `water_liters_per_m2` — how much water to apply (litres per m² per day).
- `sunlight_hours` — an **advisory** recommended daily sun-exposure (hours).

### Why linear regression?
These outputs are **continuous numbers**, not categories, so regression is the
correct tool. One `LinearRegression` model handles **both** outputs at once
(it natively supports multiple target columns).

> This does **not** contradict the project rule "don't use linear regression for
> the irrigate target." That rule is about Model 1, whose target is a *category*.
> Model 2's targets are *numbers*, so regression is right. **Right tool per
> target.**

### An honest note about "sunlight"
In a field, you can switch the **water pump** on, but you **cannot switch the sun
on**. So `sunlight_hours` is treated as an **advisory recommendation**, not
something the hardware actuates. If this were a *greenhouse with grow-lights*,
sunlight could become a real actuated output — the code is structured so that
change is easy.

### How it is trained (`src/train_regression.py`)
Same shape as Model 1 (load → clean → 80/20 split → standardise → train →
evaluate → save), but it uses **regression metrics** (Section 5) because the
outputs are numbers, and it saves a separate `regressor.joblib`.

### How to use it
```bash
python src/estimate.py 18 34 30
# water_liters_per_m2: 6.67
# sunlight_hours     : 6.70
```

---

## 5. Running both models at once — `recommend.py`

You usually want **both answers from one set of readings**. `src/recommend.py`
is the single combined entry point: give it the three values once, and it runs
Model 1 and Model 2 together.

```bash
python src/recommend.py 18 34 30
```
```
=== Irrigation recommendation ===
  Irrigate?          : Yes  (P=0.999)
  Water to apply     : 6.67 L/m^2
  Sunlight (advisory): 6.70 h
```

In the live system, the hardware host calls **one function**:
```python
from recommend import recommend
r = recommend(soil_moisture=18, temperature=34, air_humidity=30)
# {"irrigate": "Yes", "irrigate_probability": 0.999,
#  "water_liters_per_m2": 6.67, "sunlight_hours": 6.70}
```

`recommend.py` is a thin wrapper: it just calls `predict()` and `estimate()` and
merges their answers, so each model's logic stays defined in one place.

---

## 6. Understanding the output numbers

### Classification metrics (Model 1)
After training, `train.py` prints these for the 20% test set:

- **Accuracy** — overall fraction of predictions that were correct.
- **Precision** — of the times the model said "irrigate", how many really
  needed it. (High precision = few false alarms / little wasted water.)
- **Recall** — of the times water *was* actually needed, how many the model
  caught. (High recall = the plant rarely gets missed.)
- **F1 score** — a single balanced score combining precision and recall.
- **Confusion matrix** — a 2×2 table of correct vs. wrong predictions, also
  saved as `reports/confusion_matrix.png`.

### Regression metrics (Model 2)
Because the outputs are numbers, different metrics are used, **per target**:

- **R² (R-squared)** — fraction of the variation the model explains. `1.0` is
  perfect; `0` means it's no better than always guessing the average.
- **MAE (mean absolute error)** — the average size of the error, in the
  target's own units (e.g. litres).
- **RMSE (root mean squared error)** — like MAE but punishes large misses more.

### Reading the coefficients (explainability)
Both models print their learned **coefficients**. Because the features were
standardised, the **size** of a coefficient shows how much that sensor matters,
and the **sign** shows the direction. For example, in the irrigate model:

| Feature | Coefficient | Meaning |
|---------|-------------|---------|
| soil_moisture | large **negative** | wetter soil pushes toward **No** (sensible) |
| temperature   | positive | hotter pushes toward **Yes** (sensible) |
| air_humidity  | negative | drier air pushes toward **Yes** (sensible) |

The fact that `soil_moisture` has the biggest coefficient confirms it is the
most important feature — which matches the physics. **This is what makes the
model explainable**, which matters for the viva.

---

## 7. About the data (important!)

There is **no real labelled field data yet**, so both datasets are **synthetic**
(computer-generated) but **physically reasonable** — dry soil + hot + dry air
leads to "irrigate" and to higher water amounts. They are generated with a
**fixed random seed**, so everyone gets identical data and results.

- `data/irrigation.csv` ← made by `src/generate_data.py`
- `data/water_sunlight.csv` ← made by `src/generate_water_sunlight_data.py`

**When you get real measurements**, just replace the relevant CSV with real data
(same column names) and re-run the matching `train…` script. Nothing else
changes.

> **Worth saying in the viva:** in the synthetic run, the water model fits well
> (R² ≈ 0.88) but the sunlight model fits poorly (R² ≈ 0.48). This is *expected,
> not a bug* — how much sunlight a plant "needs" is mostly a property of the crop,
> not something these three sensors can reveal. It shows the model can only learn
> relationships that genuinely exist in the data.

---

## 8. Project structure

```
.
├── CLAUDE.md                  # project brief + build decisions
├── README.md                  # this file
├── requirements.txt           # Python dependencies
├── data/
│   ├── irrigation.csv         # Model 1 dataset (synthetic)
│   └── water_sunlight.csv     # Model 2 dataset (synthetic)
├── src/
│   ├── utils.py               # shared helpers: features, paths, data loading
│   ├── generate_data.py             # make Model 1's synthetic dataset
│   ├── generate_water_sunlight_data.py  # make Model 2's synthetic dataset
│   ├── train.py               # train + evaluate + save Model 1
│   ├── train_regression.py    # train + evaluate + save Model 2
│   ├── predict.py             # Model 1 inference (Yes/No)
│   ├── estimate.py            # Model 2 inference (water/sunlight)
│   └── recommend.py           # run BOTH models from one input
├── models/                    # saved models (created by training)
│   ├── model.joblib           # Model 1
│   ├── scaler.joblib          # Model 1's feature scaler
│   ├── regressor.joblib       # Model 2
│   └── regressor_scaler.joblib# Model 2's feature scaler
└── reports/
    ├── confusion_matrix.png   # Model 1 evaluation plot
    └── regression_parity.png  # Model 2 evaluation plot
```

---

## 9. How to run everything from scratch

```bash
# 1. Install dependencies (ideally inside a virtual environment)
pip install -r requirements.txt

# 2. Model 1 — irrigate decision (classification)
python src/generate_data.py     # (re)create the synthetic dataset
python src/train.py             # train, evaluate, save model + scaler + plot
python src/predict.py 18 34 30  # -> Yes

# 3. Model 2 — water/sunlight estimate (regression)
python src/generate_water_sunlight_data.py  # (re)create its synthetic dataset
python src/train_regression.py              # train, evaluate, save regressor
python src/estimate.py 18 34 30             # -> water + sunlight

# 4. Both models at once
python src/recommend.py 18 34 30
```

**Notes**
- Use `python3` instead of `python` on macOS/Linux if needed.
- The two `train…` scripts must each be run **once** before their matching
  inference script, so the `.joblib` model files exist. (They are already
  committed to the repo, so a fresh clone works straight away.)

---

## 10. Tech stack

- Python 3.x
- `pandas`, `numpy` — data handling
- `scikit-learn` — the ML models (LogisticRegression, LinearRegression)
- `joblib` — saving/loading trained models
- `matplotlib` — evaluation plots

Everything is lightweight and standard, so it can run on a Raspberry Pi or be
exported to an embedded host.
