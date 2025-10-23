-- Sample data script để tạo dữ liệu test cho Korean Learning App
-- Chạy script này trong pgAdmin để tạo sample data

-- 1. Tạo Question Types
INSERT INTO question_types (name, description) VALUES 
('multiple_choice', 'Multiple choice questions'),
('true_false', 'True or false questions'),
('fill_blank', 'Fill in the blank questions')
ON CONFLICT DO NOTHING;

-- 2. Tạo Course
INSERT INTO courses (title, description, order_index) VALUES 
('Korean Beginner', 'Basic Korean language course for beginners', 1)
ON CONFLICT DO NOTHING;

-- 3. Tạo Unit
INSERT INTO units (course_id, title, description, order_index) VALUES 
(1, 'Basic Greetings', 'Learn basic Korean greetings', 1)
ON CONFLICT DO NOTHING;

-- 4. Tạo Lesson
INSERT INTO lessons (unit_id, title, description, order_index) VALUES 
(1, 'Hello & Goodbye', 'Learn how to say hello and goodbye in Korean', 1)
ON CONFLICT DO NOTHING;

-- 5. Tạo Quiz
INSERT INTO quizzes (lesson_id, title, description, type, order_index) VALUES 
(1, 'Vocabulary Quiz', 'Test your knowledge of basic Korean vocabulary', 'vocabulary', 1)
ON CONFLICT DO NOTHING;

-- 6. Tạo Questions
INSERT INTO questions (quiz_id, question_type_id, content, correct_answer, explanation, order_index) VALUES 
(1, 1, 'What does 안녕하세요 mean?', 'Hello', '안녕하세요 is a common Korean greeting meaning "Hello"', 1),
(1, 1, 'How do you say "Goodbye" in Korean?', '안녕히 가세요', '안녕히 가세요 means "Goodbye" when the other person is leaving', 2),
(1, 1, 'What does 감사합니다 mean?', 'Thank you', '감사합니다 is a polite way to say "Thank you" in Korean', 3)
ON CONFLICT DO NOTHING;

-- 7. Tạo Question Options
INSERT INTO question_options (question_id, option_text, is_correct) VALUES 
-- Question 1 options
(1, 'Hello', true),
(1, 'Goodbye', false),
(1, 'Thank you', false),
(1, 'Sorry', false),
-- Question 2 options
(2, '안녕하세요', false),
(2, '안녕히 가세요', true),
(2, '감사합니다', false),
(2, '죄송합니다', false),
-- Question 3 options
(3, 'Hello', false),
(3, 'Goodbye', false),
(3, 'Thank you', true),
(3, 'Sorry', false)
ON CONFLICT DO NOTHING;

-- 8. Kiểm tra dữ liệu đã tạo
SELECT 'Courses' as table_name, COUNT(*) as count FROM courses
UNION ALL
SELECT 'Units', COUNT(*) FROM units
UNION ALL
SELECT 'Lessons', COUNT(*) FROM lessons
UNION ALL
SELECT 'Quizzes', COUNT(*) FROM quizzes
UNION ALL
SELECT 'Questions', COUNT(*) FROM questions
UNION ALL
SELECT 'Question Options', COUNT(*) FROM question_options
UNION ALL
SELECT 'Question Types', COUNT(*) FROM question_types;

-- 9. Kiểm tra lesson với quizzes
SELECT 
    l.id as lesson_id,
    l.title as lesson_title,
    COUNT(q.id) as quiz_count,
    COUNT(qu.id) as question_count
FROM lessons l
LEFT JOIN quizzes q ON l.id = q.lesson_id
LEFT JOIN questions qu ON q.id = qu.quiz_id
GROUP BY l.id, l.title;

-- 10. Kiểm tra quiz với questions và options
SELECT 
    q.id as quiz_id,
    q.title as quiz_title,
    COUNT(qu.id) as question_count,
    COUNT(qo.id) as option_count
FROM quizzes q
LEFT JOIN questions qu ON q.id = qu.quiz_id
LEFT JOIN question_options qo ON qu.id = qo.question_id
GROUP BY q.id, q.title;

