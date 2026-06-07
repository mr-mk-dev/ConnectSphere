-- ============================================================
--  ConnectSphere — Seed 500 Test Users + Follow Relationships
--  Password for ALL users: Test@1234
--  BCrypt hash below is pre-computed for "Test@1234"
-- ============================================================

DO $$
DECLARE
    passions TEXT[] := ARRAY[
        'TECHNOLOGY','PROGRAMMING','ARTIFICIAL_INTELLIGENCE','PHOTOGRAPHY','MUSIC',
        'GAMING','FITNESS','TRAVEL','COOKING','READING','WRITING','ART_AND_DESIGN',
        'ENTREPRENEURSHIP','FINANCE_AND_INVESTING','FASHION','FILM_AND_CINEMA',
        'SPORTS','SCIENCE','MENTAL_HEALTH','ENVIRONMENT','EDUCATION','POLITICS',
        'ASTRONOMY','SPIRITUALITY','VOLUNTEERING','ANIME_AND_MANGA',
        'CARS_AND_AUTOMOBILES','PETS_AND_ANIMALS','FOOD_AND_CULTURE','CONTENT_CREATION'
    ];

    bios TEXT[] := ARRAY[
        'Love coding and building cool things 🚀',
        'Photography enthusiast capturing moments 📸',
        'Fitness freak hitting PRs every week 💪',
        'Travel addict exploring the world ✈️',
        'Music lover, guitar player 🎵',
        'Gaming nerd, pro level 🎮',
        'Food explorer on a mission 🍜',
        'Bookworm and lifelong learner 📚',
        'Entrepreneur chasing dreams 💡',
        'Tech enthusiast building the future ⚡',
        'Coffee ☕ + Code = Life',
        'Anime fan and proud of it 🎌',
        'Car lover, speed addict 🏎️',
        'Nature lover, environmentalist 🌿',
        'Just here to vibe and create 🎨'
    ];

    -- Username prefixes: first 100 slots use "manish" to make search testing rich
    prefixes TEXT[] := ARRAY[
        'manish','manish','manish','manish','manish',   -- 50 manish users (idx 1-5, cycling)
        'dev','tech','coder','pro','star'               -- 50 other users  (idx 6-10, cycling)
    ];

    i         INT;
    uname     TEXT;
    email     TEXT;
    uid       UUID;
    bcrypt    TEXT := '$2a$10$SlMEAl9mZqNLGbR1z7tFH.OzJT16L7NObEqkJAqVo5FEdtqj4xvGi';
    -- ^ BCrypt of "Test@1234" — all seed users share this password

BEGIN
    FOR i IN 1..500 LOOP
        uid   := gen_random_uuid();
        uname := prefixes[((i-1) % 10) + 1] || i::TEXT;
        email := uname || '@connectsphere.dev';

        INSERT INTO users (
            id, auth_provider, bio, created_at, date_of_birth,
            email, is_active, is_verified, passion, password,
            provider_id, role, updated_at, username,
            profile_url, username_changed_at
        ) VALUES (
            uid,
            'LOCAL',
            bios[((i-1) % 15) + 1],
            NOW() - (random() * INTERVAL '365 days'),
            (DATE '1990-01-01' + (floor(random() * 10000))::INT),
            email,
            TRUE,
            TRUE,
            passions[((i-1) % 30) + 1],
            bcrypt,
            NULL,
            'USER',
            NOW(),
            uname,
            'https://api.dicebear.com/9.x/avataaars/svg?seed=' || uname,
            NULL
        )
        ON CONFLICT (username) DO NOTHING
        ON CONFLICT (email)    DO NOTHING;

    END LOOP;
END $$;


-- ============================================================
--  Add Follow Relationships so "sort by followers" is testable
--  Strategy:
--    • manish1  → 200 followers  (should appear first in search)
--    • manish2  → 150 followers
--    • manish3  → 100 followers
--    • manish4  →  50 followers
--    • rest     →   random 0-20 followers
-- ============================================================

DO $$
DECLARE
    star_users   UUID[];
    all_users    UUID[];
    follower_uid UUID;
    target_uid   UUID;
    i            INT;
    j            INT;
    follow_count INT;
BEGIN
    -- Fetch the top-4 manish users by created order
    SELECT ARRAY(
        SELECT id FROM users
        WHERE username LIKE 'manish%'
        ORDER BY created_at
        LIMIT 4
    ) INTO star_users;

    -- Fetch all user IDs for use as followers
    SELECT ARRAY(SELECT id FROM users) INTO all_users;

    -- Give star users lots of followers
    -- follow_count per star: 200, 150, 100, 50
    FOR i IN 1..4 LOOP
        target_uid   := star_users[i];
        follow_count := CASE i WHEN 1 THEN 200 WHEN 2 THEN 150 WHEN 3 THEN 100 ELSE 50 END;

        FOR j IN 1..follow_count LOOP
            follower_uid := all_users[((j - 1) % array_length(all_users, 1)) + 1];
            -- Skip self-follow
            CONTINUE WHEN follower_uid = target_uid;

            INSERT INTO follows (id, follower_id, following_id, created_at)
            VALUES (gen_random_uuid(), follower_uid, target_uid, NOW())
            ON CONFLICT DO NOTHING;
        END LOOP;
    END LOOP;

    -- Give remaining users a random 0–20 followers each
    FOR i IN 5..array_length(all_users, 1) LOOP
        target_uid   := all_users[i];
        follow_count := floor(random() * 20)::INT;

        FOR j IN 1..follow_count LOOP
            follower_uid := all_users[((j + i) % array_length(all_users, 1)) + 1];
            CONTINUE WHEN follower_uid = target_uid;

            INSERT INTO follows (id, follower_id, following_id, created_at)
            VALUES (gen_random_uuid(), follower_uid, target_uid, NOW())
            ON CONFLICT DO NOTHING;
        END LOOP;
    END LOOP;

END $$;
