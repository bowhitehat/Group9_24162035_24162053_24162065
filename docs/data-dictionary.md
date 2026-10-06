# Từ điển dữ liệu từ JPA model

Nguồn: `src/main/java/com/group9/topicmanagement/model`, đối chiếu ngày 06/10/2026. Tên cột mặc định chuyển camelCase sang snake_case. Các bảng nghiệp vụ kế thừa `created_at`, `updated_at` từ BaseEntity; enum lưu STRING. Danh sách dưới đây phản ánh model, không phải bản export schema MySQL đang chạy.

## departments

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| code | code | String | NOT NULL; length=20 |
| name | name | String | NOT NULL; length=160 |
| description | description | String | length=1000 |
| active | active | boolean | NOT NULL |

Unique constraint cấp bảng: `code`.

## registration_periods

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| name | name | String | NOT NULL; length=180 |
| type | type | PeriodType | Enumerated; NOT NULL; length=20 |
| status | status | PeriodStatus | Enumerated; NOT NULL; length=40 |
| lecturerStart | lecturer_start | LocalDateTime | NOT NULL |
| lecturerEnd | lecturer_end | LocalDateTime | NOT NULL |
| studentStart | student_start | LocalDateTime | NOT NULL |
| studentEnd | student_end | LocalDateTime | NOT NULL |
| reportSubmissionDeadline | report_submission_deadline | LocalDateTime | Mặc định JPA |
| reviewDeadline | review_deadline | LocalDateTime | Mặc định JPA |
| councilDate | council_date | LocalDate | Mặc định JPA |

## roles

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| name | name | RoleName | Enumerated; NOT NULL; length=40 |

Unique constraint cấp bảng: `name`.

## users

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| username | username | String | NOT NULL; length=80 |
| passwordHash | password_hash | String | NOT NULL; length=100 |
| fullName | full_name | String | NOT NULL; length=160 |
| email | email | String | NOT NULL; length=160 |
| studentCode | student_code | String | length=30 |
| enabled | enabled | boolean | NOT NULL |
| department | department_id | Department | ManyToOne |
| roles | user_roles | Set<Role> | ManyToMany; bảng nối user_roles |

Unique constraint trên users: `username`, `email`, `student_code`. Bảng nối user_roles có unique `(user_id, role_id)`.

## announcements

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| title | title | String | NOT NULL |
| content | content | String | NOT NULL |
| creator | creator_id | User | ManyToOne |
| status | status | AnnouncementStatus | Enumerated; NOT NULL; length=20 |
| targetRoles | announcement_target_roles | Set<Role> | ManyToMany; bảng nối announcement_target_roles |
| publishedTime | published_time | LocalDateTime | Mặc định JPA |
| expirationTime | expiration_time | LocalDateTime | Mặc định JPA |

Unique constraint trên bảng nối announcement_target_roles: `(announcement_id, role_id)`.

## councils

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| name | name | String | NOT NULL |
| registrationPeriod | registration_period_id | RegistrationPeriod | ManyToOne |
| reportDate | report_date | LocalDateTime | Mặc định JPA |
| location | location | String | Mặc định JPA |
| status | status | CouncilStatus | Enumerated; NOT NULL |

## council_assignments

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| council | council_id | Council | ManyToOne |
| topic | topic_id | Topic | ManyToOne |

Unique constraint cấp bảng: `council_id, topic_id`.

## council_members

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| council | council_id | Council | ManyToOne |
| member | member_id | User | ManyToOne |
| role | role | CouncilMemberRole | Enumerated; NOT NULL |

Unique constraint cấp bảng: `council_id, member_id`.

## evaluations

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| topic | topic_id | Topic | ManyToOne |
| evaluator | evaluator_id | User | ManyToOne |
| evaluationType | evaluation_type | EvaluationType | Enumerated; NOT NULL; length=30 |
| reviewerAssignment | reviewer_assignment_id | ReviewerAssignment | ManyToOne |
| councilMember | council_member_id | CouncilMember | ManyToOne |
| status | status | EvaluationStatus | Enumerated; NOT NULL; length=20 |
| comments | comments | String | Mặc định JPA |
| submissionTime | submission_time | LocalDateTime | Mặc định JPA |
| lockedTime | locked_time | LocalDateTime | Mặc định JPA |
| scores | Quan hệ ngược | List<EvaluationScore> | OneToMany; mappedBy=evaluation |

Unique constraint cấp bảng: `topic_id, evaluator_id, evaluation_type`.

## evaluation_criteria

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| name | name | String | NOT NULL |
| description | description | String | Mặc định JPA |
| registrationPeriod | registration_period_id | RegistrationPeriod | ManyToOne |
| displayOrder | display_order | Integer | NOT NULL |
| isMandatory | is_mandatory | Boolean | Mặc định JPA |
| isActive | is_active | Boolean | Mặc định JPA |

