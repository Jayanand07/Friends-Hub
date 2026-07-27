import { ArrowUp, ArrowDown } from "lucide-react";

const ScrollToTop = () => {
  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const scrollToBottom = () => {
    const scrollToCurrentBottom = () => {
      window.scrollTo({
        top: document.documentElement.scrollHeight,
        behavior: "smooth",
      });
    };

    scrollToCurrentBottom();
    // Re-check after lazy-loaded content may have rendered in
    setTimeout(scrollToCurrentBottom, 500);
  };

  return (
    <div className="fixed bottom-24 right-6 z-50 flex flex-col gap-3">
      <button
        onClick={scrollToTop}
        aria-label="Scroll to top"
        className="p-3 rounded-full bg-[var(--accent)] text-white shadow-lg hover:opacity-90 transition-all duration-300"
      >
        <ArrowUp size={20} />
      </button>
      <button
        onClick={scrollToBottom}
        aria-label="Scroll to bottom"
        className="p-3 rounded-full bg-[var(--accent)] text-white shadow-lg hover:opacity-90 transition-all duration-300"
      >
        <ArrowDown size={20} />
      </button>
    </div>
  );
};

export default ScrollToTop;