export default function SkeletonLoader({ type = 'text', className = '' }) {
    const baseClass = "skeleton bg-[var(--bg-elevated)] rounded-md";

    if (type === 'avatar') {
        return <div className={`${baseClass} rounded-full w-10 h-10 ${className}`} />;
    }

    if (type === 'card') {
        return (
            <div className={`p-4 border border-[var(--border-color)] rounded-xl bg-[var(--bg-card)] ${className}`}>
                <div className="flex gap-3 mb-4">
                    <div className={`${baseClass} rounded-full w-10 h-10 flex-shrink-0`} />
                    <div className="flex-1 space-y-2 py-1">
                        <div className={`${baseClass} h-3 w-1/3`} />
                        <div className={`${baseClass} h-2 w-1/4`} />
                    </div>
                </div>
                <div className={`${baseClass} h-32 w-full rounded-lg mb-4`} />
                <div className="space-y-2">
                    <div className={`${baseClass} h-3 w-full`} />
                    <div className={`${baseClass} h-3 w-5/6`} />
                </div>
            </div>
        );
    }

    return <div className={`${baseClass} h-4 w-full ${className}`} />;
}
