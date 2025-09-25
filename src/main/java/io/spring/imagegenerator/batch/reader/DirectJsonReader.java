package io.spring.imagegenerator.batch.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

@Component
public class DirectJsonReader implements ItemReader<Map<String, Object>> {
    
    private final String filePath = "src/main/resources/space_data.json";
    private JsonParser parser;
    private boolean initialized = false;
    private int processedCount = 0;
    
    // ID 생성기
    private AtomicLong spaceIdGenerator = new AtomicLong(1);
    private AtomicLong hostIdGenerator = new AtomicLong(1);
    private AtomicLong guestIdGenerator = new AtomicLong(1);
    private AtomicLong spaceContentIdGenerator = new AtomicLong(1);
    
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private void initialize() throws IOException {
        if (!initialized) {
            FileInputStream fis = new FileInputStream(filePath);
            parser = new JsonFactory().createParser(fis);
            
            // JSON 구조 탐색
            if (parser.nextToken() == JsonToken.START_OBJECT) {
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String fieldName = parser.getCurrentName();
                    if ("spaces".equals(fieldName)) {
                        parser.nextToken(); // START_ARRAY
                        break;
                    }
                    parser.skipChildren();
                }
            }
            
            initialized = true;
            System.out.println("[Direct JSON Reader] Initialized - ready for streaming read");
        }
    }

    @Override
    public Map<String, Object> read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        initialize();
        
        JsonToken token = parser.nextToken();
        if (token == JsonToken.END_ARRAY || token == null) {
            parser.close();
            return null; // 읽기 완료
        }
        
        // START_OBJECT가 아니면 스키핑
        if (token != JsonToken.START_OBJECT) {
            parser.skipChildren();
            return read(); // 다음 객체 시도
        }
        
        // JSON에서 직접 원시 데이터 추출 (객체 생성 최소화)
        Map<String, Object> rawData = new HashMap<>();
        
        // Space 기본 정보
        rawData.put("spaceId", spaceIdGenerator.getAndIncrement());
        
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            if (fieldName == null) {
                parser.skipChildren(); // null인 경우 해당 값 건너뛰기
                continue;
            }
            parser.nextToken(); // 필드 값으로 이동
            
            try {
                switch (fieldName) {
                    case "code" -> rawData.put("code", parser.getText());
                    case "name" -> rawData.put("name", parser.getText());
                    case "validHours" -> rawData.put("validHours", parser.getIntValue());
                    case "openedAt" -> rawData.put("openedAt", parser.getText());
                    case "maxCapacity" -> rawData.put("maxCapacity", parser.getLongValue());
                    case "type" -> rawData.put("type", parser.getText());
                    case "createdAt" -> rawData.put("createdAt", parser.getText());
                    case "updatedAt" -> rawData.put("updatedAt", parser.getText());
                
                case "host" -> {
                    if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                        Map<String, Object> hostData = new HashMap<>();
                        hostData.put("hostId", hostIdGenerator.getAndIncrement());
                        
                        while (parser.nextToken() != JsonToken.END_OBJECT) {
                            String hostField = parser.getCurrentName();
                            if (hostField == null) {
                                parser.skipChildren();
                                continue;
                            }
                            parser.nextToken();
                            
                            switch (hostField) {
                                case "name" -> hostData.put("name", parser.getText());
                                case "pictureUrl" -> hostData.put("pictureUrl", parser.getText());
                                case "agreedTerms" -> hostData.put("agreedTerms", parser.getBooleanValue());
                                case "createdAt" -> hostData.put("createdAt", parser.getText());
                                case "updatedAt" -> hostData.put("updatedAt", parser.getText());
                                case "kakao" -> {
                                    if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                                        Map<String, Object> kakaoData = new HashMap<>();
                                        while (parser.nextToken() != JsonToken.END_OBJECT) {
                                            String kakaoField = parser.getCurrentName();
                                            if (kakaoField == null) {
                                                parser.skipChildren();
                                                continue;
                                            }
                                            parser.nextToken();
                                            if ("userId".equals(kakaoField)) {
                                                String userId = parser.getText();
                                                if (userId != null && !userId.isEmpty()) {
                                                    kakaoData.put("userId", userId);
                                                }
                                            } else {
                                                parser.skipChildren();
                                            }
                                        }
                                        // kakao 데이터가 있을 때만 저장
                                        if (!kakaoData.isEmpty()) {
                                            hostData.put("kakao", kakaoData);
                                        }
                                    }
                                }
                                default -> parser.skipChildren();
                            }
                        }
                        // host 데이터가 의미있을 때만 저장 (hostId는 항상 생성됨)
                        if (hostData.size() > 1) { // hostId만 있는 게 아니라 다른 데이터도 있을 때
                            rawData.put("host", hostData);
                        }
                    }
                }
                
                case "guests" -> {
                    if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
                        // Guest 수만 세고 건너뛰기 (나중에 Writer에서 직접 처리)
                        int guestCount = 0;
                        int photoCount = 0;
                        
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                                guestCount++;
                                
                                while (parser.nextToken() != JsonToken.END_OBJECT) {
                                    String guestField = parser.getCurrentName();
                                    if (guestField == null) {
                                        parser.skipChildren();
                                        continue;
                                    }
                                    parser.nextToken();
                                
                                    if ("photos".equals(guestField) && parser.getCurrentToken() == JsonToken.START_ARRAY) {
                                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                                            photoCount++;
                                            parser.skipChildren(); // Photo 객체 건너뛰기
                                        }
                                    } else {
                                        parser.skipChildren();
                                    }
                                }
                            } else {
                                parser.skipChildren(); // START_OBJECT가 아닌 경우 건너뛰기
                            }
                        }
                        
                        rawData.put("guestCount", guestCount);
                        rawData.put("photoCount", photoCount);
                        
                        // 나중에 Writer에서 사용할 ID 범위 저장
                        long startGuestId = guestIdGenerator.get();
                        guestIdGenerator.addAndGet(guestCount);
                        rawData.put("startGuestId", startGuestId);
                        
                        long startContentId = spaceContentIdGenerator.get();
                        spaceContentIdGenerator.addAndGet(photoCount);
                        rawData.put("startContentId", startContentId);
                    }
                }
                
                default -> parser.skipChildren();
                }
            } catch (Exception e) {
                System.err.println("Error processing field '" + fieldName + "': " + e.getMessage());
                parser.skipChildren(); // 에러 발생시 해당 필드 건너뛰기
            }
        }
        
        processedCount++;
        if (processedCount % 100 == 0) {
            System.out.printf("[Direct JSON Reader] Processed %d spaces\n", processedCount);
        }
        
        return rawData;
    }
}
