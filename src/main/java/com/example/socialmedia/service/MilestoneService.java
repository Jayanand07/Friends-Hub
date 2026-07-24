package com.example.socialmedia.service;

import com.example.socialmedia.dto.MilestoneResponse;
import com.example.socialmedia.dto.MilestoneResponse.Badge;
import com.example.socialmedia.dto.MilestoneResponse.TimelineEvent;
import com.example.socialmedia.entity.Follow;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.repository.BlockRepository;
import com.example.socialmedia.repository.FollowRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class MilestoneService {

    // Each entry: [days threshold, id, label, emoji, description]
    private static final Object[][] BADGE_DEFS = {
        {  1L,   "first_day",   "Day 1",         "🌱", "Connected for the first time"},
        {  7L,   "one_week",    "1 Week",         "🤝", "Friends for a whole week"},
        { 30L,   "one_month",   "1 Month",        "📅", "One month of friendship"},
        { 90L,   "three_months","3 Months",       "🌟", "Three months together"},
        {180L,   "six_months",  "6 Months",       "🎉", "Half a year as friends"},
        {365L,   "one_year",    "1 Year",         "🏆", "Friends for a whole year!"},
        {730L,   "two_years",   "2 Years",        "💎", "Two years of friendship"},
        {1095L,  "three_years", "3 Years",        "👑", "Three incredible years"},
    };

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;

    public MilestoneService(UserRepository userRepository, FollowRepository followRepository,
            BlockRepository blockRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.blockRepository = blockRepository;
    }

    @Transactional(readOnly = true)
    public MilestoneResponse getMilestones(Long friendId, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // SECURITY: Block check — prevent milestone viewing with blocked users
        if (blockRepository.existsByBlockerAndBlocked(friend, requester) ||
                blockRepository.existsByBlockerAndBlocked(requester, friend)) {
            throw new RuntimeException("Action not allowed");
        }

        // Must be connected in at least one direction
        Follow aToB = followRepository.findByFollowerAndFollowing(requester, friend).orElse(null);
        Follow bToA = followRepository.findByFollowerAndFollowing(friend, requester).orElse(null);

        if (aToB == null && bToA == null) {
            throw new RuntimeException("No connection found");
        }

        boolean isMutual = aToB != null && bToA != null;

        // Earliest follow determines friendship start date
        LocalDateTime start = earliest(aToB, bToA);
        long daysConnected = ChronoUnit.DAYS.between(start.toLocalDate(), LocalDate.now());
        String friendsSince = start.toLocalDate().format(ISO_DATE);

        // Build badges
        List<Badge> badges = new ArrayList<>();
        String nextMilestone = null;
        long daysToNext = 0;

        for (Object[] def : BADGE_DEFS) {
            long threshold = (Long) def[0];
            String id      = (String) def[1];
            String label   = (String) def[2];
            String emoji   = (String) def[3];
            String desc    = (String) def[4];

            boolean unlocked = daysConnected >= threshold;
            String unlockedAt = null;
            if (unlocked) {
                LocalDate unlockedDate = start.toLocalDate().plusDays(threshold);
                unlockedAt = unlockedDate.format(ISO_DATE);
            } else if (nextMilestone == null) {
                nextMilestone = label;
                daysToNext = threshold - daysConnected;
            }
            badges.add(new Badge(id, label, emoji, desc, unlocked, unlockedAt));
        }

        // Build timeline
        List<TimelineEvent> timeline = buildTimeline(start, daysConnected, isMutual, aToB, bToA);

        return new MilestoneResponse(daysConnected, friendsSince, isMutual,
                badges, timeline, nextMilestone, daysToNext);
    }

    private LocalDateTime earliest(Follow a, Follow b) {
        LocalDateTime ta = (a != null && a.getCreatedAt() != null) ? a.getCreatedAt() : LocalDateTime.now();
        LocalDateTime tb = (b != null && b.getCreatedAt() != null) ? b.getCreatedAt() : LocalDateTime.now();
        return ta.isBefore(tb) ? ta : tb;
    }

    private List<TimelineEvent> buildTimeline(LocalDateTime start, long daysConnected,
                                              boolean isMutual, Follow aToB, Follow bToA) {
        List<TimelineEvent> events = new ArrayList<>();

        events.add(new TimelineEvent(
                start.toLocalDate().format(ISO_DATE),
                "Connection started", "🌱"));

        if (isMutual && aToB != null && bToA != null
                && aToB.getCreatedAt() != null && bToA.getCreatedAt() != null
                && !aToB.getCreatedAt().toLocalDate().equals(bToA.getCreatedAt().toLocalDate())) {
            LocalDateTime mutualDate = aToB.getCreatedAt().isAfter(bToA.getCreatedAt())
                    ? aToB.getCreatedAt() : bToA.getCreatedAt();
            events.add(new TimelineEvent(
                    mutualDate.toLocalDate().format(ISO_DATE),
                    "Became mutual friends", "🤝"));
        }

        // Add a timeline entry for each unlocked milestone date
        for (Object[] def : BADGE_DEFS) {
            long threshold = (Long) def[0];
            if (daysConnected >= threshold) {
                LocalDate milestoneDate = start.toLocalDate().plusDays(threshold);
                events.add(new TimelineEvent(
                        milestoneDate.format(ISO_DATE),
                        (String) def[2] + " milestone reached",
                        (String) def[3]));
            }
        }

        // Sort chronologically
        events.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return events;
    }
}
