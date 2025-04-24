package huan.diy.r1iot.direct;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import huan.diy.r1iot.util.R1IotUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dev.langchain4j.data.message.UserMessage.userMessage;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class AiAssistant {

    private ChatLanguageModel openAiModel;
    private String systemPrompt;
    private BoxDecision boxDecision;
    private ChatMemory chatMemory;


    public String chat(String text) {
        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(boxDecision);
        List<ChatMessage> chatMessages = chatMemory.messages();
        UserMessage userMessage = userMessage(text);
        chatMessages.add(userMessage);

        List<ChatMessage> reqMessages = new ArrayList<>();
        reqMessages.add(new SystemMessage(systemPrompt + """

                Lưu ý:
                Bạn là một trợ lý thông minh bằng tiếng Việt, có thể trả lời các câu hỏi của người dùng!
                Bạn nên trả lời ngắn gọn, rõ ràng và hữu ích.
                Khi người dùng yêu cầu phát nhạc, tin tức, radio hoặc sách nói, hãy sử dụng công cụ tương ứng.
                Khi người dùng hỏi về thời tiết, hãy sử dụng công cụ queryWeather.
                Khi người dùng muốn điều khiển thiết bị nhà thông minh, hãy sử dụng công cụ homeassistant.
                Khi người dùng muốn tra cứu nghĩa của một từ hoặc cụm từ tiếng Việt, hãy sử dụng công cụ lookupDictionary.
                """));
        reqMessages.addAll(chatMessages);
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(reqMessages)
                .parameters(ChatRequestParameters.builder()
                        .toolSpecifications(toolSpecifications)
                        .build())
                .build();

        ChatResponse chatResponse = openAiModel.chat(chatRequest);
        AiMessage aiMessage = chatResponse.aiMessage();

        List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();

        if (CollectionUtils.isEmpty(toolExecutionRequests)) {
            chatMessages.add(aiMessage);
            return aiMessage.text();
        }
        var toolExecutionRequest = toolExecutionRequests.get(0);
        ToolExecutor toolExecutor = new DefaultToolExecutor(boxDecision, toolExecutionRequest);
        String result = toolExecutor.execute(toolExecutionRequest, UUID.randomUUID().toString());
        ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage.from(toolExecutionRequest, result);

        log.info(toolExecutionResultMessage.toString());
        chatMessages.add(toolExecutionResultMessage);

        return null;

    }

}
