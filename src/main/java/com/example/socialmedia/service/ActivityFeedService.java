package com.example.socialmedia.service;

import com.example.socialmedia.dto.ActivityFeedItem;
import com.example.socialmedia.entity.Follow;
import com.example.socialmedia.entity.Post;
import com.example.socialmedia.entity.User;
import com.example.socialmedia.entity.UserInfo;
import com.example.socialmedia.repository.BlockRepository;
import com.example.socialmedia.repository.FollowRepository;
import com.example.socialmedia.repository.PostRepository;
import com.example.socialmedia.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityFeedService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int LOOKBACK_DAYS = 30;

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final BlockRepository blockRepository;

    public ActivityFeedService(UserRepository userRepository,
                               FollowRepository followRepository,
                               PostRepository postRepository,
                               BlockRepository blockRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.postRepository = postRepository;
        this.blockRepository = blockRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivityFeedItem> getFeed(String email, int page, int size) {
        User me = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Follow> myFollows = followRepository.findByFollowerId(me.getId());
        if (myFollows.isEmpty()) return List.of();

        List<Long> followingIds = myFollows.stream()
                .map(f -> f.getFollowing().getId())
                .collect(Collectors.toList());

        LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);

        // ── Post events ──────────────────────────────────────
        List<Post> posts = postRepository.findRecentByUserIds(
                followingIds, since, PageRequest.of(0, 200));

        List<ActivityFeedItem> items = new ArrayList<>();

        for (Post p : posts) {
            // Post author is already guaranteed to be someone the viewer follows
            // (queried via followingIds). The privateAccount check is intentionally
            // omitted here because the viewer already follows this user, so they
            // are authorized to see their posts.
            items.add(ActivityFeedItem.forPost(
                    p.getUser().getId(),
                    displayName(p.getUser()),
                    picUrl(p.getUser()),
                    p.getId(),
                    p.getContent(),
                    p.getImageUrl(),
                    p.getLikes() != null ? p.getLikes().size() : 0,
                    p.getComments() != null ? p.getComments().size() : 0,
                    p.getCreatedAt() != null ? p.getCreatedAt().format(ISO) : null));
        }

        // ── New-friend events (followings who followed someone new) ──
        // M-5: Batch fetch all friend-of-friend follows in a single query
        List<Long> friendIds = myFollows.stream()
                .map(f -> f.getFollowing().getId())
                .collect(Collectors.toList());

        // H-8: Filter out blocked relationships before fetching
        List<Long> allowedFriendIds = friendIds.stream()
                .filter(fid -> !blockRepository.existsByBlockerIdAndBlockedId(me.getId(), fid)
                        && !blockRepository.existsByBlockerIdAndBlockedId(fid, me.getId()))
                .collect(Collectors.toList());

        if (!allowedFriendIds.isEmpty()) {
            List<Follow> allFriendFollows = followRepository.findByFollowerIdIn(allowedFriendIds);
            for (Follow ff : allFriendFollows) {
                if (ff.getCreatedAt() != null && ff.getCreatedAt().isAfter(since)
                        && !ff.getFollowing().getId().equals(me.getId())) {
                    items.add(ActivityFeedItem.forNewFriend(
                            ff.getFollower().getId(),
                            displayName(ff.getFollower()),
                            picUrl(ff.getFollower()),
                            ff.getFollowing().getId(),
                            displayName(ff.getFollowing()),
                            picUrl(ff.getFollowing()),
                            ff.getCreatedAt().format(ISO)));
                }
            }
        }

        // Sort descending by timestamp, paginate
        items.sort(Comparator.comparing(
                i -> i.getTimestamp() != null ? i.getTimestamp() : "",
                Comparator.reverseOrder()));

        int from = page * size;
        if (from >= items.size()) return List.of();
        return items.subList(from, Math.min(from + size, items.size()));
    }

    private String displayName(User u) {
        UserInfo info = u.getUserInfo();
        if (info != null) {
            String name = ((info.getFirstName() != null ? info.getFirstName() : "") + " "
                    + (info.getLastName() != null ? info.getLastName() : "")).trim();
            if (!name.isBlank()) return name;
        }
        return "User#" + u.getId();
    }

    private String picUrl(User u) {
        return u.getUserInfo() != null ? u.getUserInfo().getProfilePicUrl() : null;
    }
}
