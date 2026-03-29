from __future__ import annotations

import re
from typing import List

import numpy as np

from app.models.model_registry import ModelRegistry
from app.utils.schemas import FraudRequest, FraudResponse


class FraudService:
    def __init__(self, registry: ModelRegistry) -> None:
        self.registry = registry

    def evaluate(self, request: FraudRequest) -> FraudResponse:
        signals: List[str] = []
        text = request.resume_text or ""
        features = np.asarray([[len(text), text.lower().count("led"), text.lower().count("guaranteed")]], dtype=np.float32)

        if self.registry.fraud_model is not None:
            prediction = self.registry.fraud_model.predict(features)[0]
            raw_score = float(-self.registry.fraud_model.decision_function(features)[0])
        else:
            prediction = -1 if text.lower().count("guaranteed") or len(text) < 120 else 1
            raw_score = min(0.95, 0.15 + (text.lower().count("guaranteed") * 0.2))

        if len(text) < 120:
            signals.append("Resume is unusually short")
        if text.lower().count("guaranteed"):
            signals.append("Contains exaggerated guarantee phrasing")
        if self._has_timeline_inconsistency(text):
            signals.append("Potential career timeline inconsistency detected")
        if self._has_repetitive_bullets(text):
            signals.append("Multiple near-duplicate achievement statements")

        suspicious = prediction == -1 or len(signals) >= 2
        fraud_score = min(0.99, max(raw_score, len(signals) * 0.18))
        return FraudResponse(
            suspicious=suspicious,
            fraudScore=fraud_score,
            signals=signals,
            modelVersion=self.registry.version,
        )

    def _has_timeline_inconsistency(self, text: str) -> bool:
        years = [int(match) for match in re.findall(r"\b(?:19|20)\d{2}\b", text)]
        return any(years[index] > years[index + 1] for index in range(len(years) - 1))

    def _has_repetitive_bullets(self, text: str) -> bool:
        lines = [line.strip().lower() for line in text.splitlines() if len(line.strip()) > 15]
        return len(lines) != len(set(lines))
