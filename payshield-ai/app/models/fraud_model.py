"""
Fraud Detection Model using TensorFlow Autoencoder.

This module implements an anomaly-detection-based fraud detection model that:
1. Trains a deep autoencoder on synthetic normal transaction data
2. Measures reconstruction error to detect anomalous (potentially fraudulent) transactions
3. Provides feature importance via gradient-based attribution

Architecture:
  Input(6) → Dense(32, relu) → Dense(16, relu) → Dense(8, relu)  [Encoder]
           → Dense(16, relu) → Dense(32, relu) → Dense(6, sigmoid) [Decoder]

High reconstruction error = anomalous transaction = potential fraud.
"""

import numpy as np
import logging
import time
import os
from datetime import datetime
from typing import Dict, Tuple, Optional

import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers, Model

from app.config import settings

logger = logging.getLogger(__name__)

# Suppress TF warnings
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"
tf.get_logger().setLevel("WARNING")

# Feature names used by the model
FEATURE_NAMES = [
    "amount_normalized",
    "frequency_1h",
    "frequency_24h",
    "time_anomaly",
    "high_amount_flag",
    "velocity",
]

NUM_FEATURES = len(FEATURE_NAMES)


def build_autoencoder(input_dim: int) -> Model:
    """
    Build a deep autoencoder for anomaly detection.

    The encoder compresses transaction features into a low-dimensional
    latent space. The decoder reconstructs the original features.
    Normal transactions are reconstructed accurately (low error).
    Fraudulent transactions have high reconstruction error.
    """
    # Encoder
    encoder_input = keras.Input(shape=(input_dim,), name="encoder_input")
    x = layers.Dense(32, activation="relu", name="enc_dense_1")(encoder_input)
    x = layers.BatchNormalization(name="enc_bn_1")(x)
    x = layers.Dropout(0.2, name="enc_dropout_1")(x)
    x = layers.Dense(16, activation="relu", name="enc_dense_2")(x)
    x = layers.BatchNormalization(name="enc_bn_2")(x)
    latent = layers.Dense(8, activation="relu", name="latent")(x)

    # Decoder
    x = layers.Dense(16, activation="relu", name="dec_dense_1")(latent)
    x = layers.BatchNormalization(name="dec_bn_1")(x)
    x = layers.Dropout(0.2, name="dec_dropout_2")(x)
    x = layers.Dense(32, activation="relu", name="dec_dense_2")(x)
    x = layers.BatchNormalization(name="dec_bn_2")(x)
    decoder_output = layers.Dense(
        input_dim, activation="sigmoid", name="decoder_output"
    )(x)

    autoencoder = Model(
        inputs=encoder_input, outputs=decoder_output, name="fraud_autoencoder"
    )
    autoencoder.compile(
        optimizer=keras.optimizers.Adam(learning_rate=0.001),
        loss="mse",
    )
    return autoencoder


