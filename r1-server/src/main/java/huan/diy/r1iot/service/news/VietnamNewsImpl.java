package huan.diy.r1iot.service.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.service.IWebAlias;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service("vietnamNews")
@Slf4j
public class VietnamNewsImpl implements INewsService, IWebAlias {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getAlias() {
        return "VOV Tin tức";
    }

    @Override
    public JsonNode fetchNews(String userInput, Device device) {
        // URL của VOV - Đài Tiếng nói Việt Nam
        String url = "https://vov.vn/tin-moi-nhat.rss";
        
        try {
            // Lấy nội dung RSS
            String rssContent = restTemplate.getForObject(url, String.class);
            
            // Parse RSS bằng Jsoup
            Document doc = Jsoup.parse(rssContent);
            Elements items = doc.select("item");
            
            // Tạo danh sách các URL audio
            List<String> audioUrls = new ArrayList<>();
            
            // Tạo JSON response
            ArrayNode musicInfo = objectMapper.createArrayNode();
            int index = 0;
            
            for (Element item : items) {
                try {
                    String title = item.select("title").text();
                    String description = item.select("description").text();
                    String link = item.select("link").text();
                    
                    // Lấy nội dung trang tin để tìm audio (nếu có)
                    String pageContent = restTemplate.getForObject(link, String.class);
                    Document pageDoc = Jsoup.parse(pageContent);
                    Elements audioElements = pageDoc.select("audio source");
                    
                    if (!audioElements.isEmpty()) {
                        String audioUrl = audioElements.first().attr("src");
                        
                        ObjectNode music = objectMapper.createObjectNode();
                        music.put("id", index++);
                        music.put("title", title);
                        music.put("artist", "VOV");
                        music.put("url", audioUrl);
                        musicInfo.add(music);
                    }
                    
                    // Giới hạn số lượng tin tức
                    if (index >= 10) {
                        break;
                    }
                } catch (Exception e) {
                    log.error("Error processing news item", e);
                }
            }
            
            // Nếu không tìm thấy audio, sử dụng URL mặc định
            if (musicInfo.size() == 0) {
                // URL mặc định cho VOV
                String defaultAudioUrl = "https://media.vov.vn/sites/default/files/2023-12/vov1-12h_29.mp3";
                
                ObjectNode music = objectMapper.createObjectNode();
                music.put("id", 0);
                music.put("title", "Bản tin VOV");
                music.put("artist", "VOV");
                music.put("url", defaultAudioUrl);
                musicInfo.add(music);
            }
            
            // Tạo response
            ObjectNode result = objectMapper.createObjectNode();
            ObjectNode ret = objectMapper.createObjectNode();
            
            ret.put("count", musicInfo.size());
            ret.set("musicinfo", musicInfo);
            ret.put("pagesize", String.valueOf(musicInfo.size()));
            ret.put("errorCode", 0);
            ret.put("page", "1");
            ret.put("source", 1);
            
            result.set("result", ret);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error fetching Vietnam news", e);
            
            // Tạo response lỗi
            ObjectNode result = objectMapper.createObjectNode();
            ObjectNode ret = objectMapper.createObjectNode();
            
            ArrayNode musicInfo = objectMapper.createArrayNode();
            ObjectNode music = objectMapper.createObjectNode();
            music.put("id", 0);
            music.put("title", "Không thể tải tin tức");
            music.put("artist", "VOV");
            music.put("url", "https://media.vov.vn/sites/default/files/2023-12/vov1-12h_29.mp3");
            musicInfo.add(music);
            
            ret.put("count", 1);
            ret.set("musicinfo", musicInfo);
            ret.put("pagesize", "1");
            ret.put("errorCode", 0);
            ret.put("page", "1");
            ret.put("source", 1);
            
            result.set("result", ret);
            
            return result;
        }
    }
}
