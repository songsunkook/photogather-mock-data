package io.spring.imagegenerator.csv;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Scanner;

public class SpaceMockDataGenerator {

    private static final String[] SPACE_NAMES = {
        "여행 추억", "생일 파티", "졸업식", "가족 모임", "친구들과 함께",
        "회사 워크샵", "결혼식 준비", "아기 백일잔치", "휴가 사진", "동창회",
        "캠핑 추억", "콘서트 현장", "맛집 탐방", "운동회", "축제 현장"
    };

    private static final String[] GUEST_NAMES = {
        "김민수", "이영희", "박철수", "정수정", "최영준", "한지민", "강호동", "송지효",
        "유재석", "김태희", "이병헌", "전지현", "조인성", "윤아", "수지", "아이유",
        "김우빈", "박보영", "송중기", "김고은"
    };

    private static final String[] HOST_NAMES = {
        "호스트김", "호스트이", "호스트박", "호스트최", "호스트정", "호스트한",
        "관리자A", "관리자B", "관리자C", "운영자1", "운영자2", "매니저김"
    };

    private static final String[] SPACE_TYPES = {"PRIVATE", "PUBLIC"};

    private Random random = new Random();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("생성할 Space 개수를 입력하세요: ");
        int spaceCount = scanner.nextInt();

