-- V8__seed_evaluation_data.sql

-- Giả định có registration_period_id = 1, insert các criteria mẫu
INSERT INTO evaluation_criteria (name, description, registration_period_id, display_order, is_mandatory, is_active, created_at, updated_at)
VALUES
('Nội dung', 'Đánh giá nội dung đề tài', 1, 1, TRUE, TRUE, NOW(), NOW()),
('Kỹ thuật', 'Đánh giá kỹ thuật sử dụng', 1, 2, TRUE, TRUE, NOW(), NOW()),
('Sản phẩm', 'Đánh giá sản phẩm hoàn thiện', 1, 3, TRUE, TRUE, NOW(), NOW()),
('Báo cáo', 'Đánh giá chất lượng báo cáo', 1, 4, TRUE, TRUE, NOW(), NOW()),
('Trình bày/Phản biện', 'Đánh giá kỹ năng trình bày và phản biện', 1, 5, TRUE, TRUE, NOW(), NOW());
