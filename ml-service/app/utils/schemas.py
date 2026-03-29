from __future__ import annotations

from typing import Dict, List, Optional

from pydantic import BaseModel, ConfigDict, Field


class SmartHireBaseModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True)


class RankCandidateInput(SmartHireBaseModel):
    candidate_id: int = Field(alias="candidateId")
    candidate_name: str = Field(alias="candidateName")
    resume_text: str = Field(alias="resumeText", default="")
    skills: List[str] = Field(default_factory=list)
    features: Dict[str, float] = Field(default_factory=dict)


class RankRequest(SmartHireBaseModel):
    job_id: int = Field(alias="jobId")
    job_title: str = Field(alias="jobTitle")
    job_description: str = Field(alias="jobDescription")
    required_skills: List[str] = Field(alias="requiredSkills", default_factory=list)
    candidates: List[RankCandidateInput]


class RankResultItem(SmartHireBaseModel):
    candidate_id: int = Field(alias="candidateId")
    score: float
    explanation: str
    strengths: List[str]
    gaps: List[str]


class RankResponse(SmartHireBaseModel):
    model_version: str = Field(alias="modelVersion")
    results: List[RankResultItem]


class JobCandidate(SmartHireBaseModel):
    job_id: int = Field(alias="jobId")
    title: str
    company: str
    description: str
    skills: List[str] = Field(default_factory=list)


class RecommendationRequest(SmartHireBaseModel):
    candidate_id: int = Field(alias="candidateId")
    resume_text: str = Field(alias="resumeText", default="")
    applied_job_ids: List[int] = Field(alias="appliedJobIds", default_factory=list)
    jobs: List[JobCandidate] = Field(default_factory=list)


class RecommendationResult(SmartHireBaseModel):
    job_id: int = Field(alias="jobId")
    score: float
    reason: str


class RecommendationResponse(SmartHireBaseModel):
    model_version: str = Field(alias="modelVersion")
    recommendations: List[RecommendationResult]


class InterviewRequest(SmartHireBaseModel):
    job_title: str = Field(alias="jobTitle")
    question: str
    answer: str


class InterviewResponse(SmartHireBaseModel):
    score: float
    summary: str
    strengths: List[str]
    improvements: List[str]
    model_version: str = Field(alias="modelVersion")


class VideoRequest(SmartHireBaseModel):
    video_url: str = Field(alias="videoUrl")
    transcript: Optional[str] = None


class VideoResponse(SmartHireBaseModel):
    dominant_emotion: str = Field(alias="dominantEmotion")
    confidence: float
    emotions: Dict[str, float]
    model_version: str = Field(alias="modelVersion")


class FraudRequest(SmartHireBaseModel):
    candidate_name: Optional[str] = Field(alias="candidateName", default=None)
    resume_text: str = Field(alias="resumeText")


class FraudResponse(SmartHireBaseModel):
    suspicious: bool
    fraud_score: float = Field(alias="fraudScore")
    signals: List[str]
    model_version: str = Field(alias="modelVersion")


class SpamRequest(SmartHireBaseModel):
    room_id: str = Field(alias="roomId")
    sender_id: int = Field(alias="senderId")
    content: str
    recent_messages: List[str] = Field(alias="recentMessages", default_factory=list)


class SpamResponse(SmartHireBaseModel):
    allow: bool
    action: str
    reason: str
    score: float


class EmbeddingRequest(SmartHireBaseModel):
    subject_type: str = Field(alias="subjectType")
    subject_id: int = Field(alias="subjectId")
    text: str


class EmbeddingResponse(SmartHireBaseModel):
    model_name: str = Field(alias="modelName")
    model_version: str = Field(alias="modelVersion")
    embedding: List[float]
