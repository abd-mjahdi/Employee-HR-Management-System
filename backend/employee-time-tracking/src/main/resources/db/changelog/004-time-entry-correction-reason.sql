ALTER TABLE time_entries
    ADD COLUMN IF NOT EXISTS correction_reason TEXT;