class FraudDetectionModel:
    """
    TensorFlow Autoencoder-based fraud detection model.

    Trained on synthetic data representing normal transaction patterns.
    Anomalous transactions (potential fraud) are detected based on
    reconstruction error — the higher the error, the more likely fraud.
    """

    def __init__(self):
        self.model: Optional[Model] = None
        self.is_trained: bool = False
        self.training_samples: int = 0
        self.last_trained: Optional[datetime] = None
        self.model_version: str = "2.0.0-tf"
        self._model_path = os.path.join(os.path.dirname(__file__), "saved_model")
        # Threshold calibrated from training data (set during training)
        self._reconstruction_threshold: float = 0.0
        # Mean and std for standardization (computed during training)
        self._feature_mean: Optional[np.ndarray] = None
        self._feature_std: Optional[np.ndarray] = None

    def train(self, force_retrain: bool = False):
        """
        Train the autoencoder on synthetic normal transaction data.
        In production, this would train on real historical transaction data.
        """
        # Check for saved model
        if not force_retrain and self._load_saved_model():
            logger.info("Loaded pre-trained TensorFlow model from disk")
            return

        logger.info("Training TensorFlow Autoencoder model on synthetic data...")

        # Generate synthetic normal transaction data
        np.random.seed(42)
        tf.random.set_seed(42)

        n_normal = 10000
        n_fraud = 200  # ~2% fraud rate (used only for threshold calibration)

        # Normal transactions: low amounts, normal frequency, daytime
        normal_data = np.column_stack(
            [
                np.random.beta(2, 5, n_normal),  # amount_normalized
                np.random.beta(2, 8, n_normal),  # frequency_1h
                np.random.beta(2, 6, n_normal),  # frequency_24h
                np.random.choice(
                    [0.0, 0.0, 0.0, 0.0, 0.8], n_normal
                ),  # time_anomaly
                np.random.choice(
                    [0.0, 0.0, 0.0, 0.3], n_normal
                ),  # high_amount_flag
                np.random.choice(
                    [0.0, 0.0, 0.0, 0.0, 0.7], n_normal
                ),  # velocity
            ]
        )

        # Fraudulent data for threshold calibration
        fraud_data = np.column_stack(
            [
                np.random.beta(5, 2, n_fraud),  # amount_normalized
                np.random.beta(6, 2, n_fraud),  # frequency_1h
                np.random.beta(5, 2, n_fraud),  # frequency_24h
                np.random.choice([0.0, 0.8, 0.8], n_fraud),  # time_anomaly
                np.random.choice([0.0, 0.3, 0.6, 0.6], n_fraud),  # high_amount_flag
                np.random.choice([0.0, 0.7, 0.7, 0.7], n_fraud),  # velocity
            ]
        )

        # Standardize features (fit on normal data only)
        self._feature_mean = normal_data.mean(axis=0)
        self._feature_std = normal_data.std(axis=0)
        self._feature_std[self._feature_std == 0] = 1.0  # avoid division by zero

        normal_scaled = (normal_data - self._feature_mean) / self._feature_std

        # Build and train autoencoder on NORMAL data only
        self.model = build_autoencoder(NUM_FEATURES)

        logger.info("Autoencoder architecture:")
        self.model.summary(print_fn=logger.info)

        history = self.model.fit(
            normal_scaled,
            normal_scaled,  # autoencoder target = input
            epochs=50,
            batch_size=64,
            validation_split=0.1,
            shuffle=True,
            verbose=0,
            callbacks=[
                keras.callbacks.EarlyStopping(
                    monitor="val_loss",
                    patience=5,
                    restore_best_weights=True,
                ),
            ],
        )

        final_loss = history.history["loss"][-1]
        val_loss = history.history["val_loss"][-1]
        logger.info(
            f"Training complete — loss: {final_loss:.6f}, val_loss: {val_loss:.6f}"
        )

        # Calibrate threshold using reconstruction error on normal + fraud data
        normal_errors = self._compute_reconstruction_errors(normal_scaled)
        fraud_scaled = (fraud_data - self._feature_mean) / self._feature_std
        fraud_errors = self._compute_reconstruction_errors(fraud_scaled)

        # Set threshold at 95th percentile of normal errors
        self._reconstruction_threshold = float(np.percentile(normal_errors, 95))
        logger.info(
            f"Reconstruction threshold: {self._reconstruction_threshold:.6f} "
            f"(normal mean={normal_errors.mean():.6f}, "
            f"fraud mean={fraud_errors.mean():.6f})"
        )

        self.is_trained = True
        self.training_samples = n_normal
        self.last_trained = datetime.now()

        # Save model
        self._save_model()

        logger.info(
            f"Model trained successfully: {self.training_samples} normal samples, "
            f"threshold={self._reconstruction_threshold:.6f}"
        )

    def predict(self, features: Dict[str, float]) -> Tuple[float, Dict[str, float]]:
        """
        Predict fraud score for a transaction using reconstruction error.

        Args:
            features: Dictionary of feature name -> value

        Returns:
            Tuple of (fraud_score, feature_importance)
            fraud_score: 0.0 (safe) to 1.0 (highly fraudulent)
            feature_importance: per-feature reconstruction error contribution
        """
        if not self.is_trained:
            raise RuntimeError("Model is not trained. Call train() first.")

        start_time = time.time()

        # Extract feature values in correct order
        feature_vector = np.array(
            [[features.get(name, 0.0) for name in FEATURE_NAMES]], dtype=np.float32
        )

        # Standardize
        feature_scaled = (feature_vector - self._feature_mean) / self._feature_std

        # Get reconstruction from autoencoder
        input_tensor = tf.constant(feature_scaled, dtype=tf.float32)
        reconstructed = self.model(input_tensor, training=False).numpy()

        # Compute per-feature reconstruction error
        per_feature_error = np.square(feature_scaled - reconstructed)[0]

        # Total reconstruction error (MSE)
        total_error = float(per_feature_error.mean())

        # Convert to 0-1 fraud score
        fraud_score = self._error_to_score(total_error)

        # Feature importance = per-feature error normalized
        feature_importance = self._compute_feature_importance(per_feature_error)

        elapsed_ms = (time.time() - start_time) * 1000
        logger.debug(
            f"Prediction: error={total_error:.6f}, score={fraud_score:.4f}, "
            f"latency={elapsed_ms:.2f}ms"
        )

        return fraud_score, feature_importance

    def _compute_reconstruction_errors(self, data: np.ndarray) -> np.ndarray:
        """Compute reconstruction errors for a batch of samples"""
        reconstructed = self.model.predict(data, verbose=0)
        errors = np.mean(np.square(data - reconstructed), axis=1)
        return errors

    def _error_to_score(self, reconstruction_error: float) -> float:
        """
        Convert reconstruction error to a 0-1 fraud probability score.

        Uses the calibrated threshold:
        - Error below threshold → low score (normal)
        - Error above threshold → high score (anomalous/fraudulent)
        - Sigmoid-like mapping for smooth scoring
        """
        if self._reconstruction_threshold <= 0:
            return 0.5

        # Ratio of error to threshold
        ratio = reconstruction_error / self._reconstruction_threshold

        # Sigmoid mapping centered at threshold (ratio=1)
        # ratio < 1 → score < 0.5 (normal)
        # ratio > 1 → score > 0.5 (anomalous)
        score = 1.0 / (1.0 + np.exp(-5.0 * (ratio - 1.0)))

        return float(np.clip(score, 0.0, 1.0))

    def _compute_feature_importance(
        self, per_feature_error: np.ndarray
    ) -> Dict[str, float]:
        """
        Compute feature importance from per-feature reconstruction error.
        Features with higher reconstruction error contribute more to the anomaly.
        """
        total = per_feature_error.sum()
        importance = {}

        for i, name in enumerate(FEATURE_NAMES):
            if total > 0:
                importance[name] = round(float(per_feature_error[i] / total), 4)
            else:
                importance[name] = round(1.0 / NUM_FEATURES, 4)

        return importance

    def determine_risk_level(self, fraud_score: float) -> str:
        """Determine risk level from fraud score"""
        if fraud_score >= 0.9:
            return "CRITICAL"
        elif fraud_score >= settings.fraud_threshold:
            return "HIGH"
        elif fraud_score >= settings.high_risk_threshold:
            return "MEDIUM"
        return "LOW"

    def determine_fraud_reason(
        self,
        features: Dict[str, float],
        feature_importance: Dict[str, float],
        fraud_score: float,
    ) -> str:
        """Generate a human-readable fraud reason based on top contributing features"""
        if fraud_score < settings.high_risk_threshold:
            return "Transaction appears normal — low reconstruction error."

        reasons = []

        # Sort features by importance (reconstruction error contribution)
        sorted_features = sorted(
            feature_importance.items(), key=lambda x: x[1], reverse=True
        )

        feature_descriptions = {
            "velocity": "High velocity transactions detected — multiple rapid transactions",
            "amount_normalized": "Unusually high transaction amount deviates from learned patterns",
            "time_anomaly": "Transaction at unusual time of day — high reconstruction error",
            "frequency_1h": "Abnormal transaction frequency in the last hour",
            "frequency_24h": "Elevated transaction frequency over 24 hours",
            "high_amount_flag": "Transaction amount significantly above average baseline",
        }

        for name, imp in sorted_features[:3]:
            if imp > 0.1 and features.get(name, 0.0) > 0.3:
                reasons.append(feature_descriptions.get(name, f"Anomaly in {name}"))

        if not reasons:
            reasons.append(
                "Multiple anomaly indicators combined — high autoencoder reconstruction error"
            )

        return "; ".join(reasons) + "."

    def _save_model(self):
        """Save trained model and parameters to disk"""
        os.makedirs(self._model_path, exist_ok=True)

        # Save TensorFlow model
        model_dir = os.path.join(self._model_path, "autoencoder.keras")
        self.model.save(model_dir)

        # Save normalization params and threshold
        params = {
            "feature_mean": self._feature_mean.tolist(),
            "feature_std": self._feature_std.tolist(),
            "reconstruction_threshold": self._reconstruction_threshold,
            "training_samples": self.training_samples,
        }
        params_path = os.path.join(self._model_path, "params.npy")
        np.save(params_path, params)

        logger.info(f"TensorFlow model saved to {self._model_path}")

    def _load_saved_model(self) -> bool:
        """Load saved model from disk"""
        model_dir = os.path.join(self._model_path, "autoencoder.keras")
        params_path = os.path.join(self._model_path, "params.npy")

        if os.path.exists(model_dir) and os.path.exists(params_path):
            try:
                self.model = keras.models.load_model(model_dir)
                params = np.load(params_path, allow_pickle=True).item()
                self._feature_mean = np.array(params["feature_mean"])
                self._feature_std = np.array(params["feature_std"])
                self._reconstruction_threshold = params["reconstruction_threshold"]
                self.training_samples = params.get("training_samples", 10000)
                self.is_trained = True
                self.last_trained = datetime.fromtimestamp(
                    os.path.getmtime(params_path)
                )
                return True
            except Exception as e:
                logger.warning(f"Failed to load saved TensorFlow model: {e}")
        return False


# Singleton instance
fraud_model = FraudDetectionModel()