Unique constraint cấp bảng: `registration_period_id, name`, `registration_period_id, display_order`.

## evaluation_scores

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| evaluation | evaluation_id | Evaluation | ManyToOne |
| criterion | criterion_id | EvaluationCriterion | ManyToOne |
| score | score | BigDecimal | NOT NULL; precision=4; scale=2 |
| note | note | String | length=500 |

Unique constraint cấp bảng: `evaluation_id, criterion_id`.

## reviewer_assignments

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| topic | topic_id | Topic | ManyToOne |
| reviewer | reviewer_id | User | ManyToOne |
| assigner | assigner_id | User | ManyToOne |
| assignedAt | assigned_at | LocalDateTime | Mặc định JPA |
| deadline | deadline | LocalDateTime | Mặc định JPA |
| status | status | ReviewerAssignmentStatus | Enumerated; NOT NULL |
| submissionTime | submission_time | LocalDateTime | Mặc định JPA |
| note | note | String | length=500 |

Unique constraint cấp bảng: `topic_id, reviewer_id`.

## topic_results

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| topic | topic_id | Topic | OneToOne; UNIQUE |
| councilAssignment | council_assignment_id | CouncilAssignment | ManyToOne; UNIQUE |
| finalScore | final_score | BigDecimal | precision=4; scale=2 |
| status | status | TopicResultStatus | Enumerated; NOT NULL; length=30 |
| confirmer | confirmer_id | User | ManyToOne |
| confirmedTime | confirmed_time | LocalDateTime | Mặc định JPA |
| publisher | publisher_id | User | ManyToOne |
| publishedTime | published_time | LocalDateTime | Mặc định JPA |

## report_submission

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| studentGroup | student_group_id | StudentGroup | ManyToOne |
| topicRegistration | topic_registration_id | TopicRegistration | ManyToOne |
| submitter | submitter_id | User | ManyToOne |
| originalFileName | original_file_name | String | NOT NULL; length=255 |
| storedFileName | stored_file_name | String | NOT NULL; length=255 |
| contentType | content_type | String | NOT NULL; length=100 |
| fileSize | file_size | Long | NOT NULL |
| version | version | Integer | NOT NULL |
| note | note | String | length=500 |

Unique constraint cấp bảng: `topic_registration_id, version`.

## topic_registration

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| studentGroup | student_group_id | StudentGroup | ManyToOne |
| topic | topic_id | Topic | ManyToOne |
| registrationPeriod | registration_period_id | RegistrationPeriod | ManyToOne |
| status | status | RegistrationStatus | Enumerated; NOT NULL; length=20 |
| approver | approver_id | User | ManyToOne |
| rejectionReason | rejection_reason | String | length=500 |
| approvedAt | approved_at | LocalDateTime | Mặc định JPA |

Unique constraint cấp bảng: `student_group_id, registration_period_id`.

## group_member

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| group | group_id | StudentGroup | ManyToOne |
| member | member_id | User | ManyToOne |
| isLeader | is_leader | boolean | NOT NULL |

Unique constraint cấp bảng: `group_id, member_id`.

## student_group

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| registrationPeriod | registration_period_id | RegistrationPeriod | ManyToOne |
| leader | leader_id | User | ManyToOne |
| members | Quan hệ ngược | Set<GroupMember> | OneToMany; mappedBy=group |

Unique constraint cấp bảng: `registration_period_id, leader_id`.

## topic

| Thuộc tính | Cột / quan hệ | Kiểu Java | Mapping / ràng buộc |
|---|---|---|---|
| id | id | Long | Id; GeneratedValue |
| code | code | String | NOT NULL; length=20 |
| title | title | String | NOT NULL; length=255 |
| description | description | String | Mặc định JPA |
| requirement | requirement | String | Mặc định JPA |
| department | department_id | Department | ManyToOne |
| registrationPeriod | registration_period_id | RegistrationPeriod | ManyToOne |
| status | status | TopicStatus | Enumerated; NOT NULL; length=20 |
| proposer | proposer_id | User | ManyToOne |
| advisors | topic_advisors | Set<User> | ManyToMany; bảng nối topic_advisors |
| rejectionReason | rejection_reason | String | length=500 |

Unique constraint trên topic: `(code, registration_period_id)`. Bảng nối topic_advisors có unique `(topic_id, advisor_id)`.

## Bảng nối

- `user_roles`: user_id → users; role_id → roles.
- `topic_advisors`: topic_id → topic; advisor_id → users.
- `announcement_target_roles`: announcement_id → announcements; role_id → roles.

Các khóa ngoại được đặt tên trong relationship mapping. Quy tắc phụ thuộc trạng thái, vai trò hoặc hạn thời gian vẫn kiểm tra tại service; UNIQUE không thay thế các kiểm tra này.
