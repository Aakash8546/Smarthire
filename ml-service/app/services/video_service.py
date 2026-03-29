from __future__ import annotations

import tempfile
from pathlib import Path
from urllib.parse import urlparse
from urllib.request import urlretrieve

import cv2
import numpy as np

from app.models.model_registry import ModelRegistry
from app.utils.schemas import VideoRequest, VideoResponse


class VideoService:
    def __init__(self, registry: ModelRegistry) -> None:
        self.registry = registry

    def analyze(self, request: VideoRequest) -> VideoResponse:
        video_path = self._resolve_video_path(request.video_url)
        capture = cv2.VideoCapture(str(video_path))
        frame_scores = []
        frame_count = 0

        while capture.isOpened() and frame_count < 60:
            ok, frame = capture.read()
            if not ok:
                break
            if frame_count % 10 == 0:
                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                brightness = float(np.mean(gray) / 255.0)
                contrast = float(np.std(gray) / 255.0)
                engaged = min(1.0, (contrast * 1.2) + 0.1)
                neutral = max(0.0, 1.0 - abs(0.55 - brightness))
                stressed = max(0.0, 0.8 - engaged)
                frame_scores.append((engaged, neutral, stressed))
            frame_count += 1

        capture.release()
        if not frame_scores:
            emotions = {"neutral": 0.45, "engaged": 0.35, "stressed": 0.20}
        else:
            means = np.mean(np.asarray(frame_scores), axis=0)
            emotions = {"engaged": float(means[0]), "neutral": float(means[1]), "stressed": float(means[2])}

        dominant = max(emotions, key=emotions.get)
        confidence = float(emotions[dominant])
        return VideoResponse(
            dominantEmotion=dominant,
            confidence=confidence,
            emotions=emotions,
            modelVersion=self.registry.version,
        )

    def _resolve_video_path(self, video_url: str) -> Path:
        parsed = urlparse(video_url)
        if parsed.scheme in {"http", "https"}:
            temp_file = Path(tempfile.gettempdir()) / f"smarthire_{Path(parsed.path).name or 'video.mp4'}"
            urlretrieve(video_url, temp_file)
            return temp_file
        return Path(video_url)
