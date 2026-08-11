ALTER TABLE time_entries
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
