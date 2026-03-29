-- database/schema.sql
-- SmartHire Database Schema

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50) NOT NULL DEFAULT 'CANDIDATE',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Jobs table
CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    requirements TEXT,
    location VARCHAR(255),
    salary_range VARCHAR(100),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    recruiter_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Resumes table
CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    file_url VARCHAR(500),
    parsed_text TEXT,
    skills TEXT[],
    experience_years DECIMAL(3,1),
    education_level VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Applications table
CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    resume_id BIGINT REFERENCES resumes(id),
    status VARCHAR(50) DEFAULT 'PENDING',
    score DECIMAL(5,2),
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(job_id, user_id)
);

-- Embeddings table for FAISS
CREATE TABLE embeddings (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL, -- 'RESUME', 'JOB', 'USER'
    entity_id BIGINT NOT NULL,
    embedding_vector VECTOR(768),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(entity_type, entity_id)
);

-- Recruiter feedback table
CREATE TABLE recruiter_feedback (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES applications(id),
    recruiter_id BIGINT NOT NULL REFERENCES users(id),
    rating INTEGER CHECK (rating BETWEEN 1 AND 5),
    feedback TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Recommendations cache
CREATE TABLE recommendations_cache (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    recommendation_data JSONB NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Chat rooms table
CREATE TABLE chat_rooms (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT REFERENCES jobs(id),
    candidate_id BIGINT REFERENCES users(id),
    recruiter_id BIGINT REFERENCES users(id),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Messages table
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL REFERENCES chat_rooms(id),
    sender_id BIGINT NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    is_spam BOOLEAN DEFAULT false,
    is_blocked BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_recruiter ON jobs(recruiter_id);
CREATE INDEX idx_applications_job ON applications(job_id);
CREATE INDEX idx_applications_user ON applications(user_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_resumes_user ON resumes(user_id);
CREATE INDEX idx_chat_rooms_job ON chat_rooms(job_id);
CREATE INDEX idx_chat_rooms_candidate ON chat_rooms(candidate_id);
CREATE INDEX idx_messages_room ON messages(chat_room_id);
CREATE INDEX idx_messages_created ON messages(created_at);