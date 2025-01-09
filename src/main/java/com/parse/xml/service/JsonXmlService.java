package com.parse.xml.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class JsonXmlService {


    public JSONArray parseJsonAndXml(String jsonString) throws Exception {
        JSONArray jsonArray = new JSONArray(jsonString);
        JSONArray resultArray = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            JSONObject sourceObject = jsonObject.getJSONObject("_source");
            String xmlString = sourceObject.getString("item_xml_t");

            // 따옴표, 세미콜론, 앞뒤 공백 제거
            xmlString = xmlString.replace("\"\"<", "<").replace(">\"\"", ">");
            xmlString = xmlString.replace(";", "");
            xmlString = xmlString.trim();

            Map<String, Object> parsedXml = parseXml(xmlString);
            JSONObject itemXml = new JSONObject(parsedXml);

            sourceObject.put("item_xml", itemXml);
            resultArray.put(jsonObject);
        }

        return resultArray;
    }



    private Map<String, Object> parseXml(String xmlString) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xmlString));
        Document doc = builder.parse(is);
        XPath xpath = XPathFactory.newInstance().newXPath();

        Map<String, Object> resultMap = new HashMap<>();

        // 1. Question 파싱
        String question = extractText(doc, xpath, "//Question/Paragraph[1]//Run/@Text");
        if (question != null) {
            String tableQuestion = extractText(doc, xpath, "//Table//Run/@Text");
            if (tableQuestion != null) {
                question += tableQuestion;
            }
            resultMap.put("question", decode(question));
        }

        // 2. List 파싱 (option & answer)
        List<String> optionList = new ArrayList<>();
        for (int j = 1; j <= 5; j++) {
            String optionPath = "//List[@IsCorrectAll='True']/ListItem[@OriginalSequence='" + j + "']//Run/@Text";
            String option = extractText(doc, xpath, optionPath);
            if (option != null) {
                optionList.add(decode(option));

                String isCorrectPath = "//List[@IsCorrectAll='True']/ListItem[@OriginalSequence='" + j + "' and @IsCorrectAnswer='True']";
                if (xpath.evaluate(isCorrectPath, doc, XPathConstants.NODE) != null) {
                    resultMap.put("answer", j + "번");
                }
            }
        }
        resultMap.put("option_list", optionList);


        // 3. CorrectAnswer 파싱 (answer - TextBox)
        String correctAnswer = extractText(doc, xpath, "//CorrectAnswer");
        if (correctAnswer != null) {
            resultMap.put("answer", decode(correctAnswer));
        }

        // 4. Explanation - intention 파싱
        String intention = extractText(doc, xpath, "//Explanation/Paragraph[1]//Run/@Text");
        if (intention != null) {
            resultMap.put("intention", decode(intention).trim());
        }

        // 5. Explanation 파싱
        String explanation = extractText(doc, xpath, "//Explanation/Paragraph[position() > 1]//Run/@Text");
        if (explanation != null) {
            resultMap.put("explanation", decode(explanation));
        }

        return resultMap;
    }

    private String extractText(Document doc, XPath xpath, String expression) throws XPathExpressionException {
        StringBuilder sb = new StringBuilder();
        NodeList nodeList = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            sb.append(node.getTextContent());
        }
        return !sb.isEmpty() ? sb.toString() : null;
    }

    private String decode(String text) {
        if(text == null) return null;
        String unicodeDecoded = StringEscapeUtils.unescapeJava(text);
        return StringEscapeUtils.unescapeHtml4(unicodeDecoded);
    }
}