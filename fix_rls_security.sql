-- =============================================================================
-- fix_rls_security.sql
-- Social-Hub (friendshub.me) — Full RLS Security Migration
-- =============================================================================
-- HOW TO RUN:
--   Supabase Dashboard → SQL Editor → paste this entire file → Run
--
-- ARCHITECTURE NOTE:
--   Spring Boot uses the SERVICE ROLE KEY (bypasses RLS entirely).
--   The Supabase ANON KEY is used by no one right now, but this hardens
--   the database against anyone who might try hitting the PostgREST API directly.
--
--   Pattern used: "Deny All from client, Allow All from service role"
--   RLS enabled + zero SELECT/INSERT/UPDATE/DELETE policies = deny all to anon/authenticated.
--   The service_role key auto-bypasses RLS, so Spring Boot is completely unaffected.
--
-- AFTER RUNNING THIS FILE:
--   • The "UNRESTRICTED" badges in Supabase Table Editor → turn to RLS
--   • The 18 security linter warnings disappear
--   • PostgREST REST API will return 0 rows (anon) or 403 (no policy) for all tables
-- =============================================================================

-- =====================================================================
-- PHASE 1: Enable RLS on ALL 18 tables
-- =====================================================================

ALTER TABLE public.users              ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.posts              ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stories            ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_views        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reactions          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comments           ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.likes              ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.follows            ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.follow_requests    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_messages      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_groups        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_group_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_group_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.blocks             ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_info          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.api_usage_log      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_action_logs  ENABLE ROW LEVEL SECURITY;

-- =====================================================================
-- PHASE 2: Drop critical security exposure — password column
-- =====================================================================
-- The `password` column in public.users contains BCrypt hashes.
-- Spring Boot manages auth itself (BCrypt + JWT). Supabase does NOT
-- need this column. Dropping it eliminates the most critical exposure.
--
-- SAFE TO RUN: Spring Boot reads/writes `password` via JPA using the
-- service role key, which connects directly via JDBC (not PostgREST).
-- The JDBC connection bypasses PostgREST entirely — it talks to Postgres
-- directly. BUT wait — dropping the column will break Spring Boot's JPA
-- User entity which has `@Column nullable=false private String password`.
--
-- RECOMMENDED APPROACH (two options):
--
-- OPTION A (RECOMMENDED): Drop the column AND update the Spring Boot
--   User entity to remove the password field, using a separate
--   credential store (e.g. a private `user_credentials` table with RLS).
--   This is the cleanest long-term fix.
--
-- OPTION B (QUICK FIX): Keep the column in Postgres but hide it from
--   PostgREST by revoking SELECT on that specific column for anon/authenticated.
--   This is immediately safe and requires NO Spring Boot changes.
--
-- We apply OPTION B below (safe, immediate, zero downtime).
-- Uncomment OPTION A's ALTER TABLE DROP COLUMN only if you update the
-- Spring Boot User entity simultaneously.
-- =====================================================================

-- OPTION B: Revoke SELECT on the password column for anon and authenticated roles
-- (PostgREST won't include it in any response)
REVOKE SELECT (password) ON public.users FROM anon;
REVOKE SELECT (password) ON public.users FROM authenticated;

-- Also revoke on sensitive token columns (verification tokens, reset tokens)
REVOKE SELECT (verification_token)    ON public.users FROM anon;
REVOKE SELECT (verification_token)    ON public.users FROM authenticated;
REVOKE SELECT (token_expiry)          ON public.users FROM anon;
REVOKE SELECT (token_expiry)          ON public.users FROM authenticated;
REVOKE SELECT (password_reset_token)  ON public.users FROM anon;
REVOKE SELECT (password_reset_token)  ON public.users FROM authenticated;
REVOKE SELECT (reset_token_expiry)    ON public.users FROM anon;
REVOKE SELECT (reset_token_expiry)    ON public.users FROM authenticated;

-- OPTION A (COMMENTED OUT): Only uncomment after updating Spring Boot User entity
-- to remove the `password` field and use a separate credentials table
-- ALTER TABLE public.users DROP COLUMN IF EXISTS password;
-- ALTER TABLE public.users DROP COLUMN IF EXISTS verification_token;
-- ALTER TABLE public.users DROP COLUMN IF EXISTS token_expiry;
-- ALTER TABLE public.users DROP COLUMN IF EXISTS password_reset_token;
-- ALTER TABLE public.users DROP COLUMN IF EXISTS reset_token_expiry;

-- =====================================================================
-- PHASE 3: Backend-only tables — Deny ALL (no policies = deny all)
-- =====================================================================
-- These tables are ONLY accessed via Spring Boot (service role key).
-- The service role key bypasses RLS, so no policies needed.
-- Adding ANY policy would open access — so we intentionally add NONE.
-- Tables: admin_action_logs, api_usage_log, notifications, chat_messages,
--         chat_groups, chat_group_members, chat_group_messages, follows,
--         follow_requests, blocks, user_info, story_views, reactions,
--         likes, comments, users
-- =====================================================================

-- Drop any accidentally existing policies first (idempotent cleanup)
DO $$
DECLARE
    pol record;
BEGIN
    FOR pol IN
        SELECT schemaname, tablename, policyname
        FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename IN (
              'users', 'admin_action_logs', 'api_usage_log', 'notifications',
              'chat_messages', 'chat_groups', 'chat_group_members', 'chat_group_messages',
              'follows', 'follow_requests', 'blocks', 'user_info', 'story_views',
              'reactions', 'likes', 'comments', 'stories', 'posts'
          )
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON %I.%I',
                       pol.policyname, pol.schemaname, pol.tablename);
    END LOOP;
