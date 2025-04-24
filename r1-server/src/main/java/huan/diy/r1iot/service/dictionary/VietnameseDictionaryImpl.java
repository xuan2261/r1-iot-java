package huan.diy.r1iot.service.dictionary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service("vietnameseDictionary")
@Slf4j
public class VietnameseDictionaryImpl implements IDictionaryService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Từ điển cơ bản tiếng Việt
    private static final Map<String, String> BASIC_DICTIONARY = new HashMap<>();

    static {
        // Khởi tạo từ điển cơ bản
        BASIC_DICTIONARY.put("xin chào", "Lời chào hỏi thông thường trong tiếng Việt.\nVí dụ: Xin chào, tôi tên là Hùng.");
        BASIC_DICTIONARY.put("cảm ơn", "Lời nói để bày tỏ lòng biết ơn đối với người khác.\nVí dụ: Cảm ơn bạn đã giúp đỡ tôi.");
        BASIC_DICTIONARY.put("tạm biệt", "Lời chào khi chia tay.\nVí dụ: Tạm biệt, hẹn gặp lại.");
        BASIC_DICTIONARY.put("hạnh phúc", "Trạng thái cảm xúc tích cực, vui vẻ, mãn nguyện.\nVí dụ: Gia đình là nguồn hạnh phúc lớn nhất của tôi.");
        BASIC_DICTIONARY.put("yêu thương", "Tình cảm gắn bó, trân trọng dành cho người khác.\nVí dụ: Tình yêu thương giữa cha mẹ và con cái là vô điều kiện.");
        BASIC_DICTIONARY.put("tâm huyết", "Sự quan tâm, đầu tư công sức, tình cảm đặc biệt vào một việc gì đó.\nVí dụ: Anh ấy đã dồn hết tâm huyết vào dự án này.");
        BASIC_DICTIONARY.put("trí tuệ", "Khả năng tư duy, suy nghĩ, hiểu biết của con người.\nVí dụ: Trí tuệ nhân tạo đang phát triển nhanh chóng.");
        BASIC_DICTIONARY.put("thông minh", "Có khả năng hiểu biết, học hỏi và giải quyết vấn đề nhanh chóng.\nVí dụ: Cô ấy là một học sinh thông minh.");
        BASIC_DICTIONARY.put("sáng tạo", "Khả năng tạo ra những ý tưởng, sản phẩm mới mẻ, độc đáo.\nVí dụ: Sáng tạo là yếu tố quan trọng trong nghệ thuật.");
        BASIC_DICTIONARY.put("đổi mới", "Thay đổi, cải tiến để trở nên tốt hơn.\nVí dụ: Công ty cần đổi mới để phát triển.");
        BASIC_DICTIONARY.put("phát triển", "Quá trình tiến triển, mở rộng, trở nên tốt hơn.\nVí dụ: Việt Nam đang trong giai đoạn phát triển mạnh mẽ.");
        BASIC_DICTIONARY.put("tiến bộ", "Sự thay đổi theo hướng tích cực, tốt hơn.\nVí dụ: Học sinh đã có nhiều tiến bộ trong học tập.");
        BASIC_DICTIONARY.put("thành công", "Đạt được mục tiêu, kết quả tốt đẹp như mong muốn.\nVí dụ: Sự kiện đã thành công tốt đẹp.");
        BASIC_DICTIONARY.put("thất bại", "Không đạt được mục tiêu, kết quả như mong muốn.\nVí dụ: Thất bại là mẹ thành công.");
        BASIC_DICTIONARY.put("cố gắng", "Nỗ lực, dồn sức để làm việc gì đó.\nVí dụ: Hãy cố gắng hết sức mình.");
        BASIC_DICTIONARY.put("nỗ lực", "Sự cố gắng, chăm chỉ làm việc để đạt được mục tiêu.\nVí dụ: Nhờ nỗ lực không ngừng, anh ấy đã thành công.");
        BASIC_DICTIONARY.put("kiên trì", "Bền bỉ, không bỏ cuộc dù gặp khó khăn.\nVí dụ: Sự kiên trì sẽ giúp bạn vượt qua mọi thử thách.");
        BASIC_DICTIONARY.put("nhẫn nại", "Kiên nhẫn, chịu đựng, không nóng vội.\nVí dụ: Nhẫn nại là đức tính cần thiết khi dạy trẻ em.");
        BASIC_DICTIONARY.put("tự tin", "Tin tưởng vào khả năng của bản thân.\nVí dụ: Hãy tự tin khi trình bày ý kiến của mình.");
        BASIC_DICTIONARY.put("tự hào", "Cảm giác hãnh diện về bản thân hoặc điều gì đó.\nVí dụ: Tôi tự hào về đất nước Việt Nam.");
    }

    @Override
    public String lookup(String word) {
        try {
            // Tìm kiếm trong từ điển cơ bản
            String lowercaseWord = word.toLowerCase().trim();
            if (BASIC_DICTIONARY.containsKey(lowercaseWord)) {
                return formatBasicResult(lowercaseWord, BASIC_DICTIONARY.get(lowercaseWord));
            }

            // Tìm kiếm trên từ điển trực tuyến tracau.vn
            return lookupOnline(lowercaseWord);
        } catch (Exception e) {
            log.error("Error looking up word: " + word, e);
            return "Không tìm thấy từ \"" + word + "\" trong từ điển.";
        }
    }

    private String formatBasicResult(String word, String definition) {
        StringBuilder result = new StringBuilder();
        result.append("Từ: ").append(word).append("\n");

        String[] parts = definition.split("\\n");
        if (parts.length > 0) {
            result.append("Nghĩa: ").append(parts[0]).append("\n");
        }

        if (parts.length > 1) {
            result.append(parts[1]);
        }

        return result.toString();
    }

    private String lookupOnline(String word) {
        try {
            // Mã hóa từ cần tra cứu
            String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8);

            // Sử dụng từ điển trực tuyến tracau.vn
            String url = "https://tracau.vn/?s=" + encodedWord;

            // Lấy nội dung trang web
            String html = restTemplate.getForObject(url, String.class);

            // Parse HTML bằng Jsoup
            Document doc = Jsoup.parse(html);

            // Tìm phần tử chứa định nghĩa
            Elements meaningElements = doc.select(".dict-definition");

            if (!meaningElements.isEmpty()) {
                StringBuilder result = new StringBuilder();
                result.append("Từ: ").append(word).append("\n");

                // Lấy từ loại và nghĩa
                for (Element meaningElement : meaningElements) {
                    Element wordTypeElement = meaningElement.selectFirst(".dict-definition-type");
                    if (wordTypeElement != null) {
                        result.append("Từ loại: ").append(wordTypeElement.text().trim()).append("\n");
                    }

                    Elements definitionElements = meaningElement.select(".dict-definition-meaning");
                    if (!definitionElements.isEmpty()) {
                        result.append("Nghĩa:\n");
                        for (Element definitionElement : definitionElements) {
                            result.append("- ").append(definitionElement.text().trim()).append("\n");
                        }
                    }
                }

                // Lấy ví dụ
                Elements exampleElements = doc.select(".example-item");
                if (!exampleElements.isEmpty()) {
                    result.append("Ví dụ:\n");
                    for (int i = 0; i < Math.min(3, exampleElements.size()); i++) {
                        Element exampleElement = exampleElements.get(i);
                        result.append("- ").append(exampleElement.text().trim()).append("\n");
                    }
                }

                return result.toString();
            }

            // Nếu không tìm thấy trên tracau.vn, thử tìm trên tudienso.com
            return lookupOnTudienso(word);
        } catch (Exception e) {
            log.error("Error looking up word online: " + word, e);
            return lookupOnTudienso(word);
        }
    }

    private String lookupOnTudienso(String word) {
        try {
            // Mã hóa từ cần tra cứu
            String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8);

            // Sử dụng từ điển trực tuyến tudienso.com
            String url = "https://tudienso.com/tu-dien/viet-viet/" + encodedWord;

            // Lấy nội dung trang web
            String html = restTemplate.getForObject(url, String.class);

            // Parse HTML bằng Jsoup
            Document doc = Jsoup.parse(html);

            // Tìm phần tử chứa định nghĩa
            Element definitionElement = doc.selectFirst(".definition");

            if (definitionElement != null) {
                StringBuilder result = new StringBuilder();
                result.append("Từ: ").append(word).append("\n");

                // Lấy nghĩa
                result.append("Nghĩa: ").append(definitionElement.text().trim()).append("\n");

                return result.toString();
            }

            return "Không tìm thấy từ \"" + word + "\" trong từ điển.";
        } catch (Exception e) {
            log.error("Error looking up word on tudienso: " + word, e);
            return "Không tìm thấy từ \"" + word + "\" trong từ điển.";
        }
    }

    @Override
    public String getAlias() {
        return "Từ điển tiếng Việt";
    }
}
