import { useRef, useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ZoomIn, ZoomOut, Maximize2, Loader, Users } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getNetworkGraph } from '../api/users';
import { isSafeUrl } from '../utils/sanitize';

const NODE_RADIUS = 24;
const CENTER_RADIUS = 36;

function buildLayout(center, friends, mutuals) {
    const nodes = [];
    const edges = [];
    const nodeMap = {};

    // Central node
    const centerNode = { id: center.id, label: center.firstName || 'You', pic: center.profilePicUrl, type: 'center', x: 0, y: 0 };
    nodes.push(centerNode);
    nodeMap[center.id] = centerNode;

    // Friend nodes in a circle
    const friendRadius = 180;
    friends.forEach((f, i) => {
        const angle = (2 * Math.PI * i) / friends.length - Math.PI / 2;
        const node = {
            id: f.id,
            label: f.firstName || 'Friend',
            pic: f.profilePicUrl,
            type: 'friend',
            x: Math.cos(angle) * friendRadius,
            y: Math.sin(angle) * friendRadius,
        };
        nodes.push(node);
        nodeMap[f.id] = node;
        edges.push({ from: center.id, to: f.id, type: 'friend' });
    });

    // Mutual nodes in outer ring
    const mutualRadius = 320;
    const placed = new Set(friends.map(f => f.id));
    placed.add(center.id);
    let mutualIdx = 0;
    mutuals.forEach((m) => {
        if (placed.has(m.id)) return;
        const angle = (2 * Math.PI * mutualIdx) / mutuals.length;
        const node = {
            id: m.id,
            label: m.firstName || 'Mutual',
            pic: m.profilePicUrl,
            type: 'mutual',
            x: Math.cos(angle) * mutualRadius,
            y: Math.sin(angle) * mutualRadius,
        };
        nodes.push(node);
        nodeMap[m.id] = node;
        // Connect to shared friends
        if (m.mutualFriendIds) {
            m.mutualFriendIds.forEach(fid => {
                if (nodeMap[fid]) edges.push({ from: m.id, to: fid, type: 'mutual' });
            });
        }
        mutualIdx++;
        placed.add(m.id);
    });

    return { nodes, edges };
}

function Avatar({ node, r, onClick, isHovered }) {
    const label = node.label || '?';
    const initial = label.charAt(0).toUpperCase();
    const accentColor = '#E8824A';

    const ringScale = isHovered ? 1.15 : 1;

    return (
        <g
            transform={`translate(${node.x}, ${node.y})`}
            onClick={() => onClick(node)}
            style={{ cursor: 'pointer' }}
        >
            <circle
                r={r + 3}
                fill="none"
                stroke={node.type === 'center' ? accentColor : node.type === 'mutual' ? '#a78bfa' : accentColor}
                strokeWidth={node.type === 'center' ? 3 : 1.5}
                opacity={isHovered ? 1 : 0.6}
                style={{ transform: `scale(${ringScale})`, transition: 'transform 0.2s, opacity 0.2s' }}
            />
            <circle r={r} fill="var(--bg-elevated)" />
            {node.pic && isSafeUrl(node.pic) ? (
                <image
                    href={node.pic}
                    x={-r} y={-r}
                    width={r * 2} height={r * 2}
                    clipPath={`circle(${r}px at ${r}px ${r}px)`}
                    style={{ borderRadius: '50%' }}
                />
            ) : (
                <text
                    textAnchor="middle"
                    dominantBaseline="central"
                    fontSize={r * 0.75}
                    fontWeight="700"
                    fill="white"
                    style={{ userSelect: 'none' }}
                >
                    {initial}
                </text>
            )}
            <text
                y={r + 13}
                textAnchor="middle"
                fontSize="9"
                fill="var(--text-secondary)"
                style={{ userSelect: 'none' }}
            >
                {label.length > 10 ? label.slice(0, 9) + '…' : label}
            </text>
        </g>
    );
}

