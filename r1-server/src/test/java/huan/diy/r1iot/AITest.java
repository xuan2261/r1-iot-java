package huan.diy.r1iot;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

public class AITest {


    static class BoxDecision {

        @Tool("""
                Dùng để trả lời các câu hỏi chung của người dùng
                """)
        String questionAnswer(@P("Nội dung người dùng nhập") String userInput) {
            System.out.println("Called questionAnswer with userInput=" + userInput);
            return userInput;
        }

        @Tool("""
                Dùng để xử lý yêu cầu phát nhạc
                """)
        void playMusic(@P(value = "Tác giả bài hát, có thể để trống", required = false) String author,
                       @P(value = "Tên bài hát, có thể để trống", required = false) String songName,
                       @P(value = "Từ khóa tìm kiếm bài hát, có thể để trống", required = false) String keyword) {
            System.out.println("Called playMusic with author=" + author + ", songName=" + songName + ", keyword=" + keyword);
        }

        @Tool("""
                Cài đặt chung cho loa: đèn nền, âm lượng, dừng, ngủ v.v.
                """)
        void voiceBoxSetting(@P("Nội dung người dùng nhập") String userInput) {
            System.out.println("Called voiceBoxSetting with userInput=" + userInput);
        }

        @Tool("""
                Điều khiển nhà thông minh, như bật đèn, bình nước nóng, điều hòa, điều chỉnh nhiệt độ, kiểm tra độ ẩm, v.v.
                """)
        void homeassistant(String actionCommand) {
            System.out.println("Called homeassistant with  actionCommand=" + actionCommand );
        }

        @Tool("""
                Dùng để phát tin tức, như thể thao, tài chính, công nghệ, giải trí v.v.
                """)
        void playNews(@P("Nội dung người dùng nhập") String userInput) {
            System.out.println("Called playNews with userInput=" + userInput);
        }

        @Tool("""
                Dùng để phát truyện, radio v.v.
                """)
        void playAudio(@P("Từ khóa") String keyword) {
            System.out.println("Called playAudio with userInput=" + keyword);
        }


    }

    interface Assistant {

        String chat(String userMessage);
    }

    @Test
    public void mainTest() {

        Function<Object, String> systemMessageProvider = (context) -> {
            return """
                Bạn là một trợ lý loa thông minh, chịu trách nhiệm xử lý các lệnh bằng giọng nói của người dùng.

                Lưu ý:
                Mỗi lần bạn chỉ nên sử dụng một chức năng, không sử dụng nhiều chức năng cùng lúc.
                """;
        };

        ChatLanguageModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.x.ai/v1")
                .apiKey("xai-MAPslh")
                .modelName("grok-2-latest")
                .strictTools(false)
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)

                .tools(new BoxDecision())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .systemMessageProvider(systemMessageProvider) // 使用 Provider 方式设置 SystemMessage

                .build();

        String question = "小白兔白又白是个啥？";

        String answer = assistant.chat(question);
         answer = assistant.chat("播放它");

        System.out.println(answer);
        // The square root of the sum of the number of letters in the words "hello" and "world" is approximately 3.162.
    }


}
