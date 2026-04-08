-- Migration V2: Add new fields to enrollment table and create document_progress table
-- Created: 2026-04-07
-- Description: Add course expiry, learning time tracking, and progress fields

-- Add columns to enrollments table if they don't exist
ALTER TABLE enrollments 
ADD COLUMN IF NOT EXISTS access_duration_days INTEGER,
ADD COLUMN IF NOT EXISTS progress_percentage DECIMAL(5,2) DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS total_time_spent_seconds BIGINT DEFAULT 0,
ADD COLUMN IF NOT EXISTS last_accessed_at TIMESTAMP;

-- Create document_progress table for tracking document reading
CREATE TABLE IF NOT EXISTS document_progress (
    id BIGSERIAL PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE,
    reading_time_seconds INTEGER DEFAULT 0,
    read_count INTEGER DEFAULT 0,
    last_read_at TIMESTAMP,
    CONSTRAINT fk_document_progress_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE CASCADE,
    CONSTRAINT fk_document_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
    UNIQUE(enrollment_id, lesson_id)
);

-- Add index for faster queries
CREATE INDEX IF NOT EXISTS idx_document_progress_enrollment ON document_progress(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_document_progress_lesson ON document_progress(lesson_id);

-- Add index on enrollments for learning time queries
CREATE INDEX IF NOT EXISTS idx_enrollments_total_time ON enrollments(total_time_spent_seconds);
CREATE INDEX IF NOT EXISTS idx_enrollments_expires_at ON enrollments(expires_at);
