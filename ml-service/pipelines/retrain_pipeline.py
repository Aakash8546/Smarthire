from __future__ import annotations

import json
from pathlib import Path

import faiss
import joblib
import lightgbm as lgb
import numpy as np
import pandas as pd
import xgboost as xgb
from sentence_transformers import SentenceTransformer
from sklearn.ensemble import IsolationForest
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression


BASE_DIR = Path(__file__).resolve().parents[1]
ARTIFACTS_DIR = BASE_DIR / "artifacts"
DATA_DIR = BASE_DIR / "data"
VERSION = "2026.03"


def train() -> None:
    ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)
    sentence_model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")

    ranking_df = pd.read_csv(DATA_DIR / "ranking_training.csv")
    ranking_features = ranking_df[["similarity", "skill_overlap", "resume_score", "match_percentage", "resume_length"]]
    ranking_labels = ranking_df["label"]
    ranker = xgb.XGBRanker(
        objective="rank:pairwise",
        n_estimators=200,
        learning_rate=0.08,
        max_depth=5,
        subsample=0.9,
        colsample_bytree=0.8,
    )
    group_sizes = ranking_df.groupby("job_id").size().tolist()
    ranker.fit(ranking_features, ranking_labels, group=group_sizes)
    ranking_dir = ARTIFACTS_DIR / "ranking"
    ranking_dir.mkdir(exist_ok=True)
    ranker.save_model(ranking_dir / "xgb_ranker.json")

    recommendation_df = pd.read_csv(DATA_DIR / "recommendation_training.csv")
    lgbm = lgb.LGBMRanker(objective="lambdarank", learning_rate=0.05, n_estimators=200)
    lgbm.fit(
        recommendation_df[["content_score", "collaborative_score", "skill_count"]],
        recommendation_df["label"],
        group=recommendation_df.groupby("candidate_id").size().tolist(),
    )
    recommendation_dir = ARTIFACTS_DIR / "recommendation"
    recommendation_dir.mkdir(exist_ok=True)
    lgbm.booster_.save_model(recommendation_dir / "lgbm_ranker.txt")

    fraud_df = pd.read_csv(DATA_DIR / "fraud_training.csv")
    fraud_features = fraud_df[["resume_length", "leadership_count", "guarantee_count"]]
    fraud_model = IsolationForest(contamination=0.08, n_estimators=250, random_state=42)
    fraud_model.fit(fraud_features)
    fraud_dir = ARTIFACTS_DIR / "fraud"
    fraud_dir.mkdir(exist_ok=True)
    joblib.dump(fraud_model, fraud_dir / "isolation_forest.joblib")

    spam_df = pd.read_csv(DATA_DIR / "spam_training.csv")
    vectorizer = TfidfVectorizer(ngram_range=(1, 2), max_features=5000)
    spam_matrix = vectorizer.fit_transform(spam_df["text"])
    spam_model = LogisticRegression(max_iter=200)
    spam_model.fit(spam_matrix, spam_df["label"])
    spam_dir = ARTIFACTS_DIR / "spam"
    spam_dir.mkdir(exist_ok=True)
    joblib.dump(vectorizer, spam_dir / "spam_vectorizer.joblib")
    joblib.dump(spam_model, spam_dir / "spam_classifier.joblib")

    jobs_df = pd.read_csv(DATA_DIR / "jobs_catalog.csv")
    texts = (jobs_df["title"] + ". " + jobs_df["description"]).tolist()
    embeddings = np.asarray(sentence_model.encode(texts, normalize_embeddings=True), dtype=np.float32)
    faiss_dir = ARTIFACTS_DIR / "faiss"
    faiss_dir.mkdir(exist_ok=True)
    index = faiss.IndexFlatIP(embeddings.shape[1])
    index.add(embeddings)
    faiss.write_index(index, str(faiss_dir / "jobs.index"))
    mapping = {str(row.job_id): idx for idx, row in enumerate(jobs_df.itertuples())}
    (faiss_dir / "jobs_mapping.json").write_text(json.dumps(mapping, indent=2))

    metadata = {"version": VERSION}
    (ARTIFACTS_DIR / "metadata.json").write_text(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    train()
