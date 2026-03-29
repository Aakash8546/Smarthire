from __future__ import annotations

from typing import List

import numpy as np

from app.models.model_registry import ModelRegistry


class EmbeddingService:
    def __init__(self, registry: ModelRegistry) -> None:
        self.registry = registry

    def encode(self, text: str) -> List[float]:
        vector = self.registry.sentence_model.encode(text or "", normalize_embeddings=True)
        return np.asarray(vector, dtype=np.float32).tolist()
