-- space 데이터 삽입
-- 1. 시작 시간 설정
SET @start_time = NOW(6);

-- 2. 직접 LOAD DATA 실행 (프로시저 밖에서)
LOAD DATA LOCAL INFILE '/home/ubuntu/test-data/1_space.csv'
INTO TABLE space -- 테이블 변경
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(code, name, valid_hours, opened_at, max_capacity, type, created_at, updated_at);

-- 3. 결과 확인
CALL ShowLoadResults();

-- host 데이터 삽입
SET @start_time = NOW(6);

LOAD DATA LOCAL INFILE '/home/ubuntu/test-data/2_host.csv'
INTO TABLE host
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(name, picture_url, agreed_terms, created_at, updated_at);

CALL ShowLoadResults();

-- space_host_map 삽입
SET @start_time = NOW(6);

LOAD DATA LOCAL INFILE '/home/ubuntu/test-data/3_space_host_map.csv'
INTO TABLE space_host_map
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(space_id, host_id, created_at, updated_at);

CALL ShowLoadResults();

-- host_kakao 삽입
SET @start_time = NOW(6);

LOAD DATA LOCAL INFILE '/home/ubuntu/test-data/4_host_kakao.csv'
INTO TABLE host_kakao
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(host_id, user_id);

CALL ShowLoadResults();

-- guest 삽입
SET @start_time = NOW(6);

LOAD DATA LOCAL INFILE '/home/ubuntu/test-data/5_guest.csv'
INTO TABLE guest
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(space_id, name, created_at, updated_at);

CALL ShowLoadResults();

-- space_content 삽입
SET @start_time = NOW(6);

LOAD DATA LOCAL INFILE '/home/ubuntu/test-data/6_space_content.csv'
INTO TABLE space_content
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(content_type, space_id, guest_id);

CALL ShowLoadResults();

-- photo 삽입
SET @start_time = NOW(6);

LOAD DATA LOCAL INFILE '/home/ubuntu/test-data/7_photo.csv'
INTO TABLE photo
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(original_name, path, captured_at, capacity, created_at);

CALL ShowLoadResults();
