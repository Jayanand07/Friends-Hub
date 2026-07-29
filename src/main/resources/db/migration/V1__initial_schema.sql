-- =============================================================================
-- V1__initial_schema.sql
-- FriendsHub — Initial Database Migration & Security Baseline
-- =============================================================================

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_chat_messages_sender_receiver ON public.chat_messages (sender_id, receiver_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_chat_messages_receiver_sender ON public.chat_messages (receiver_id, sender_id, timestamp);

CREATE INDEX IF NOT EXISTS idx_chat_group_messages_group_created ON public.chat_group_messages (group_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_group_members_user_id ON public.chat_group_members (user_id);
CREATE INDEX IF NOT EXISTS idx_chat_group_members_group_id ON public.chat_group_members (group_id);

CREATE INDEX IF NOT EXISTS idx_users_email_lower ON public.users (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_public_key ON public.users (id) WHERE public_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_follows_follower_id ON public.follows (follower_id);
CREATE INDEX IF NOT EXISTS idx_follows_following_id ON public.follows (following_id);

-- Row Level Security (RLS) Enablement
ALTER TABLE public.users              ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.posts              ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stories            ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_views        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comments           ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.likes              ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.follows            ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_messages      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_groups        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_group_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_group_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.blocks             ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_info          ENABLE ROW LEVEL SECURITY;
