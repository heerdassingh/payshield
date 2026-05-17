from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """Application configuration"""
    
    # App settings
    app_name: str = "PayShield AI Service"
    app_version: str = "1.0.0"
    debug: bool = False
    
    # Model settings
    model_contamination: float = 0.1  # Expected proportion of outliers
    model_n_estimators: int = 100  # Number of trees in Isolation Forest
    fraud_threshold: float = 0.7  # Score above this = fraud
    high_risk_threshold: float = 0.5  # Score above this = high risk
    
    # RAG settings
    chroma_persist_dir: str = "./chroma_data"
    embedding_model: str = "all-MiniLM-L6-v2"
    
    # MongoDB settings (for fetching training data)
    mongodb_uri: str = "mongodb://localhost:27017/payshield"
    
    # Redis settings
    redis_host: str = "localhost"
    redis_port: int = 6379
    
    class Config:
        env_prefix = "PAYSHIELD_AI_"
        env_file = ".env"


settings = Settings()
