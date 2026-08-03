import { useEffect, useRef, useState } from "react";
import { supabase } from "../lib/supabaseClient";
import api from "../api/axios";

export default function AuthCallback() {
  const processedRef = useRef(false);
  const [statusMsg, setStatusMsg] = useState("Authenticating with Google...");

  useEffect(() => {
    let isMounted = true;

    const handleAuthUser = async (user, session) => {
      if (processedRef.current || !user || !user.email) return;
      processedRef.current = true;

      try {
        const res = await api.post("/auth/oauth/google", {
          email: user.email,
          name: user.user_metadata?.full_name || user.user_metadata?.name || user.email.split("@")[0],
          idToken: session?.access_token || null
        });

        if (!isMounted) return;

        const { token, refreshToken } = res.data;
        if (!token) throw new Error("No token returned from backend");

        localStorage.setItem("token", token);
        if (refreshToken) localStorage.setItem("refreshToken", refreshToken);

        // Hard redirect — ensures ProtectedRoute reads token from localStorage fresh
        window.location.replace("/");
      } catch (err) {
        console.error("Backend OAuth login failed:", err.response?.data || err.message);
        if (!isMounted) return;
        const msg = err.response?.data?.message || err.message || "OAuth login failed";
        setStatusMsg(msg);
        setTimeout(() => {
          window.location.replace("/login");
        }, 2000);
      }
    };

    // First: check for an already-established session (handles page reloads/redirects)
    supabase.auth.getSession().then(({ data: { session }, error }) => {
      if (error) console.error("Supabase getSession error:", error);
      if (session?.user) {
        handleAuthUser(session.user, session);
      }
    });

    // Second: listen for the SIGNED_IN event from Supabase OAuth redirect
    const { data: { subscription } } = supabase.auth.onAuthStateChange((event, session) => {
      if ((event === "SIGNED_IN" || event === "TOKEN_REFRESHED") && session?.user) {
        handleAuthUser(session.user, session);
      }
    });

    return () => {
      isMounted = false;
      subscription.unsubscribe();
    };
  }, []);

  return (
    <div className="flex items-center justify-center min-h-screen bg-[var(--bg-primary)]">
      <div className="flex flex-col items-center gap-4 text-center px-4">
        <div className="w-10 h-10 border-4 border-[var(--accent)] border-t-transparent rounded-full animate-spin" />
        <p className="text-sm font-semibold text-[var(--accent)]">{statusMsg}</p>
      </div>
    </div>
  );
}
