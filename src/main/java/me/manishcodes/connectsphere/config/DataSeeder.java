package me.manishcodes.connectsphere.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs ONCE on startup (only in "local" profile).
 * Seeds 500 test users + follow relationships if the users table has fewer than 100 rows.
 */
@Slf4j
@Component
@Profile("local")   // ← only runs when spring.profiles.active=local
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    private static final String BCRYPT_TEST_AT_1234 =
            "$2a$10$SlMEAl9mZqNLGbR1z7tFH.OzJT16L7NObEqkJAqVo5FEdtqj4xvGi";

    @Override
    public void run(String... args) {

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (count != null && count >= 100) {
            log.info("DataSeeder — {} users already exist, skipping seed.", count);
            return;
        }

        log.info("DataSeeder — seeding 500 test users...");
        seedUsers();
        log.info("DataSeeder — adding follow relationships...");
        seedFollows();
        log.info("DataSeeder — done! {} users now in DB.",
                jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
    }

    // ── Insert 500 users ────────────────────────────────────────────────────────
    private void seedUsers() {

        String[] passions = {
            "TECHNOLOGY","PROGRAMMING","ARTIFICIAL_INTELLIGENCE","PHOTOGRAPHY","MUSIC",
            "GAMING","FITNESS","TRAVEL","COOKING","READING","WRITING","ART_AND_DESIGN",
            "ENTREPRENEURSHIP","FINANCE_AND_INVESTING","FASHION","FILM_AND_CINEMA",
            "SPORTS","SCIENCE","MENTAL_HEALTH","ENVIRONMENT","EDUCATION","POLITICS",
            "ASTRONOMY","SPIRITUALITY","VOLUNTEERING","ANIME_AND_MANGA",
            "CARS_AND_AUTOMOBILES","PETS_AND_ANIMALS","FOOD_AND_CULTURE","CONTENT_CREATION"
        };

        String[] bios = {
            "Love coding and building cool things 🚀",
            "Photography enthusiast capturing moments 📸",
            "Fitness freak hitting PRs every week 💪",
            "Travel addict exploring the world ✈️",
            "Music lover, guitar player 🎵",
            "Gaming nerd, pro level 🎮",
            "Food explorer on a mission 🍜",
            "Bookworm and lifelong learner 📚",
            "Entrepreneur chasing dreams 💡",
            "Tech enthusiast building the future ⚡",
            "Coffee ☕ + Code = Life",
            "Anime fan and proud of it 🎌",
            "Car lover, speed addict 🏎️",
            "Nature lover, environmentalist 🌿",
            "Just here to vibe and create 🎨"
        };

        // First 250 use "manish" prefix, next 250 use mixed prefixes
        String[] prefixes = {
            "manish","manish","manish","manish","manish",
            "dev","tech","coder","pro","star"
        };

        String sql = """
                INSERT INTO users (
                    id, auth_provider, bio, created_at, date_of_birth,
                    email, is_active, is_verified, passion, password,
                    provider_id, role, updated_at, username, profile_url, username_changed_at
                ) VALUES (
                    gen_random_uuid(), 'LOCAL', ?, NOW() - (random() * INTERVAL '365 days'),
                    CURRENT_DATE - (floor(random() * 10000))::INT * INTERVAL '1 day',
                    ?, TRUE, TRUE, ?, ?, NULL, 'USER', NOW(), ?, ?, NULL
                )
                ON CONFLICT (username) DO NOTHING
                """;

        for (int i = 1; i <= 500; i++) {
            String prefix  = prefixes[(i - 1) % 10];
            String username = prefix + i;
            String email    = username + "@connectsphere.dev";
            String bio      = bios[(i - 1) % bios.length];
            String passion  = passions[(i - 1) % passions.length];
            String avatar   = "https://api.dicebear.com/9.x/avataaars/svg?seed=" + username;

            try {
                jdbc.update(sql, bio, email, passion, BCRYPT_TEST_AT_1234, username, avatar);
            } catch (Exception e) {
                log.warn("Skipping user {} — {}", username, e.getMessage());
            }
        }
    }

    // ── Add follow relationships ─────────────────────────────────────────────────
    private void seedFollows() {

        // Top 4 manish users get heavy followers (so search sort is visible)
        var topManish = jdbc.queryForList(
                "SELECT id FROM users WHERE username LIKE 'manish%' ORDER BY created_at LIMIT 4",
                java.util.UUID.class
        );

        var allUserIds = jdbc.queryForList("SELECT id FROM users", java.util.UUID.class);
        int total = allUserIds.size();

        String followSql = """
                INSERT INTO follows (id, follower_id, following_id, created_at)
                VALUES (gen_random_uuid(), ?, ?, NOW())
                ON CONFLICT DO NOTHING
                """;

        int[] followerCounts = {200, 150, 100, 50};

        for (int i = 0; i < topManish.size(); i++) {
            java.util.UUID target = topManish.get(i);
            int count = followerCounts[i];

            for (int j = 0; j < count; j++) {
                java.util.UUID follower = allUserIds.get(j % total);
                if (follower.equals(target)) continue; // no self-follow
                try {
                    jdbc.update(followSql, follower, target);
                } catch (Exception ignored) {}
            }
            log.info("  → {} followers added to {}", count, target);
        }

        // Random 1–20 followers for everyone else
        for (int i = 4; i < total; i++) {
            java.util.UUID target = allUserIds.get(i);
            int count = (int) (Math.random() * 20) + 1;

            for (int j = 0; j < count; j++) {
                java.util.UUID follower = allUserIds.get((i + j + 1) % total);
                if (follower.equals(target)) continue;
                try {
                    jdbc.update(followSql, follower, target);
                } catch (Exception ignored) {}
            }
        }
    }
}
