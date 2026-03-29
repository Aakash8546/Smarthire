from __future__ import annotations

import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

from app.models.model_registry import ModelRegistry
from app.utils.schemas import InterviewRequest, InterviewResponse


class InterviewService:
    def __init__(self, registry: ModelRegistry) -> None:
        self.registry = registry

    def evaluate(self, request: InterviewRequest) -> InterviewResponse:
        expected = f"{request.job_title}. {request.question}"
        expected_vector = np.asarray(
            self.registry.sentence_model.encode(expected, normalize_embeddings=True),
            dtype=np.float32,
        )
        answer_vector = np.asarray(
            self.registry.sentence_model.encode(request.answer, normalize_embeddings=True),
            dtype=np.float32,
        )
        semantic_score = float(cosine_similarity([expected_vector], [answer_vector])[0][0])
        completeness = min(len(request.answer.split()) / 120.0, 1.0)
        final_score = (semantic_score * 0.7) + (completeness * 0.3)

        strengths = []
        improvements = []
        if semantic_score > 0.55:
            strengths.append("Answer stayed on-topic and covered relevant concepts")
        else:
            improvements.append("Increase alignment between the answer and the interview question")
        if completeness > 0.45:
            strengths.append("Provided enough detail to evaluate reasoning")
        else:
            improvements.append("Add more concrete examples and technical depth")

        summary = (
            f"Semantic relevance={semantic_score:.2f}, completeness={completeness:.2f}. "
            "Score blends answer quality and response depth."
        )
        return InterviewResponse(
            score=final_score,
            summary=summary,
            strengths=strengths or ["Candidate provided a minimally viable answer"],
            improvements=improvements,
            modelVersion=self.registry.version,
        )
