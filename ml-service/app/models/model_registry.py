from __future__ import annotations

import json
import logging
import os
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import faiss
import joblib
import lightgbm as lgb
import numpy as np
import xgboost as xgb
from sentence_transformers import SentenceTransformer

logger = logging.getLogger(__name__)

BASE_DIR = Path(__file__).resolve().parents[2]
ARTIFACTS_DIR = BASE_DIR / "artifacts"


@dataclass
class ModelRegistry:
    sentence_model: SentenceTransformer
    ranking_model: Any | None
    recommendation_model: Any | None
    fraud_model: Any | None
    spam_model: Any | None
    spam_vectorizer: Any | None
    faiss_index: faiss.Index | None
    faiss_mapping: dict[str, Any]
    version: str

    @classmethod
    def load(cls) -> "ModelRegistry":
        version = os.getenv("SMARTHIRE_ML_MODEL_VERSION", "2026.03")
        sentence_model_name = os.getenv("SMARTHIRE_SENTENCE_MODEL", "sentence-transformers/all-MiniLM-L6-v2")
        sentence_model = SentenceTransformer(sentence_model_name)

        ranking_model = None
        ranking_path = ARTIFACTS_DIR / "ranking" / "xgb_ranker.json"
        if ranking_path.exists():
            model = xgb.XGBRanker()
            model.load_model(ranking_path)
            ranking_model = model

        recommendation_model = None
        recommendation_path = ARTIFACTS_DIR / "recommendation" / "lgbm_ranker.txt"
        if recommendation_path.exists():
            recommendation_model = lgb.Booster(model_file=str(recommendation_path))

        fraud_model = cls._load_joblib(ARTIFACTS_DIR / "fraud" / "isolation_forest.joblib")
        spam_model = cls._load_joblib(ARTIFACTS_DIR / "spam" / "spam_classifier.joblib")
        spam_vectorizer = cls._load_joblib(ARTIFACTS_DIR / "spam" / "spam_vectorizer.joblib")

        faiss_index = None
        faiss_path = ARTIFACTS_DIR / "faiss" / "jobs.index"
        if faiss_path.exists():
            faiss_index = faiss.read_index(str(faiss_path))

        mapping_path = ARTIFACTS_DIR / "faiss" / "jobs_mapping.json"
        faiss_mapping = json.loads(mapping_path.read_text()) if mapping_path.exists() else {}
        logger.info("Loaded SmartHire ML registry version=%s", version)
        return cls(
            sentence_model=sentence_model,
            ranking_model=ranking_model,
            recommendation_model=recommendation_model,
            fraud_model=fraud_model,
            spam_model=spam_model,
            spam_vectorizer=spam_vectorizer,
            faiss_index=faiss_index,
            faiss_mapping=faiss_mapping,
            version=version,
        )

    @staticmethod
    def _load_joblib(path: Path) -> Any | None:
        if path.exists():
            return joblib.load(path)
        return None
