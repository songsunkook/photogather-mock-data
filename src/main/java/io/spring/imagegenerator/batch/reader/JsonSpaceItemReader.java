package io.spring.imagegenerator.batch.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import io.spring.imagegenerator.dto.JsonSpaceData;

public class JsonSpaceItemReader implements ItemReader<JsonSpaceData.SpaceData>, ItemStream {
    
    private final String filePath;
    private final ObjectMapper objectMapper;
    private JsonParser parser;
    private FileInputStream fileInputStream;
    private boolean initialized = false;
    private boolean insideSpacesArray = false;
    private long processedCount = 0;
    private long skipCount = 0;
    private long totalSkipped = 0;
    
    public JsonSpaceItemReader(String filePath) {
        this(filePath, 0);
    }
    
    public JsonSpaceItemReader(String filePath, long skipCount) {
        this.filePath = filePath;
        this.skipCount = skipCount;
        this.objectMapper = new ObjectMapper();
        
        // Jackson의 LocalDateTime 처리를 위한 설정
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        javaTimeModule.addDeserializer(java.time.LocalDateTime.class, 
            new LocalDateTimeDeserializer(dateTimeFormatter));
        javaTimeModule.addSerializer(java.time.LocalDateTime.class, 
            new LocalDateTimeSerializer(dateTimeFormatter));
        
        this.objectMapper.registerModule(javaTimeModule);
    }
    
    @Override
    public JsonSpaceData.SpaceData read() throws Exception, UnexpectedInputException, 
                                                  ParseException, NonTransientResourceException {
        if (!initialized) {
            initializeReader();
            initialized = true;
        }
        
        try {
            // Find next SpaceData object in the stream
            while (parser.nextToken() != null) {
                if (!insideSpacesArray) {
                    // Look for "spaces" array
                    if (parser.getCurrentToken() == JsonToken.FIELD_NAME && "spaces".equals(parser.currentName())) {
                        parser.nextToken(); // Move to START_ARRAY
                        if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
                            insideSpacesArray = true;
                            System.out.println("Found spaces array - starting streaming read");
                        }
                    }
                } else {
                    // Inside spaces array
                    if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                        // Read one SpaceData object
                        JsonSpaceData.SpaceData spaceData = objectMapper.readValue(parser, JsonSpaceData.SpaceData.class);
                        processedCount++;
                        
                        // Skip logic for restart
                        if (totalSkipped < skipCount) {
                            totalSkipped++;
                            if (totalSkipped % 1000 == 0) {
                                System.out.printf("[JSON Reader] Skipping for restart: %d/%d spaces skipped\n", totalSkipped, skipCount);
                            }
                            continue; // Skip this item
                        }
                        
                        // Progress reporting
                        if (processedCount % 100 == 0) {
                            System.out.printf("[JSON Reader] Progress: %d spaces read from JSON (skipped: %d)\n", processedCount, totalSkipped);
                        }
                        
                        return spaceData;
                    } else if (parser.getCurrentToken() == JsonToken.END_ARRAY) {
                        // End of spaces array
                        System.out.printf("[JSON Reader] Completed: %d total spaces read from JSON (skipped: %d)\n", processedCount, totalSkipped);
                        closeResources();
                        return null;
                    }
                }
            }
            
            // End of file
            closeResources();
            return null;
            
        } catch (Exception e) {
            closeResources();
            throw new ParseException("Error reading JSON stream: " + e.getMessage(), e);
        }
    }
    
    private void initializeReader() throws IOException {
        System.out.printf("Starting streaming JSON read: %s\n", filePath);
        
        fileInputStream = new FileInputStream(filePath);
        parser = new JsonFactory().createParser(fileInputStream);
        
        // Find the root object
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw new ParseException("Expected JSON to start with object");
        }
    }
    
    private void closeResources() {
        try {
            if (parser != null) {
                parser.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing JSON reader resources: " + e.getMessage());
        }
    }
    
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            if (!initialized) {
                initializeReader();
                initialized = true;
            }
        } catch (IOException e) {
            throw new ItemStreamException("Failed to open JSON reader", e);
        }
    }
    
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putLong("processedCount", processedCount);
    }
    
    @Override
    public void close() throws ItemStreamException {
        closeResources();
    }
}