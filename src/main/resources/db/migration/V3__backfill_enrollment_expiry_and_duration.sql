-- Migration V3: Backfill enrollment expiry and duration for existing records
-- This script assumes a PostgreSQL database and should be run against the LMS database.

-- Ensure missing numeric values default to zero.
UPDATE enrollments
SET total_time_spent_seconds = 0
WHERE total_time_spent_seconds IS NULL;

UPDATE enrollments
SET progress_percentage = 0.0
WHERE progress_percentage IS NULL;

-- Populate missing access duration and expires_at for enrollments that already have enrolled_at
UPDATE enrollments e
SET access_duration_days = COALESCE(access_duration_days, CASE c.level
        WHEN 'BEGINNER' THEN 180
        WHEN 'INTERMEDIATE' THEN 270
        WHEN 'ADVANCED' THEN 365
        ELSE 180
    END),
    expires_at = COALESCE(expires_at, e.enrolled_at + CASE c.level
        WHEN 'BEGINNER' THEN INTERVAL '180 days'
        WHEN 'INTERMEDIATE' THEN INTERVAL '270 days'
        WHEN 'ADVANCED' THEN INTERVAL '365 days'
        ELSE INTERVAL '180 days'
    END)
FROM courses c
WHERE e.course_id = c.id
  AND e.enrolled_at IS NOT NULL
  AND (e.expires_at IS NULL OR e.access_duration_days IS NULL);
