"""
RAG Pipeline for Fraud Analysis.

Uses LangChain and ChromaDB to provide context-aware fraud explanations
by retrieving relevant fraud pattern documentation for each prediction.
"""

import os
import logging
from typing import List, Optional, Tuple

from app.config import settings

logger = logging.getLogger(__name__)


class FraudRAGPipeline:
    """
    RAG (Retrieval-Augmented Generation) pipeline for fraud analysis.
    
    Loads fraud pattern documents into a ChromaDB vector store,
    and retrieves relevant context for fraud predictions.
    """
    
    def __init__(self):
        self.vectorstore = None
        self.documents_loaded: int = 0
        self.is_ready: bool = False
        self._knowledge_base_dir = os.path.join(
            os.path.dirname(__file__), "knowledge_base"
        )
    
    def initialize(self):
        """Initialize the RAG pipeline by loading documents into ChromaDB"""
        try:
            from langchain_community.document_loaders import DirectoryLoader, TextLoader
            from langchain.text_splitter import RecursiveCharacterTextSplitter
            from langchain_community.embeddings import HuggingFaceEmbeddings
            from langchain_community.vectorstores import Chroma
            
            logger.info("Initializing RAG pipeline...")
            
            # Load documents from knowledge base directory
            loader = DirectoryLoader(
                self._knowledge_base_dir,
                glob="**/*.txt",
                loader_cls=TextLoader
            )
            documents = loader.load()
            
            if not documents:
                logger.warning("No documents found in knowledge base directory")
                return
            
            logger.info(f"Loaded {len(documents)} documents from knowledge base")
            
            # Split documents into chunks
            text_splitter = RecursiveCharacterTextSplitter(
                chunk_size=500,
                chunk_overlap=100,
                separators=["\n\n", "\n", ". ", " ", ""]
            )
            chunks = text_splitter.split_documents(documents)
            logger.info(f"Split into {len(chunks)} chunks")
            
            # Create embeddings and vector store
            embeddings = HuggingFaceEmbeddings(
                model_name=settings.embedding_model,
                model_kwargs={"device": "cpu"}
            )
            
            self.vectorstore = Chroma.from_documents(
                documents=chunks,
                embedding=embeddings,
                persist_directory=settings.chroma_persist_dir
            )
            
            self.documents_loaded = len(documents)
            self.is_ready = True
            
            logger.info(
                f"RAG pipeline initialized: {self.documents_loaded} documents, "
                f"{len(chunks)} chunks indexed"
            )
            
        except Exception as e:
            logger.error(f"Failed to initialize RAG pipeline: {e}")
            logger.info("RAG pipeline will operate in degraded mode (no context)")
            self.is_ready = False
    
    def get_fraud_context(
        self,
        features: dict,
        fraud_score: float,
        top_k: int = 3
    ) -> str:
        """
        Retrieve relevant fraud pattern context for a given transaction.
        
        Args:
            features: Transaction features dictionary
            fraud_score: Predicted fraud score
            top_k: Number of relevant documents to retrieve
            
        Returns:
            Contextual explanation based on retrieved documents
        """
        if not self.is_ready or self.vectorstore is None:
            return self._generate_fallback_context(features, fraud_score)
        
        try:
            # Build a query based on the most suspicious features
            query = self._build_context_query(features, fraud_score)
            
            # Retrieve relevant documents
            docs = self.vectorstore.similarity_search(query, k=top_k)
            
            if not docs:
                return self._generate_fallback_context(features, fraud_score)
            
            # Compose explanation from retrieved context
            context_parts = []
            for doc in docs:
                # Extract the most relevant snippet
                content = doc.page_content.strip()
                if len(content) > 200:
                    content = content[:200] + "..."
                context_parts.append(content)
            
            explanation = self._compose_explanation(features, fraud_score, context_parts)
            return explanation
            
        except Exception as e:
            logger.error(f"RAG query failed: {e}")
            return self._generate_fallback_context(features, fraud_score)
    
    def query(self, query_text: str, top_k: int = 3) -> Tuple[str, List[str], float]:
        """
        Direct query to the RAG pipeline.
        
        Args:
            query_text: Natural language query about fraud patterns
            top_k: Number of documents to retrieve
            
        Returns:
            Tuple of (answer, source_documents, confidence)
        """
        if not self.is_ready or self.vectorstore is None:
            return (
                "RAG pipeline is not initialized. Please try again later.",
                [],
                0.0
            )
        
        try:
            # Retrieve relevant documents with scores
            results = self.vectorstore.similarity_search_with_score(query_text, k=top_k)
            
            if not results:
                return ("No relevant documents found for your query.", [], 0.0)
            
            # Compose answer from retrieved documents
            sources = []
            context_parts = []
            total_score = 0.0
            
            for doc, score in results:
                source = os.path.basename(doc.metadata.get("source", "unknown"))
                sources.append(source)
                context_parts.append(doc.page_content.strip())
                total_score += score
            
            avg_score = total_score / len(results) if results else 0.0
            confidence = max(0.0, min(1.0, 1.0 - avg_score))  # Lower distance = higher confidence
            
            answer = self._format_query_answer(query_text, context_parts)
            
            return (answer, sources, confidence)
            
        except Exception as e:
            logger.error(f"RAG query failed: {e}")
            return (f"Query failed: {str(e)}", [], 0.0)
    
    def _build_context_query(self, features: dict, fraud_score: float) -> str:
        """Build a search query based on transaction features"""
        query_parts = []
        
        if features.get("velocity", 0) > 0.5:
            query_parts.append("high velocity rapid transactions")
        if features.get("amount_normalized", 0) > 0.5:
            query_parts.append("high value transaction amount")
        if features.get("time_anomaly", 0) > 0.5:
            query_parts.append("unusual time of day transaction")
        if features.get("frequency_1h", 0) > 0.5:
            query_parts.append("high frequency transactions per hour")
        if features.get("frequency_24h", 0) > 0.5:
            query_parts.append("elevated daily transaction count")
        if features.get("high_amount_flag", 0) > 0.3:
            query_parts.append("large amount exceeding normal limits")
        
        if not query_parts:
            query_parts.append("normal transaction pattern fraud detection")
        
        return " ".join(query_parts)
    
    def _compose_explanation(
        self, 
        features: dict, 
        fraud_score: float, 
        context_parts: List[str]
    ) -> str:
        """Compose a fraud explanation using retrieved context"""
        explanation = f"AI Analysis (score: {fraud_score:.2f}): "
        
        if fraud_score >= 0.7:
            explanation += "This transaction exhibits high-risk patterns. "
        elif fraud_score >= 0.5:
            explanation += "This transaction shows moderate risk indicators. "
        else:
            explanation += "This transaction appears within normal parameters. "
        
        # Add relevant context
        explanation += "Based on fraud pattern analysis: "
        for i, part in enumerate(context_parts[:2], 1):
            explanation += f"[{i}] {part} "
        
        return explanation.strip()
    
    def _generate_fallback_context(self, features: dict, fraud_score: float) -> str:
        """Generate explanation when RAG is unavailable"""
        parts = []
        
        if features.get("velocity", 0) > 0.5:
            parts.append("High transaction velocity detected")
        if features.get("amount_normalized", 0) > 0.7:
            parts.append("Transaction amount is unusually high")
        if features.get("time_anomaly", 0) > 0.5:
            parts.append("Transaction occurred at an unusual time")
        if features.get("frequency_1h", 0) > 0.7:
            parts.append("Excessive transactions in the last hour")
        
        if not parts:
            if fraud_score > 0.5:
                parts.append("Multiple anomaly indicators detected by the Isolation Forest model")
            else:
                parts.append("Transaction appears within normal parameters")
        
        return f"AI Analysis (score: {fraud_score:.2f}): {'; '.join(parts)}."
    
    def _format_query_answer(self, query: str, context_parts: List[str]) -> str:
        """Format a direct query answer from retrieved context"""
        answer = f"Based on PayShield fraud knowledge base:\n\n"
        for i, part in enumerate(context_parts, 1):
            answer += f"{i}. {part}\n\n"
        return answer.strip()


# Singleton instance
rag_pipeline = FraudRAGPipeline()
