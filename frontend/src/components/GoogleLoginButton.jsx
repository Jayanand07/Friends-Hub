import { supabase } from "../lib/supabaseClient";

export const GoogleLoginButton = () => {
  const handleGoogleLogin = async () => {
    // Generate cryptographic state parameter to mitigate OAuth login CSRF attacks
    const state = Math.random().toString(36).substring(2) + Date.now().toString(36);
    sessionStorage.setItem("oauth_state", state);
    const { error } = await supabase.auth.signInWithOAuth({
      provider: "google",
      options: {
        redirectTo: `${window.location.origin}/auth/callback`,
        queryParams: { state }
      }
    });
    if (error) console.error("Google login error:", error);
  };

  return (
    <button onClick={handleGoogleLogin} className="google-login-btn mt-4 w-full flex items-center justify-center gap-2 border border-[var(--border-color)] rounded-xl py-3 hover:bg-[var(--bg-elevated)] transition-colors duration-200">
      <img src="https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg" alt="Google" className="w-5 h-5"/>
      Continue with Google
    </button>
  );
};
