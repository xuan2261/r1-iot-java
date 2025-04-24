package huan.diy.r1iot.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import huan.diy.r1iot.direct.AIDirect;
import huan.diy.r1iot.direct.AiAssistant;
import huan.diy.r1iot.model.AsrHandleType;
import huan.diy.r1iot.model.AsrResult;
import huan.diy.r1iot.util.R1IotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class AsrServerHandler {


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AIDirect aiDirect;

    public AsrResult handle(String data) {
        try {
            // drop if contentLength = 0
            if (data.contains("Content-Length: 0")) {
                return new AsrResult(AsrHandleType.DROPPED, data, data);
            }

            JsonNode jsonNode;
            String lastLine;
            try {
                String[] lines = data.split("\n");
                lastLine = lines[lines.length - 1];
                jsonNode = objectMapper.readTree(lastLine);
            } catch (Exception e) {
                // some case, asr only return partial json snippet
                // ObjectMapper có khả năng xử lý lỗi
                return new AsrResult(AsrHandleType.APPEND, data, data);
            }

            if (lastLine.length() - jsonNode.toString().length() > 5) {
                log.warn("obj {}", data);
                return new AsrResult(AsrHandleType.APPEND, data, data);
            }

            // contain text
            if (jsonNode.has("text")) {
                return new AsrResult(AsrHandleType.END, jsonNode.get("asr_recongize").asText(), data);
            }

            if (jsonNode.has("asr_recongize")) {
                return new AsrResult(AsrHandleType.PREFIX, jsonNode.get("asr_recongize").asText(), data);
            }
            // doesn't contain text
            if (!jsonNode.has("text")) {
                return new AsrResult(AsrHandleType.SKIP, data, data);
            }

            return new AsrResult(AsrHandleType.END, data, data);
        } catch (Exception e) {
            log.warn("handle error {}", data);
            // some case, asr only return partial json snippet
            return new AsrResult(AsrHandleType.DROPPED, data, data);
        }

    }


    public String enhance(String prefix, String lstRespStr, String deviceId) {
        JsonNode jsonNode;
        String lastLine;
        try {
            String[] lines = lstRespStr.split("\n");
            lastLine = lines[lines.length - 1];
            jsonNode = objectMapper.readTree(lastLine);
        } catch (Exception e) {
            log.warn("resp last {}", lstRespStr);
            log.error("error: ", e);
            // some case, asr only return partial json snippet
            return null;
        }

        try {
            String asrResult = prefix + jsonNode.get("asr_recongize").asText();
            ((ObjectNode) jsonNode).put("text", asrResult);
            R1IotUtils.JSON_RET.set(jsonNode);
            AiAssistant assistant = aiDirect.getAssistants().get(deviceId);

            String answer = assistant.chat(asrResult);

            JsonNode fixedJsonNode = R1IotUtils.JSON_RET.get();
            if (answer != null) {
                fixedJsonNode = R1IotUtils.sampleChatResp(answer);
            }

            String modifiedJson = lastLine;
            try {
                modifiedJson = objectMapper.writeValueAsString(fixedJsonNode);
            } catch (JsonProcessingException e) {
                log.warn("not json data {}", lstRespStr);
            }
            String newText = replaceLastLine(lstRespStr, modifiedJson);

            // Chuyển đổi phần thân phản hồi thành mảng byte (mã hóa UTF-8)
            byte[] responseBytes = modifiedJson.getBytes(StandardCharsets.UTF_8);

            // Tính toán độ dài byte
            int contentLength = responseBytes.length;
            // Thay thế trường Content-Length
            String newContentLength = "Content-Length: " + contentLength;
            return newText.replaceAll("Content-Length: \\d+", newContentLength);

        } catch (Exception e) {
            log.warn("resp last {}", lstRespStr);
            log.error("error: ", e);
            // some case, asr only return partial json snippet
            return null;
        } finally {
            R1IotUtils.remove();
        }


    }


    private static String replaceLastLine(String text, String newLastLine) {
        int lastNewlineIndex = text.lastIndexOf("\n");
        if (lastNewlineIndex == -1) {
            return newLastLine; // Nếu chỉ có một dòng, trả về trực tiếp nội dung mới
        }
        return text.substring(0, lastNewlineIndex + 1) + newLastLine;
    }
}
