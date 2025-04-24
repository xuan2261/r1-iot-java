package huan.diy.r1iot.service.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import huan.diy.r1iot.model.Device;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service("Claude")
@Slf4j
public class ClaudeAI implements IAIService {
    private static final String BASE_URL = "https://api.anthropic.com/v1";
    private static final String MODEL = "claude-3-opus-20240229";

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public ChatLanguageModel buildModel(Device device) {
        return OpenAiChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(device.getAiConfig().getKey())
                .modelName(MODEL)
                .strictTools(false)
                .build();
    }

    @Override
    public String getAlias() {
        return "Claude AI (Hỗ trợ tiếng Việt tốt)";
    }
}
