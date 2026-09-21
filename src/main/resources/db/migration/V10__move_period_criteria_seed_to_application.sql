-- V9 originally used a database trigger to seed the five default evaluation
-- criteria. From V10 onward the application service owns that operation in the
-- same transaction that creates a registration period. This avoids requiring
-- TRIGGER privileges on managed MySQL hosting while keeping existing data.
DROP TRIGGER IF EXISTS trg_period_after_insert_criteria;

-- Backfill defensively for databases where a period was created before the
-- application-level seeding was enabled.
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