export default function NetworkGraph({ profile }) {
    const navigate = useNavigate();
    const svgRef = useRef(null);
    const [graph, setGraph] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [transform, setTransform] = useState({ x: 0, y: 0, scale: 1 });
    const [dragging, setDragging] = useState(false);
    const dragStart = useRef(null);
    const [hovered, setHovered] = useState(null);
    const [tooltip, setTooltip] = useState(null);

    useEffect(() => {
        if (!profile?.userId) return;
        setLoading(true);
        setError(null);
        getNetworkGraph(profile.userId)
            .then(res => {
                const data = res.data;
                setGraph(buildLayout(
                    data.center || profile,
                    data.friends || [],
                    data.mutuals || []
                ));
            })
            .catch(() => {
                // Fallback: show center node only
                setGraph(buildLayout(profile, [], []));
                setError('Could not load full network. Showing available data.');
            })
            .finally(() => setLoading(false));
    }, [profile?.userId]);

    const handleWheel = useCallback((e) => {
        e.preventDefault();
        setTransform(t => {
            const factor = e.deltaY < 0 ? 1.1 : 0.9;
            const newScale = Math.min(3, Math.max(0.3, t.scale * factor));
            return { ...t, scale: newScale };
        });
    }, []);

    useEffect(() => {
        const el = svgRef.current;
        if (!el) return;
        el.addEventListener('wheel', handleWheel, { passive: false });
        return () => el.removeEventListener('wheel', handleWheel);
    }, [handleWheel]);

    const onMouseDown = (e) => {
        if (e.button !== 0) return;
        setDragging(true);
        dragStart.current = { x: e.clientX - transform.x, y: e.clientY - transform.y };
    };

    const onMouseMove = (e) => {
        if (!dragging || !dragStart.current) return;
        setTransform(t => ({ ...t, x: e.clientX - dragStart.current.x, y: e.clientY - dragStart.current.y }));
    };

    const onMouseUp = () => { setDragging(false); dragStart.current = null; };

    const resetView = () => setTransform({ x: 0, y: 0, scale: 1 });
    const zoom = (dir) => setTransform(t => ({ ...t, scale: Math.min(3, Math.max(0.3, t.scale * (dir > 0 ? 1.2 : 0.8))) }));

    const handleNodeClick = (node) => {
        if (node.type !== 'center') navigate(`/profile/${node.id}`);
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <Loader size={24} className="text-[var(--accent)] animate-spin" />
            </div>
        );
    }

    const { nodes, edges } = graph;
    const friendCount = nodes.filter(n => n.type === 'friend').length;
    const mutualCount = nodes.filter(n => n.type === 'mutual').length;

    return (
        <div className="relative w-full rounded-xl border border-[var(--border-color)] bg-[var(--bg-card)] overflow-hidden" style={{ height: 480 }}>
            {/* Controls */}
            <div className="absolute top-3 right-3 z-10 flex flex-col gap-1.5">
                {[{ icon: ZoomIn, fn: () => zoom(1) }, { icon: ZoomOut, fn: () => zoom(-1) }, { icon: Maximize2, fn: resetView }].map(({ icon: Icon, fn }, i) => (
                    <button key={i} onClick={fn} className="btn-icon w-8 h-8 bg-[var(--bg-elevated)] border border-[var(--border-color)] shadow-sm">
                        <Icon size={14} />
                    </button>
                ))}
            </div>

            {/* Legend */}
            <div className="absolute top-3 left-3 z-10 flex flex-col gap-1 bg-[var(--bg-elevated)]/80 backdrop-blur-sm rounded-lg px-3 py-2 border border-[var(--border-color)]">
                {[
                    { color: '#E8824A', label: `You` },
                    { color: '#E8824A', label: `${friendCount} Friends`, opacity: '60%' },
                    { color: '#a78bfa', label: `${mutualCount} Mutuals` },
                ].map(({ color, label, opacity }, i) => (
                    <div key={i} className="flex items-center gap-1.5">
                        <div className="w-2 h-2 rounded-full" style={{ background: color, opacity: opacity || '100%' }} />
                        <span className="text-[10px] text-[var(--text-secondary)]">{label}</span>
                    </div>
                ))}
            </div>

            {error && (
                <div className="absolute bottom-3 left-1/2 -translate-x-1/2 z-10 text-[10px] text-[var(--text-muted)] bg-[var(--bg-elevated)] px-3 py-1.5 rounded-full border border-[var(--border-color)]">
                    {error}
                </div>
            )}

            {nodes.length <= 1 && !loading && (
                <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 pointer-events-none">
                    <Users size={32} className="text-[var(--text-muted)] opacity-30" />
                    <p className="text-[13px] text-[var(--text-muted)]">No connections to visualize yet</p>
                </div>
            )}

            <svg
                ref={svgRef}
                width="100%"
                height="100%"
                style={{ cursor: dragging ? 'grabbing' : 'grab', display: 'block' }}
                onMouseDown={onMouseDown}
                onMouseMove={onMouseMove}
                onMouseUp={onMouseUp}
                onMouseLeave={onMouseUp}
            >
                <defs>
                    <radialGradient id="bgGrad" cx="50%" cy="50%" r="50%">
                        <stop offset="0%" stopColor="var(--bg-card)" />
                        <stop offset="100%" stopColor="var(--bg-primary)" />
                    </radialGradient>
                    {nodes.map(n => n.pic && isSafeUrl(n.pic) && (
                        <clipPath key={`clip-${n.id}`} id={`clip-${n.id}`}>
                            <circle r={n.type === 'center' ? CENTER_RADIUS : NODE_RADIUS} />
                        </clipPath>
                    ))}
                </defs>

                <rect width="100%" height="100%" fill="url(#bgGrad)" />

                <g transform={`translate(${transform.x + (svgRef.current?.clientWidth || 600) / 2}, ${transform.y + (svgRef.current?.clientHeight || 480) / 2}) scale(${transform.scale})`}>
                    {/* Edges */}
                    {edges.map((e, i) => {
                        const from = nodes.find(n => n.id === e.from);
                        const to = nodes.find(n => n.id === e.to);
                        if (!from || !to) return null;
                        return (
                            <line
                                key={i}
                                x1={from.x} y1={from.y}
                                x2={to.x} y2={to.y}
                                stroke={e.type === 'mutual' ? '#a78bfa' : '#E8824A'}
                                strokeWidth={e.type === 'mutual' ? 0.8 : 1.2}
                                opacity={e.type === 'mutual' ? 0.25 : 0.35}
                            />
                        );
                    })}

                    {/* Nodes */}
                    {nodes.map(n => (
                        <Avatar
                            key={n.id}
                            node={n}
                            r={n.type === 'center' ? CENTER_RADIUS : NODE_RADIUS}
                            onClick={handleNodeClick}
                            isHovered={hovered === n.id}
                        />
                    ))}
                </g>
            </svg>
        </div>
    );
}
