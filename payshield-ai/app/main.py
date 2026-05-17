"""
PayShield AI Service - FastAPI Application

Provides fraud detection using a TensorFlow deep autoencoder
and RAG-based fraud pattern analysis using LangChain + ChromaDB.
"""

import time
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from prometheus_fastapi_instrumentator import Instrumentator
from prometheus_client import Counter, Histogram, Gauge

from app.config import settings
from app.schemas.transaction import (
    TransactionFeatures,
    FraudPrediction,
    RAGQuery,
    RAGResponse,
    ModelStatus,
    HealthResponse,
)
from app.models.fraud_model import fraud_model
from app.rag.pipeline import rag_pipeline

# Logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s"
)
logger = logging.getLogger(__name__)

# Prometheus metrics
PREDICTIONS_TOTAL = Counter(
    "fraud_predictions_total",
    "Total fraud predictions made",
    ["result"]
)
PREDICTION_LATENCY = Histogram(
    "fraud_prediction_latency_seconds",
    "Fraud prediction latency in seconds",
    buckets=[0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0]
)
FRAUD_SCORE_DISTRIBUTION = Histogram(
    "fraud_score_distribution",
    "Distribution of fraud scores",
    buckets=[0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0]
)
RAG_QUERIES_TOTAL = Counter(
    "rag_queries_total",
    "Total RAG queries made"
)
MODEL_READY = Gauge(
    "model_ready",
    "Whether the fraud model is trained and ready"
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application startup and shutdown events"""
    # Startup
    logger.info("Starting PayShield AI Service...")
    
    # Train the fraud model
    fraud_model.train()
    MODEL_READY.set(1 if fraud_model.is_trained else 0)
    
    # Initialize RAG pipeline
    rag_pipeline.initialize()
    
    logger.info("PayShield AI Service ready!")
    
    yield
    
    # Shutdown
    logger.info("Shutting down PayShield AI Service...")


# Create FastAPI app
app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="AI-powered fraud detection with TensorFlow Autoencoder and RAG pipeline",
    lifespan=lifespan,
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Prometheus instrumentation
Instrumentator().instrument(app).expose(app)


# ============================================================
# Health Check
# ============================================================

@app.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """Health check endpoint"""
    return HealthResponse(
        status="healthy",
        service="payshield-ai",
        version=settings.app_version,
        model_ready=fraud_model.is_trained,
        rag_ready=rag_pipeline.is_ready,
    )


# ============================================================
# Fraud Prediction
# ============================================================

@app.post("/api/v1/predict", response_model=FraudPrediction, tags=["Fraud Detection"])
async def predict_fraud(features: TransactionFeatures):
    """
    Predict fraud score for a transaction using TensorFlow Autoencoder.
    
    Accepts transaction features and returns:
    - Fraud score (0-1) based on reconstruction error
    - Risk level classification
    - Human-readable explanation
    - RAG-enhanced context
    - Feature importance from reconstruction error decomposition
    """
    if not fraud_model.is_trained:
        raise HTTPException(
            status_code=503,
            detail="Model is not trained yet. Please wait for initialization."
        )
    
    start_time = time.time()
    
    # Extract features dict
    feature_dict = {
        "amount_normalized": features.amount_normalized,
        "frequency_1h": features.frequency_1h,
        "frequency_24h": features.frequency_24h,
        "time_anomaly": features.time_anomaly,
        "high_amount_flag": features.high_amount_flag,
        "velocity": features.velocity,
    }
    
    # Run prediction
    fraud_score, feature_importance = fraud_model.predict(feature_dict)
    
    # Determine results
    is_fraudulent = fraud_score >= settings.fraud_threshold
    risk_level = fraud_model.determine_risk_level(fraud_score)
    fraud_reason = fraud_model.determine_fraud_reason(
        feature_dict, feature_importance, fraud_score
    )
    
    # Get RAG context for explanation
    rag_explanation = rag_pipeline.get_fraud_context(
        feature_dict, fraud_score
    )
    
    elapsed_ms = (time.time() - start_time) * 1000
    
    # Update metrics
    result_label = "fraud" if is_fraudulent else "legitimate"
    PREDICTIONS_TOTAL.labels(result=result_label).inc()
    PREDICTION_LATENCY.observe(elapsed_ms / 1000)
    FRAUD_SCORE_DISTRIBUTION.observe(fraud_score)
    
    logger.info(
        f"Prediction: transaction_id={features.transaction_id}, "
        f"score={fraud_score:.4f}, is_fraud={is_fraudulent}, "
        f"risk={risk_level}, latency={elapsed_ms:.2f}ms"
    )
    
    return FraudPrediction(
        transaction_id=features.transaction_id,
        fraud_score=round(fraud_score, 4),
        is_fraudulent=is_fraudulent,
        risk_level=risk_level,
        fraud_reason=fraud_reason,
        model_version=fraud_model.model_version,
        feature_importance=feature_importance,
        rag_explanation=rag_explanation,
        prediction_time_ms=round(elapsed_ms, 2),
    )


# ============================================================
# RAG Query
# ============================================================

@app.post("/api/v1/rag/query", response_model=RAGResponse, tags=["RAG"])
async def query_rag(request: RAGQuery):
    """
    Query the fraud knowledge base using RAG pipeline.
    
    Returns relevant fraud pattern information and analysis.
    """
    RAG_QUERIES_TOTAL.inc()
    
    answer, sources, confidence = rag_pipeline.query(
        request.query,
        top_k=request.top_k
    )
    
    return RAGResponse(
        query=request.query,
        answer=answer,
        sources=sources,
        confidence=round(confidence, 4),
    )


# ============================================================
# Model Status
# ============================================================

@app.get("/api/v1/model/status", response_model=ModelStatus, tags=["Model"])
async def model_status():
    """Get the current status of the fraud detection model"""
    return ModelStatus(
        model_name="TF-Autoencoder",
        model_version=fraud_model.model_version,
        is_trained=fraud_model.is_trained,
        training_samples=fraud_model.training_samples,
        last_trained=fraud_model.last_trained,
        fraud_threshold=settings.fraud_threshold,
        high_risk_threshold=settings.high_risk_threshold,
        rag_documents_loaded=rag_pipeline.documents_loaded,
        status="healthy" if fraud_model.is_trained else "initializing",
    )


@app.post("/api/v1/model/retrain", tags=["Model"])
async def retrain_model():
    """Force retrain the fraud detection model"""
    try:
        fraud_model.train(force_retrain=True)
        MODEL_READY.set(1 if fraud_model.is_trained else 0)
        return {
            "status": "success",
            "message": "Model retrained successfully",
            "training_samples": fraud_model.training_samples,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Retraining failed: {str(e)}")