END;
$$;

-- =====================================================================
-- PHASE 4: Limited public-read tables (posts, stories)
-- =====================================================================
-- These tables have data that is conceptually "public" (visible to
-- logged-in users browsing the feed), but we route ALL access through
-- Spring Boot (which uses the service role key).
--
-- Since NO direct Supabase client calls exist in the React frontend
-- (confirmed by audit — all calls go through axios to Spring Boot),
-- the safest approach is ALSO deny-all here.
--
-- If you ever want to add a read-only public API directly from PostgREST
-- in the future, uncomment the policies below.
-- =====================================================================

-- DENY ALL on posts (same as backend-only — all access via Spring Boot)
-- No policies added intentionally.

-- DENY ALL on stories (same rationale)
-- No policies added intentionally.

-- =====================================================================
-- If you ever need public read access for posts/stories via PostgREST,
-- uncomment these policies:
-- =====================================================================
/*
-- Public read on posts (no auth required — only SELECT, no write)
CREATE POLICY "public_read_posts"
    ON public.posts
    FOR SELECT
    TO anon, authenticated
    USING (true);

-- Public read on stories (SELECT only for non-expired stories)
CREATE POLICY "public_read_stories"
    ON public.stories
    FOR SELECT
    TO anon, authenticated
    USING (expires_at > now());
*/

-- =====================================================================
-- PHASE 5: Force RLS even for table owners (belt-and-suspenders)
-- =====================================================================
-- By default, table owners bypass RLS. The `postgres` role (used by
-- Supabase internally) is the owner. This ensures RLS applies to all
-- roles except service_role (which is exempt by design).
-- =====================================================================

ALTER TABLE public.users               FORCE ROW LEVEL SECURITY;
ALTER TABLE public.posts               FORCE ROW LEVEL SECURITY;
ALTER TABLE public.stories             FORCE ROW LEVEL SECURITY;
ALTER TABLE public.story_views         FORCE ROW LEVEL SECURITY;
ALTER TABLE public.reactions           FORCE ROW LEVEL SECURITY;
ALTER TABLE public.comments            FORCE ROW LEVEL SECURITY;
ALTER TABLE public.likes               FORCE ROW LEVEL SECURITY;
ALTER TABLE public.follows             FORCE ROW LEVEL SECURITY;
ALTER TABLE public.follow_requests     FORCE ROW LEVEL SECURITY;
ALTER TABLE public.notifications       FORCE ROW LEVEL SECURITY;
ALTER TABLE public.chat_messages       FORCE ROW LEVEL SECURITY;
ALTER TABLE public.chat_groups         FORCE ROW LEVEL SECURITY;
ALTER TABLE public.chat_group_members  FORCE ROW LEVEL SECURITY;
ALTER TABLE public.chat_group_messages FORCE ROW LEVEL SECURITY;
ALTER TABLE public.blocks              FORCE ROW LEVEL SECURITY;
ALTER TABLE public.user_info           FORCE ROW LEVEL SECURITY;
ALTER TABLE public.api_usage_log       FORCE ROW LEVEL SECURITY;
ALTER TABLE public.admin_action_logs   FORCE ROW LEVEL SECURITY;

-- =====================================================================
-- PHASE 6: Verify — run this SELECT after applying to confirm RLS is on
-- =====================================================================
-- NOTE: pg_tables only has `rowsecurity`. The `forcerowsecurity` flag lives
-- in pg_class (as relforcerowsecurity). We join both here.
SELECT
    n.nspname                  AS schemaname,
    c.relname                  AS tablename,
    c.relrowsecurity           AS rls_enabled,
    c.relforcerowsecurity      AS rls_forced
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relname IN (
      'users', 'posts', 'stories', 'story_views', 'reactions',
      'comments', 'likes', 'follows', 'follow_requests', 'notifications',
      'chat_messages', 'chat_groups', 'chat_group_members',
      'chat_group_messages', 'blocks', 'user_info',
      'api_usage_log', 'admin_action_logs'
  )
  AND c.relkind = 'r'  -- only regular tables (not views, sequences, etc.)
ORDER BY c.relname;

-- Expected output: ALL 18 rows → rls_enabled = true, rls_forced = true
-- =============================================================================
-- END OF MIGRATION
-- =============================================================================
