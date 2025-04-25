package huan.diy.r1iot.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service("Llama3")
@Slf4j
public class Llama3AI implements IAIService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String API_URL = "https://llama3-api.meta.ai/v1/chat/completions";
    private static final String MODEL = "meta-llama/Llama-3-8b-chat";

    @Override
    public ChatLanguageModel buildModel(Device device) {
        return OpenAiChatModel.builder()
                .baseUrl(API_URL)
                .apiKey(device.getAiConfig().getKey())
                .modelName(MODEL)
                .temperature(0.7)
                .maxTokens(800)
                .build();
    }

    @Override
    public String getAlias() {
        return "Meta Llama 3";
    }

    public String chat(String prompt, Device device) {
        try {
            // Chuẩn bị header
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + device.getAiConfig().getKey());

            // Chuẩn bị messages
            List<Message> messages = new ArrayList<>();

            // Thêm system prompt nếu có
            if (device.getAiConfig().getSystemPrompt() != null && !device.getAiConfig().getSystemPrompt().isEmpty()) {
                messages.add(new Message("system", device.getAiConfig().getSystemPrompt()));
            }

            // Thêm user message
            messages.add(new Message("user", prompt));

            // Tạo request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", MODEL);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 800);

            ArrayNode messagesNode = requestBody.putArray("messages");
            for (Message message : messages) {
                ObjectNode messageNode = messagesNode.addObject();
                messageNode.put("role", message.getRole());
                messageNode.put("content", message.getContent());
            }

            // Gửi request
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(API_URL, request, JsonNode.class);

            // Xử lý response
            JsonNode responseBody = response.getBody();
            if (responseBody != null && responseBody.has("choices") && responseBody.get("choices").size() > 0) {
                JsonNode choice = responseBody.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    return choice.get("message").get("content").asText();
                }
            }

            return "Không nhận được phản hồi từ Llama 3";
        } catch (Exception e) {
            log.error("Lỗi khi gọi API Llama 3: " + e.getMessage(), e);
            return "Đã xảy ra lỗi khi xử lý yêu cầu: " + e.getMessage();
        }
    }
}
