UPDATE departments
SET name = 'An toàn thông tin',
    description = 'Phụ trách đào tạo và nghiên cứu an toàn thông tin.',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'MMT';
