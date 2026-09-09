INSERT INTO users (id, name, avatar_url) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Alice Johnson', 'https://example.com/avatars/alice.jpg'),
    ('10000000-0000-0000-0000-000000000002', 'Bruno Silva', 'https://example.com/avatars/bruno.jpg');

INSERT INTO books (id, title, author, cover_url) VALUES
    ('20000000-0000-0000-0000-000000000001', 'The Pragmatic Programmer', 'Andrew Hunt and David Thomas', 'https://example.com/covers/pragmatic-programmer.jpg'),
    ('20000000-0000-0000-0000-000000000002', 'Clean Code', 'Robert C. Martin', 'https://example.com/covers/clean-code.jpg');

INSERT INTO feed_activities (id, user_id, book_id, status, created_at) VALUES
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'READING', TIMESTAMP '2026-01-15 10:30:00'),
    ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'READ', TIMESTAMP '2026-02-20 14:45:00');

INSERT INTO reactions (id, activity_id, user_id, type) VALUES
    ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', 'LIKE'),
    ('40000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'DISLIKE');