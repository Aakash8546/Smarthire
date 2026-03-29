from __future__ import annotations

from typing import List

import faiss
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

from app.models.model_registry import ModelRegistry
from app.utils.schemas import RankRequest, RankResponse, RankResultItem


class RankingService:
    def __init__(self, registry: ModelRegistry) -> None:
        self.registry = registry

    def rank(self, request: RankRequest) -> RankResponse:
        job_text = f"{request.job_title}. {request.job_description}. {' '.join(request.required_skills)}"
        job_embedding = np.asarray(self.registry.sentence_model.encode(job_text, normalize_embeddings=True), dtype=np.float32)
        rows = []
        heuristics = []
        candidate_embeddings = []
        for candidate in request.candidates:
            candidate_text = f"{candidate.candidate_name}. {candidate.resume_text}. {' '.join(candidate.skills)}"
            candidate_embedding = np.asarray(
                self.registry.sentence_model.encode(candidate_text, normalize_embeddings=True),
                dtype=np.float32,
            )
            candidate_embeddings.append(candidate_embedding)
            similarity = float(cosine_similarity([job_embedding], [candidate_embedding])[0][0])
            skill_overlap = candidate.features.get("skill_overlap", 0.0)
            resume_score = candidate.features.get("resume_ai_score", 0.0)
            match_percentage = candidate.features.get("match_percentage", 0.0)
            rows.append([
                similarity,
                skill_overlap,
                resume_score,
                match_percentage,
                candidate.features.get("resume_length", 0.0) / 1000.0,
            ])
            heuristics.append((candidate, similarity, skill_overlap, resume_score, match_percentage))

        matrix = np.asarray(rows, dtype=np.float32)
        if candidate_embeddings:
            faiss_index = faiss.IndexFlatIP(candidate_embeddings[0].shape[0])
            faiss_index.add(np.asarray(candidate_embeddings, dtype=np.float32))
            _, neighbor_indices = faiss_index.search(np.asarray([job_embedding], dtype=np.float32), len(candidate_embeddings))
            neighbor_rank = {candidate_index: rank for rank, candidate_index in enumerate(neighbor_indices[0].tolist())}
        else:
            neighbor_rank = {}
        if self.registry.ranking_model is not None and len(matrix) > 0:
            predictions = self.registry.ranking_model.predict(matrix)
        else:
            predictions = np.asarray([
                (sim * 0.45) + (overlap * 0.30) + (resume * 0.10) + (match * 0.15)
                for _, sim, overlap, resume, match in heuristics
            ])

        ranked_items: List[RankResultItem] = []
        for index, (candidate, similarity, skill_overlap, resume_score, _) in enumerate(heuristics):
            faiss_boost = max(0.0, 0.08 - (neighbor_rank.get(index, len(heuristics)) * 0.01))
            strengths = []
            gaps = []
            if similarity >= 0.55:
                strengths.append("Resume semantics strongly align with the job description")
            if skill_overlap >= 0.5:
                strengths.append("Strong required-skill overlap")
            if resume_score >= 0.7:
                strengths.append("High resume quality score")
            if not candidate.resume_text.strip():
                gaps.append("No resume uploaded")
            if skill_overlap < 0.3:
                gaps.append("Limited skill overlap with required job skills")

            explanation = (
                f"Similarity={similarity:.2f}, skill_overlap={skill_overlap:.2f}, "
                f"resume_quality={resume_score:.2f}."
            )
            ranked_items.append(
                RankResultItem(
                    candidateId=candidate.candidate_id,
                    score=float(predictions[index] + faiss_boost),
                    explanation=explanation,
                    strengths=strengths or ["Candidate meets baseline requirements"],
                    gaps=gaps,
                )
            )

        ranked_items.sort(key=lambda item: item.score, reverse=True)
        return RankResponse(modelVersion=self.registry.version, results=ranked_items)
