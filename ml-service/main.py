from __future__ import annotations

import logging
from functools import lru_cache

from fastapi import FastAPI

from app.models.model_registry import ModelRegistry
from app.services.embedding_service import EmbeddingService
from app.services.fraud_service import FraudService
from app.services.interview_service import InterviewService
from app.services.ranking_service import RankingService
from app.services.recommendation_service import RecommendationService
from app.services.spam_service import SpamService
from app.services.video_service import VideoService
from app.utils.logging import configure_logging
from app.utils.schemas import (
    EmbeddingRequest,
    EmbeddingResponse,
    FraudRequest,
    FraudResponse,
    InterviewRequest,
    InterviewResponse,
    RankRequest,
    RankResponse,
    RecommendationRequest,
    RecommendationResponse,
    SpamRequest,
    SpamResponse,
    VideoRequest,
    VideoResponse,
)

configure_logging()
logger = logging.getLogger("smarthire-ml")

app = FastAPI(title="SmartHire ML Service", version="1.0.0")


@lru_cache(maxsize=1)
def registry() -> ModelRegistry:
    return ModelRegistry.load()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "smarthire-ml"}


@app.post("/embeddings/generate", response_model=EmbeddingResponse)
def generate_embedding(request: EmbeddingRequest) -> EmbeddingResponse:
    service = EmbeddingService(registry())
    embedding = service.encode(request.text)
    return EmbeddingResponse(
        modelName="sentence-transformers/all-MiniLM-L6-v2",
        modelVersion=registry().version,
        embedding=embedding,
    )


@app.post("/rank", response_model=RankResponse)
def rank(request: RankRequest) -> RankResponse:
    logger.info("Ranking request job_id=%s candidates=%s", request.job_id, len(request.candidates))
    return RankingService(registry()).rank(request)


@app.post("/recommendations", response_model=RecommendationResponse)
def recommendations(request: RecommendationRequest) -> RecommendationResponse:
    logger.info("Recommendation request candidate_id=%s jobs=%s", request.candidate_id, len(request.jobs))
    return RecommendationService(registry()).recommend(request)


@app.post("/interview/evaluate", response_model=InterviewResponse)
def evaluate_interview(request: InterviewRequest) -> InterviewResponse:
    return InterviewService(registry()).evaluate(request)


@app.post("/video/analyze", response_model=VideoResponse)
def analyze_video(request: VideoRequest) -> VideoResponse:
    return VideoService(registry()).analyze(request)


@app.post("/resume/fraud-check", response_model=FraudResponse)
def fraud_check(request: FraudRequest) -> FraudResponse:
    return FraudService(registry()).evaluate(request)


@app.post("/spam/detect", response_model=SpamResponse)
def detect_spam(request: SpamRequest) -> SpamResponse:
    return SpamService(registry()).detect(request)
