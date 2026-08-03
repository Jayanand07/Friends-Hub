-- =============================================================================
-- V4__enable_rls_all_tables.sql
-- FriendsHub — Enable Row-Level Security (RLS) on all remaining public tables
-- =============================================================================

ALTER TABLE public.saved_posts          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.refresh_tokens        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.flyway_schema_history DISABLE ROW LEVEL SECURITY;
