import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { supabase } from "../lib/supabaseClient";
import { useAuth } from "../context/AuthContext";
import api from "../api/axios";

export default function AuthCallback() {
  const navigate = useNavigate();
  const { loginUser } = useAuth();
  const processedRef = useRef(false);
  const [statusMsg, setStatusMsg] = useState("Authenticating with Google...");

  useEffect(() => {
    let isMounted = true;

    const handleAuthUser = async (user, session) => {
      if (processedRef.current || !user) return;
      processedRef.current = true;

      try {
        const res = await api.post("/auth/oauth/google", {
          email: user.email,
          name: user.user_metadata?.full_name || user.user_metadata?.name || user.email.split("@")[0],
          idToken: session?.access_token || session?.provider_token || "supabase_oauth"
        });
        
        if (!isMounted) return;
        const token = res.data.token;
        const refreshToken = res.data.refreshToken;
        loginUser(token, refreshToken);
        
        // Use clean window redirect to home page
        window.location.href = "/";
      } catch (err) {
        console.error("Backend OAuth login failed:", err);
        processedRef.current = false;
        if (isMounted) {
          const msg = err.response?.data?.message || "OAuth login failed. Returning to login...";
          setStatusMsg(msg);
          setTimeout(() => {
            navigate("/login", { replace: true });
          }, 1500);
        }
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
      isMounted = false;
      subscription.unsubscribe();
    };
  }, [navigate, loginUser]);

  return (
    <div className="flex items-center justify-center min-h-screen bg-[var(--bg-primary)]">
      <div className="flex flex-col items-center gap-3 text-center px-4">
        <div className="w-8 h-8 border-3 border-[var(--accent)] border-t-transparent rounded-full animate-spin" />
        <p className="text-xs font-semibold text-[var(--accent)] animate-pulse">{statusMsg}</p>
      </div>
    </div>
  );
}
