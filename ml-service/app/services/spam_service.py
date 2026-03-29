from __future__ import annotations

import re
from typing import List

import numpy as np

from app.models.model_registry import ModelRegistry
from app.utils.schemas import SpamRequest, SpamResponse


class SpamService:
    LINK_PATTERN = re.compile(r"https?://|www\.", re.IGNORECASE)
    SCAM_PATTERN = re.compile(r"crypto|gift card|wire transfer|telegram", re.IGNORECASE)
    TOXIC_PATTERN = re.compile(r"idiot|stupid|hate you|moron", re.IGNORECASE)

    def __init__(self, registry: ModelRegistry) -> None:
        self.registry = registry

    def detect(self, request: SpamRequest) -> SpamResponse:
        normalized = request.content.strip().lower()
        if request.recent_messages and any(message.strip().lower() == normalized for message in request.recent_messages):
            return SpamResponse(allow=False, action="BLOCK", reason="Repeated message detected", score=0.94)

        if self.LINK_PATTERN.search(normalized) and self.SCAM_PATTERN.search(normalized):
            return SpamResponse(allow=False, action="BLOCK", reason="Suspicious scam link detected", score=0.99)

        if self.registry.spam_model is not None and self.registry.spam_vectorizer is not None:
            features = self.registry.spam_vectorizer.transform([request.content])
            probability = float(self.registry.spam_model.predict_proba(features)[0][1])
            if probability > 0.9:
                return SpamResponse(allow=False, action="BLOCK", reason="Spam classifier blocked message", score=probability)
            if probability > 0.65:
                return SpamResponse(allow=False, action="WARN", reason="Spam classifier flagged message", score=probability)

        if self.TOXIC_PATTERN.search(normalized):
            return SpamResponse(allow=False, action="WARN", reason="Toxic language detected", score=0.81)

        if normalized.count("!") > 8 or len(normalized) > 500:
            return SpamResponse(allow=False, action="WARN", reason="Possible spam flooding", score=0.73)

        return SpamResponse(allow=True, action="ALLOW", reason="Message accepted", score=0.05)
