-- Seed tiêu chí cho mọi đợt chưa có bộ tiêu chí (đợt tạo sau V8).
INSERT INTO evaluation_criteria (
  name,
  description,
  registration_period_id,
  display_order,
  is_mandatory,
  is_active,
  created_at,
  updated_at
)
SELECT
  criterion.name,
  criterion.description,
  period.id,
  criterion.display_order,
  TRUE,
  TRUE,
  NOW(6),
  NOW(6)
FROM registration_periods period
CROSS JOIN (
  SELECT 'Nội dung' AS name, 'Mức độ đầy đủ, chính xác và phù hợp của nội dung đề tài.' AS description, 1 AS display_order
  UNION ALL
  SELECT 'Kỹ thuật', 'Giải pháp kỹ thuật, kiến trúc và chất lượng triển khai.', 2
  UNION ALL
  SELECT 'Sản phẩm', 'Mức độ hoàn thiện và khả năng vận hành của sản phẩm.', 3
  UNION ALL
  SELECT 'Báo cáo', 'Chất lượng tài liệu, cấu trúc và cách trình bày báo cáo.', 4
  UNION ALL
  SELECT 'Trình bày/phản biện', 'Khả năng trình bày, trả lời câu hỏi và bảo vệ kết quả.', 5
) criterion
WHERE NOT EXISTS (
  SELECT 1
  FROM evaluation_criteria existing
  WHERE existing.registration_period_id = period.id
    AND existing.name = criterion.name
);

-- Đợt đăng ký tạo sau này tự nhận 5 tiêu chí mẫu.
CREATE TRIGGER trg_period_after_insert_criteria
AFTER INSERT ON registration_periods
FOR EACH ROW
INSERT INTO evaluation_criteria (
  name,
  description,
  registration_period_id,
  display_order,
  is_mandatory,
  is_active,
  created_at,
  updated_at
)
SELECT
  criterion.name,
  criterion.description,
  NEW.id,
  criterion.display_order,
  TRUE,
  TRUE,
  NOW(6),
  NOW(6)
FROM (
  SELECT 'Nội dung' AS name, 'Mức độ đầy đủ, chính xác và phù hợp của nội dung đề tài.' AS description, 1 AS display_order
  UNION ALL
  SELECT 'Kỹ thuật', 'Giải pháp kỹ thuật, kiến trúc và chất lượng triển khai.', 2
  UNION ALL
  SELECT 'Sản phẩm', 'Mức độ hoàn thiện và khả năng vận hành của sản phẩm.', 3
  UNION ALL
  SELECT 'Báo cáo', 'Chất lượng tài liệu, cấu trúc và cách trình bày báo cáo.', 4
  UNION ALL
  SELECT 'Trình bày/phản biện', 'Khả năng trình bày, trả lời câu hỏi và bảo vệ kết quả.', 5
) criterion;

-- Một đề tài chỉ có một đăng ký APPROVED. Các trạng thái khác vẫn được lưu lịch sử.
ALTER TABLE topic_registration
  ADD COLUMN approved_topic_id BIGINT
    GENERATED ALWAYS AS (IF(status = 'APPROVED', topic_id, NULL)) STORED,
  ADD CONSTRAINT uk_topic_registration_approved_topic UNIQUE (approved_topic_id);
