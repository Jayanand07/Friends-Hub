import { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { supabase } from "../lib/supabaseClient";
import { useAuth } from "../context/AuthContext";
import api from "../api/axios";

export default function AuthCallback() {
  const navigate = useNavigate();
  const { loginUser } = useAuth();
  const processedRef = useRef(false);

  useEffect(() => {
    const handleAuthUser = async (user, session) => {
      if (processedRef.current || !user) return;
      processedRef.current = true;

      try {
        const res = await api.post("/auth/oauth/google", {
          email: user.email,
          name: user.user_metadata?.full_name || user.email.split("@")[0],
          googleId: user.id,
          idToken: session?.provider_token || session?.access_token || null
        });
        
        const token = res.data.token;
        loginUser(token);
        navigate("/", { replace: true });
      } catch (err) {
        console.error("Backend OAuth login failed:", err);
        navigate("/login", { replace: true });
      }
    };

    // Fast path 1: check current session immediately
    supabase.auth.getSession().then(({ data: { session } }) => {
      if (session?.user) {
        handleAuthUser(session.user, session);
      }
    });

    // Fast path 2: subscribe to instant auth state change event
    const { data: { subscription } } = supabase.auth.onAuthStateChange((event, session) => {
      if (session?.user) {
        handleAuthUser(session.user, session);
      }
    });

    return () => {
      subscription.unsubscribe();
    };
  }, [navigate, loginUser]);

  return (
    <div className="flex items-center justify-center min-h-screen bg-[var(--bg-primary)]">
      <div className="flex flex-col items-center gap-3">
        <div className="w-8 h-8 border-3 border-[var(--accent)] border-t-transparent rounded-full animate-spin" />
        <p className="text-xs font-semibold text-[var(--accent)] animate-pulse">Authenticating...</p>
      </div>
    </div>
  );
}

