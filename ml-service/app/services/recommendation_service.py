from __future__ import annotations

from typing import List

import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

from app.models.model_registry import ModelRegistry
from app.utils.schemas import RecommendationRequest, RecommendationResponse, RecommendationResult


class RecommendationService:
    def __init__(self, registry: ModelRegistry) -> None:
        self.registry = registry

    def recommend(self, request: RecommendationRequest) -> RecommendationResponse:
        profile_text = request.resume_text or "new candidate profile"
        profile_vector = np.asarray(
            self.registry.sentence_model.encode(profile_text, normalize_embeddings=True),
            dtype=np.float32,
        )
        faiss_boost_by_job: dict[int, float] = {}
        if self.registry.faiss_index is not None and self.registry.faiss_mapping:
            top_k = min(20, len(self.registry.faiss_mapping))
            _, indices = self.registry.faiss_index.search(np.asarray([profile_vector], dtype=np.float32), top_k)
            reverse_mapping = {value: int(key) for key, value in self.registry.faiss_mapping.items()}
            for rank, index in enumerate(indices[0].tolist()):
                if index in reverse_mapping:
                    faiss_boost_by_job[reverse_mapping[index]] = max(0.0, 0.1 - (rank * 0.01))

        recommendations: List[RecommendationResult] = []
        applied_job_ids = set(request.applied_job_ids)
        for job in request.jobs:
            if job.job_id in applied_job_ids:
                continue
            job_text = f"{job.title}. {job.description}. {' '.join(job.skills)}"
            job_vector = np.asarray(
                self.registry.sentence_model.encode(job_text, normalize_embeddings=True),
                dtype=np.float32,
            )
            content_score = float(cosine_similarity([profile_vector], [job_vector])[0][0])
            collaborative_score = 0.15 if applied_job_ids else 0.05
            total_score = (content_score * 0.8) + collaborative_score + faiss_boost_by_job.get(job.job_id, 0.0)
            if self.registry.recommendation_model is not None:
                features = np.array([[content_score, collaborative_score, len(job.skills)]], dtype=np.float32)
                total_score = float(self.registry.recommendation_model.predict(features)[0])
            reason = (
                f"Content similarity {content_score:.2f} combined with "
                f"collaborative prior {collaborative_score:.2f}."
            )
            recommendations.append(
                RecommendationResult(jobId=job.job_id, score=total_score, reason=reason)
            )

        recommendations.sort(key=lambda item: item.score, reverse=True)
        return RecommendationResponse(modelVersion=self.registry.version, recommendations=recommendations[:20])
