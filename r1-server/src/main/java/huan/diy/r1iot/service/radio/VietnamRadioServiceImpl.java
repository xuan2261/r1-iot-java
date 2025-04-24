package huan.diy.r1iot.service.radio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import huan.diy.r1iot.model.Channel;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.R1GlobalConfig;
import huan.diy.r1iot.util.R1IotUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("vietnamRadio")
@Slf4j
public class VietnamRadioServiceImpl implements IRadioService {

    private static final Cache<String, String> urlCache = CacheBuilder.newBuilder()
            .expireAfterWrite(25, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    private List<Channel> vietnamRadios = new ArrayList<>();

    @Autowired
    private R1GlobalConfig globalConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            // Đọc file vietnam-radio.m3u từ resources
            InputStream is = new ClassPathResource("vietnam-radio.m3u").getInputStream();
            String content = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .reduce("", (a, b) -> a + "\n" + b);
            
            vietnamRadios = parseM3UContent(content);
            log.info("Loaded {} Vietnam radio channels", vietnamRadios.size());
        } catch (Exception e) {
            log.error("Failed to load Vietnam radio channels", e);
        }
    }

    @Override
    public JsonNode fetchRadio(String radioName, String province, Device device) {
        Channel mostSimilarChannel = null;
        int minDistance = Integer.MAX_VALUE;

        LevenshteinDistance levenshtein = new LevenshteinDistance();

        for (Channel channel : vietnamRadios) {
            String tvgName = channel.groupTitle() + " " + channel.tvgName();

            // Tính khoảng cách Levenshtein (càng nhỏ càng giống)
            int distance = levenshtein.apply((province != null ? province + " " : "") + radioName, tvgName);

            if (distance < minDistance) {
                minDistance = distance;
                mostSimilarChannel = channel;
            }
        }

        if (mostSimilarChannel == null) {
            // Nếu không tìm thấy kênh nào, sử dụng kênh mặc định (VOV1)
            for (Channel channel : vietnamRadios) {
                if (channel.tvgName().contains("VOV1")) {
                    mostSimilarChannel = channel;
                    break;
                }
            }
            
            // Nếu vẫn không tìm thấy, sử dụng kênh đầu tiên
            if (mostSimilarChannel == null && !vietnamRadios.isEmpty()) {
                mostSimilarChannel = vietnamRadios.get(0);
            }
        }

        if (mostSimilarChannel != null) {
            String url = mostSimilarChannel.url();
            if (url.contains("m3u8")) {
                return R1IotUtils.streamRespSample(url);
            } else {
                String link = globalConfig.getHostIp() + "/stream/radio/" +
                        Base64.getUrlEncoder().withoutPadding().encodeToString(url.getBytes(StandardCharsets.UTF_8))
                        + ".m3u8";
                return R1IotUtils.streamRespSample(link);
            }
        }

        // Nếu không tìm thấy kênh nào, trả về lỗi
        return R1IotUtils.sampleChatResp("Không tìm thấy kênh radio phù hợp");
    }

    @Override
    public void streamRadio(String resourceUrl, HttpServletResponse response) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(resourceUrl);
            String url = new String(decodedBytes, StandardCharsets.UTF_8);

            String m3u8Url = urlCache.get(url, () -> get302Url(url));

            // Chuyển hướng đến URL thực
            response.sendRedirect(m3u8Url);
        } catch (Exception e) {
            log.error("Error streaming radio", e);
        }
    }

    private String get302Url(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 302) {
                return response.headers().firstValue("Location").orElse(url);
            }
            return url;
        } catch (Exception e) {
            log.error("Error getting 302 URL", e);
            return url;
        }
    }

    private List<Channel> parseM3UContent(String content) {
        List<Channel> channels = new ArrayList<>();
        
        Pattern extinf = Pattern.compile("#EXTINF:-1\\s+tvg-id=\"([^\"]*)\"\\s+tvg-name=\"([^\"]*)\"\\s+tvg-logo=\"([^\"]*)\"\\s+group-title=\"([^\"]*)\",(.*)");
        
        String[] lines = content.split("\n");
        String currentTvgId = "";
        String currentTvgName = "";
        String currentTvgLogo = "";
        String currentGroupTitle = "";
        String currentName = "";
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#EXTINF")) {
                Matcher matcher = extinf.matcher(line);
                if (matcher.find()) {
                    currentTvgId = matcher.group(1);
                    currentTvgName = matcher.group(2);
                    currentTvgLogo = matcher.group(3);
                    currentGroupTitle = matcher.group(4);
                    currentName = matcher.group(5);
                }
            } else if (!line.isEmpty() && !line.startsWith("#")) {
                channels.add(new Channel(currentTvgId, currentTvgName, currentTvgLogo, currentGroupTitle, currentName, line));
            }
        }
        
        return channels;
    }
}
