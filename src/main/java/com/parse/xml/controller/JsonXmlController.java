package com.parse.xml.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.parse.xml.service.FileService;
import com.parse.xml.service.JsonXmlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class JsonXmlController {

    private final JsonXmlService jsonXmlService;
    private final FileService fileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/parse")
    public ResponseEntity<?> parseJsonXml(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty".getBytes(StandardCharsets.UTF_8));
        }

        String jsonString = new String(file.getBytes(), StandardCharsets.UTF_8);
        JSONArray result = jsonXmlService.parseJsonAndXml(jsonString);
        String resultString = result.toString(2);
        byte[] resultBytes = resultString.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", "literature_result.json"); // 다운로드 파일 이름 설정
        headers.setContentLength(resultBytes.length);


        return new ResponseEntity<>(resultBytes, headers, HttpStatus.OK);
    }


    @PostMapping("/download-csv")
    public ResponseEntity<?> downloadCsvFile(@RequestParam("file") MultipartFile file) throws Exception {
            // 파일 내용을 JSON으로 변환
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<Map<String, Object>> jsonData = new ObjectMapper().readValue(content, new TypeReference<List<Map<String, Object>>>() {});

            // CSV 파일 생성
            //ByteArrayInputStream csvFile = fileService.generateCsvFileFromSource(jsonData);
            ByteArrayInputStream csvFile = fileService.generateCsvFileWithEncoding(jsonData, "EUC-KR");
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data.csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/csv"))
                    .body(new InputStreamResource(csvFile));
    }

}
