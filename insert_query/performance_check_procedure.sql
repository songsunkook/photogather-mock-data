-- 성능 측정 프로시저, 최초 1회 설정
DELIMITER $$
CREATE PROCEDURE ShowLoadResults()
BEGIN
    DECLARE end_time DATETIME(6) DEFAULT NOW(6);
    DECLARE execution_time DECIMAL(10,6);
    DECLARE rows_loaded INT DEFAULT ROW_COUNT();

    SET execution_time = TIMESTAMPDIFF(MICROSECOND, @start_time, end_time) / 1000000;

SELECT
    'SUCCESS' as status,
    execution_time as execution_time_seconds,
    rows_loaded as rows_loaded,
    ROUND(rows_loaded / execution_time, 0) as rows_per_second;
END$$
DELIMITER ;
