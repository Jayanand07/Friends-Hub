export default function SkeletonPost() {
    return (
        <div className="bg-[var(--bg-card)] border-b border-[var(--border-color)] sm:border sm:rounded-lg sm:mb-3">
            {/* Header */}
            <div className="flex items-center gap-3 px-4 py-3">
                <div className="w-8 h-8 rounded-full skeleton" />
                <div className="flex-1">
                    <div className="h-3 w-24 skeleton mb-1.5" />
                    <div className="h-2 w-14 skeleton" />
                </div>
            </div>
            {/* Image */}
            <div className="w-full h-[300px] skeleton" style={{ borderRadius: 0 }} />
            {/* Actions */}
            <div className="px-4 py-2.5 flex gap-3">
                <div className="h-6 w-6 skeleton rounded-full" />
                <div className="h-6 w-6 skeleton rounded-full" />
                <div className="h-6 w-6 skeleton rounded-full" />
            </div>
            {/* Text */}
            <div className="px-4 pb-3 space-y-1.5">
                <div className="h-3 w-16 skeleton" />
                <div className="h-3 w-3/4 skeleton" />
            </div>
        </div>
    );
}