        SpaceMockDataGenerator generator = new SpaceMockDataGenerator();
        generator.generateAllMockData(spaceCount);
        scanner.close();
    }

    public void generateAllMockData(int spaceCount) {
        try {
            System.out.println("=== 대용량 데이터 생성 시작 ===");
            System.out.println("Space: " + spaceCount + "개");
            System.out.println("Host: " + spaceCount + "개 (1:1 매핑)");
            System.out.println("Guest: " + (spaceCount * 10) + "개 (스페이스당 10명)");
            System.out.println("Space Content: " + (spaceCount * 10 * 20) + "개 (게스트당 20개)");
            System.out.println("Photo: " + (spaceCount * 10 * 20) + "개");
            System.out.println("===============================");

            generateSpaceData("1_space.csv", spaceCount);
            generateHostData("2_host.csv", spaceCount); // 1:1 매핑
            generateSpaceHostMapData("3_space_host_map.csv", spaceCount);
            generateHostKakaoData("4_host_kakao.csv", spaceCount);
            generateGuestData("5_guest.csv", spaceCount); // 스페이스당 10명씩
            generateSpaceContentData("6_space_content.csv", spaceCount); // 게스트당 20개씩
            generatePhotoData("7_photo.csv", spaceCount * 10 * 20); // space_content와 1:1 매핑

            System.out.println("모든 목데이터 파일이 생성되었습니다!");
        } catch (Exception e) {
            System.err.println("데이터 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 1. Space 데이터 생성
    public void generateSpaceData(String fileName, int count) throws IOException {
        try (FileWriter writer = new FileWriter(fileName, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.append("code,name,valid_hours,opened_at,max_capacity,type,created_at,updated_at\n");

            for (int i = 1; i <= count; i++) {
                String code = "SPSP" + String.format("%06d", i);
                String name = SPACE_NAMES[random.nextInt(SPACE_NAMES.length)];
                int validHours = (random.nextInt(7) + 1) * 24; // 1~7일

                // createdAt: 3년 전부터 1개월 전까지
                String createdAt = getRandomDateTime(-1095, -30); // 3년(1095일) 전부터 30일 전까지
                // openedAt: createdAt 이후부터 현재까지
                String openedAt = getRandomDateTimeAfter(createdAt, 0); // createdAt 이후부터 현재까지

                long maxCapacity = (3 + random.nextInt(8)) * 1073741824L; // 2GB~10GB
                String type = SPACE_TYPES[random.nextInt(SPACE_TYPES.length)];
                String updatedAt = getRandomDateTimeAfter(createdAt, 0); // createdAt 이후부터 현재까지

                writer.append(String.format("%s,\"%s\",%d,%s,%d,%s,%s,%s\n",
                    code, name, validHours, openedAt, maxCapacity, type, createdAt, updatedAt));
            }

            System.out.println("Space 데이터 " + count + "개 생성 완료");
        }
    }

    // 2. Host 데이터 생성 (Space와 1:1 매핑)
    public void generateHostData(String fileName, int count) throws IOException {
        try (FileWriter writer = new FileWriter(fileName, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.append("name,picture_url,agreed_terms,created_at,updated_at\n");

            for (int i = 1; i <= count; i++) {
                String name = HOST_NAMES[random.nextInt(HOST_NAMES.length)] + i;
                String pictureUrl = "https://example.com/profile/" + i + ".jpg";
                boolean agreedTerms = random.nextBoolean();
                String createdAt = getRandomDateTime(-1095, -30); // 3년 전부터 30일 전까지
                String updatedAt = getRandomDateTimeAfter(createdAt, 0); // createdAt 이후부터 현재까지

                writer.append(String.format("\"%s\",%s,%b,%s,%s\n",
                    name, pictureUrl, agreedTerms, createdAt, updatedAt));

            }

            System.out.println("Host 데이터 " + count + "개 생성 완료");
        }
    }

    // 3. Space-Host 매핑 데이터 생성 (1:1 매핑)
    public void generateSpaceHostMapData(String fileName, int spaceCount) throws IOException {
        try (FileWriter writer = new FileWriter(fileName, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.append("space_id,host_id,created_at,updated_at\n");

            for (int i = 1; i <= spaceCount; i++) {
                String createdAt = getRandomDateTime(-1095, -30); // 3년 전부터 30일 전까지
                String updatedAt = getRandomDateTimeAfter(createdAt, 0); // createdAt 이후부터 현재까지

                // 1:1 매핑: space_id = host_id (AUTO_INCREMENT 순서로)
                writer.append(String.format("%d,%d,%s,%s\n",
                    i, i, createdAt, updatedAt));

            }

            System.out.println("Space-Host 매핑 데이터 " + spaceCount + "개 생성 완료");
        }
    }

    // 4. Host Kakao 데이터 생성
    public void generateHostKakaoData(String fileName, int hostCount) throws IOException {
        try (FileWriter writer = new FileWriter(fileName, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.append("host_id,user_id\n");

            for (int i = 1; i <= hostCount; i++) {
                String userId = "kakao_" + (100000000 + random.nextInt(900000000));

                writer.append(String.format("%d,%s\n", i, userId));

            }

            System.out.println("Host Kakao 데이터 " + hostCount + "개 생성 완료");
        }
    }

    // 5. Guest 데이터 생성 (각 스페이스마다 정확히 10명)
    public void generateGuestData(String fileName, int spaceCount) throws IOException {
        try (FileWriter writer = new FileWriter(fileName, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.append("space_id,name,created_at,updated_at\n");

            int guestCount = 0;
            for (int spaceId = 1; spaceId <= spaceCount; spaceId++) {
                // 각 스페이스마다 정확히 10명의 게스트 생성
                for (int guestIndex = 1; guestIndex <= 10; guestIndex++) {
                    String name = GUEST_NAMES[random.nextInt(GUEST_NAMES.length)];
                    String createdAt = getRandomDateTime(-1095, 0); // 3년 전부터 현재까지
                    String updatedAt = getRandomDateTimeAfter(createdAt, 0); // createdAt 이후부터 현재까지

                    writer.append(String.format("%d,\"%s\",%s,%s\n",
                        spaceId, name, createdAt, updatedAt));

                    guestCount++;
                }

            }

            System.out.println("Guest 데이터 " + guestCount + "개 생성 완료 (스페이스당 10명)");
        }
    }

    // 6. Space Content 데이터 생성 (각 게스트마다 정확히 20개)
    public void generateSpaceContentData(String fileName, int spaceCount) throws IOException {
        try (FileWriter writer = new FileWriter(fileName, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.append("content_type,space_id,guest_id\n");

            int contentId = 1;
            int guestId = 1;

            for (int spaceId = 1; spaceId <= spaceCount; spaceId++) {
                // 각 스페이스의 10명의 게스트
                for (int guestIndex = 1; guestIndex <= 10; guestIndex++) {
                    // 각 게스트마다 20개의 컨텐츠
                    for (int contentIndex = 1; contentIndex <= 20; contentIndex++) {
                        String contentType = "PHOTO";

                        writer.append(String.format("%s,%d,%d\n",
                            contentType, spaceId, guestId));

                        contentId++;
                    }
                    guestId++;
                }

            }

            System.out.println("Space Content 데이터 " + (contentId - 1) + "개 생성 완료 (게스트당 20개)");
        }
    }

    // 7. Photo 데이터 생성 (space_content.id와 1:1 매핑)
    public void generatePhotoData(String fileName, int count) throws IOException {
        try (FileWriter writer = new FileWriter(fileName, java.nio.charset.StandardCharsets.UTF_8)) {
            writer.append("original_name,path,captured_at,capacity,created_at\n");

            for (int i = 1; i <= count; i++) {
                String originalName = "photo_" + i + ".jpg";
                String path = "/forgather/contents/" + (i / 10000) + "/" + originalName; // 폴더 구조화
                String capturedAt = random.nextBoolean() ? getRandomDateTime(-1095, 0) : ""; // 3년 전부터 현재까지
                long capacity = (1 + random.nextInt(10)) * 1024L * 1024L; // 1MB~10MB
                String createdAt = getRandomDateTime(-1095, 0); // 3년 전부터 현재까지

                // photo.id = space_content.id (FK 제약조건)
                writer.append(String.format("\"%s\",\"%s\",%s,%d,%s\n", originalName, path, capturedAt, capacity,
                    createdAt));

            }

            System.out.println("Photo 데이터 " + count + "개 생성 완료");
        }
    }

    // 랜덤 날짜시간 생성 (현재 기준 fromDays일 전부터 toDays일 전까지)
    private String getRandomDateTime(int fromDays, int toDays) {
        LocalDateTime now = LocalDateTime.now();
        int randomDays = fromDays + random.nextInt(Math.abs(toDays - fromDays) + 1);
        LocalDateTime randomDate = now.plusDays(randomDays);

        // 시간도 랜덤하게 조정
        randomDate = randomDate.withHour(random.nextInt(24))
            .withMinute(random.nextInt(60))
            .withSecond(random.nextInt(60));

        return randomDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // 특정 날짜 이후부터 현재까지의 랜덤 날짜시간 생성
    private String getRandomDateTimeAfter(String afterDateTime, int toDays) {
        LocalDateTime baseDate = LocalDateTime.parse(afterDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime now = LocalDateTime.now().plusDays(toDays);

        // baseDate와 now 사이의 랜덤한 시점 선택
        long daysBetween = java.time.Duration.between(baseDate, now).toDays();
        if (daysBetween <= 0) {
            return baseDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        long randomDays = random.nextLong(daysBetween + 1);
        LocalDateTime randomDate = baseDate.plusDays(randomDays);

        // 시간도 랜덤하게 조정
        randomDate = randomDate.withHour(random.nextInt(24))
            .withMinute(random.nextInt(60))
            .withSecond(random.nextInt(60));

        return randomDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
