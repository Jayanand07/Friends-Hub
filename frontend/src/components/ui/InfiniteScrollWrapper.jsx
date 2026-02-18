import { useEffect, useRef, useState } from 'react';

export default function InfiniteScrollWrapper({ children, onLoadMore, hasMore, loading }) {
    const observerTarget = useRef(null);
    const onLoadMoreRef = useRef(onLoadMore);

    useEffect(() => {
        onLoadMoreRef.current = onLoadMore;
    }, [onLoadMore]);

    useEffect(() => {
        const observer = new IntersectionObserver(
            entries => {
                if (entries[0].isIntersecting && hasMore && !loading) {
                    onLoadMoreRef.current();
                }
            },
            { threshold: 0.1 }
        );

        if (observerTarget.current) {
            observer.observe(observerTarget.current);
        }

        return () => {
            if (observerTarget.current) {
                observer.unobserve(observerTarget.current);
            }
        };
    }, [hasMore, loading]);

    return (
        <>
            {children}
            {hasMore && (
                <div ref={observerTarget} className="h-10 flex items-center justify-center p-4">
                    {loading && <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-[var(--accent)]"></div>}
                </div>
            )}
        </>
    );
}
