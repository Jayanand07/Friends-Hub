-- =============================================================================
-- V4__enable_rls_all_tables.sql
-- FriendsHub — Enable Row-Level Security (RLS) on remaining public tables
-- =============================================================================

ALTER TABLE public.saved_posts   ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.refresh_tokens ENABLE ROW LEVEL SECURITY;
