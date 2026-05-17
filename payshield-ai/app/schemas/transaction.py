from pydantic import BaseModel, Field
from typing import Dict, Optional, List
from datetime import datetime


class TransactionFeatures(BaseModel):
    """Features extracted from a transaction for fraud prediction"""
    transaction_id: str = Field(..., description="Unique transaction identifier")
    user_id: str = Field(..., description="User ID")
    merchant_id: str = Field(..., description="Merchant ID")
    amount: float = Field(..., description="Transaction amount")
    currency: str = Field(default="USD", description="Currency code")
    
    # Computed features from Java backend
    amount_normalized: float = Field(default=0.0, description="Amount normalized 0-1")
    frequency_1h: float = Field(default=0.0, description="Transaction frequency in last hour (0-1)")
    frequency_24h: float = Field(default=0.0, description="Transaction frequency in last 24h (0-1)")
    time_anomaly: float = Field(default=0.0, description="Time of day anomaly score (0-1)")
    high_amount_flag: float = Field(default=0.0, description="High amount indicator (0-1)")
    velocity: float = Field(default=0.0, description="Rapid transaction velocity (0-1)")
    
    # Optional context
    device_id: Optional[str] = None
    ip_address: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None


class FraudPrediction(BaseModel):
    """Response from the fraud prediction model"""
    transaction_id: str
    fraud_score: float = Field(..., ge=0.0, le=1.0, description="Fraud probability score")
    is_fraudulent: bool = Field(..., description="Whether transaction is classified as fraud")
    risk_level: str = Field(..., description="LOW, MEDIUM, HIGH, or CRITICAL")
    fraud_reason: str = Field(..., description="Human-readable explanation")
    model_version: str = Field(default="2.0.0-tf", description="Model version used")
    feature_importance: Dict[str, float] = Field(default_factory=dict, description="Feature contribution scores")
    rag_explanation: Optional[str] = Field(None, description="RAG-enhanced explanation")
    prediction_time_ms: float = Field(default=0.0, description="Prediction latency in ms")


class RAGQuery(BaseModel):
    """Request for RAG query"""
    query: str = Field(..., description="Query about fraud patterns")
    transaction_context: Optional[Dict] = Field(None, description="Optional transaction context")
    top_k: int = Field(default=3, description="Number of relevant documents to retrieve")


class RAGResponse(BaseModel):
    """Response from RAG pipeline"""
    query: str
    answer: str
    sources: List[str] = Field(default_factory=list)
    confidence: float = Field(default=0.0, ge=0.0, le=1.0)


class ModelStatus(BaseModel):
    """Model status information"""
    model_name: str = "TF-Autoencoder"
    model_version: str = "2.0.0-tf"
    is_trained: bool = False
    training_samples: int = 0
    last_trained: Optional[datetime] = None
    fraud_threshold: float = 0.7
    high_risk_threshold: float = 0.5
    rag_documents_loaded: int = 0
    status: str = "healthy"


class HealthResponse(BaseModel):
    """Health check response"""
    status: str = "healthy"
    service: str = "payshield-ai"
    version: str = "2.0.0-tf"
    model_ready: bool = False
    rag_ready: bool = False
