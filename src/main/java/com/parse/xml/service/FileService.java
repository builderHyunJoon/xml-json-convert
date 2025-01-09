package com.parse.xml.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class FileService {

    public ByteArrayInputStream generateCsvFileWithEncoding(List<Map<String, Object>> jsonData, String encoding) {
        // Extract `_source` field for CSV generation
        List<Map<String, Object>> sourceData = jsonData.stream()
                .map(data -> (Map<String, Object>) data.get("_source"))
                .toList();

        // Retrieve headers from the first entry in `_source`
        String[] headers = sourceData.isEmpty() ? new String[0] : sourceData.get(0).keySet().toArray(new String[0]);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, Charset.forName(encoding));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers))
        ) {
            // Write each row based on `_source` fields
            for (Map<String, Object> record : sourceData) {
                csvPrinter.printRecord(Arrays.stream(headers).map(record::get).toList());
            }

            csvPrinter.flush();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error while generating CSV file", e);
        }
    }
}

