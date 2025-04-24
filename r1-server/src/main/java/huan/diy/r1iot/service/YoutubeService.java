package huan.diy.r1iot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import huan.diy.r1iot.model.R1GlobalConfig;
import huan.diy.r1iot.util.R1IotUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;


@Service
@Slf4j
public class YoutubeService {


    @Autowired
    private R1GlobalConfig r1GlobalConfig;



    private static String COOKIE_FILE;

    static {
        String homeDir = System.getProperty("user.home");
        Path cookiePath = Paths.get(homeDir, ".r1-iot", "youtube.txt");
        COOKIE_FILE = Files.exists(cookiePath) ? cookiePath.toAbsolutePath().toString() : null;
    }


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // 使用Guava Cache构建缓存，30分钟过期
    private static final Cache<String, String> urlCache = CacheBuilder.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)  // 写入后6hours过期
            .maximumSize(1000)                       // 最大缓存1000个条目
            .build();

    public ObjectNode search(String keyword, String suffix) throws Exception {
        String searchQuery = (keyword + " " + suffix).trim();
        // add header
        HttpHeaders headers = new HttpHeaders();
        headers.add("accept-language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        // Updated to use fromUriString instead of fromHttpUrl
        String url = UriComponentsBuilder.fromUriString("https://www.youtube.com/results")
                .queryParam("search_query", searchQuery)
                .build()
                .toUriString();

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                String.class
        );
        String html = response.getBody();

        // 从HTML中提取JSON数据
        Pattern pattern = Pattern.compile("ytInitialData\\s*=\\s*(\\{.+?});");
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find()) {
            throw new RuntimeException("Failed to extract ytInitialData from YouTube response");
        }
        String jsonData = matcher.group(1);

        // Phân tích JSON
        JsonNode rootNode = objectMapper.readTree(jsonData);

        // Điều hướng đến nút danh sách video
        JsonNode contents = rootNode.path("contents")
                .path("twoColumnSearchResultsRenderer")
                .path("primaryContents")
                .path("sectionListRenderer")
                .path("contents")
                .get(0)  // Phần đầu tiên
                .path("itemSectionRenderer")
                .path("contents");

        ArrayNode musicInfo = objectMapper.createArrayNode();

        StreamSupport.stream(contents.spliterator(), false)
                .filter(item -> item.has("videoRenderer"))
                .forEach(item -> {

                    JsonNode renderer = item.path("videoRenderer");


                    ObjectNode music = objectMapper.createObjectNode();
                    String id = renderer.path("videoId").asText();
                    music.put("id", id);
                    // Tiêu đề video
                    music.put("title", renderer.path("title")
                            .path("runs")
                            .get(0)
                            .path("text")
                            .asText().replaceAll(R1IotUtils.SUPPORTED_CHARS, ""));

                    // 频道名称（优先从ownerText获取）
                    music.put("artist",  renderer.path("ownerText")
                            .path("runs")
                            .get(0)
                            .path("text")
                            .asText().replaceAll(R1IotUtils.SUPPORTED_CHARS, ""));


                    music.put("url", r1GlobalConfig.getHostIp() + "/audio/play/" + id + ".m4a");

                    musicInfo.add(music);
                });


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

    }


    public void streamAudio(String vId, String rangeHeader, HttpServletResponse response) throws Exception {
        // 1. 从缓存获取或通过 yt-dlp 获取实际音频URL
        String audioUrl = getCachedAudioUrl(vId);
        if (audioUrl == null) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to get audio URL");
            return;
        }

        // 2. 代理请求音频流
        proxyAudioRequest(audioUrl, rangeHeader, response);
    }

    private String getCachedAudioUrl(String videoId) {
        try {
            // Thử lấy từ bộ nhớ đệm, nếu không tồn tại thì gọi phương thức load để lấy
            return urlCache.get(videoId, () -> fetchAudioUrlWithYtDlp(videoId));
        } catch (Exception e) {
            log.error("Error getting audio URL for video: " + videoId, e);
            return null;
        }
    }

    private String fetchAudioUrlWithYtDlp(String videoId) throws IOException, InterruptedException {

        String remoteYtDlp = r1GlobalConfig.getYtdlpEndpoint();
        if (remoteYtDlp != null) {
            // Chạy ytDlp trên arm quá chậm, tìm một máy chủ amd để chuyển tiếp.
            return fetchFromRemote(remoteYtDlp, videoId);
        }

        // Xây dựng lệnh cơ bản
        List<String> command = new ArrayList<>();
        command.add("yt-dlp");

        // Thêm Cookie (khi không phải hệ điều hành Windows và tệp Cookie tồn tại)
        if (COOKIE_FILE != null) {
            command.add("--cookies");
            command.add(COOKIE_FILE);
        }

        // Thêm các tham số khác
        Collections.addAll(command,
                "-f", "140",      // Định dạng âm thanh
                "--no-warnings",
                "--get-url",      // Chỉ lấy URL không tải xuống
                "-4",            // Bắt buộc IPv4
                videoId          // Video ID hoặc URL
        );

        // Chuyển đổi thành mảng để thực thi
        String[] cmdArray = command.toArray(new String[0]);

        Process process = Runtime.getRuntime().exec(cmdArray);


        // Đọc đầu ra tiêu chuẩn
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String url = reader.readLine();

            // Đọc đầu ra lỗi
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    System.err.println("Error: " + errorLine);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || url == null || url.isEmpty()) {
                System.err.println("Exit code: " + exitCode);
                return null;
            }

            return url;
        }
    }

    private String fetchFromRemote(String remoteYtDlp, String vId) {
        StringBuilder sb = new StringBuilder();
        if (!remoteYtDlp.startsWith("http")) {
            sb.append("http://");
        }
        sb.append(remoteYtDlp);
        sb.append("?vId=");
        sb.append(vId);


        ResponseEntity<JsonNode> getResp = restTemplate.getForEntity(sb.toString(), JsonNode.class);
        return getResp.getBody().get("url").asText();
    }

    // 代理请求方法保持不变
    public void proxyAudioRequest(String audioUrl, String rangeHeader, HttpServletResponse response) {
        HttpClient client = null;
        InputStream remoteInputStream = null;

        try {
            log.debug("Starting audio proxy request for URL: {}", audioUrl);

            // 1. Tạo client HTTP và yêu cầu
            client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(audioUrl))
                    .GET()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
                    .header("Accept", "*/*");

            if (rangeHeader != null) {
                requestBuilder.header("Range", rangeHeader);
            }

            // 2. Gửi yêu cầu và nhận phản hồi
            HttpResponse<InputStream> remoteResponse = client.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            // 3. Thiết lập tiêu đề phản hồi
            response.setStatus(remoteResponse.statusCode());
            response.setHeader("Accept-Ranges", "bytes");

            remoteResponse.headers().firstValue("Content-Type")
                    .ifPresent(type -> response.setContentType(type));

            remoteResponse.headers().firstValue("Content-Length")
                    .ifPresent(len -> response.setHeader("Content-Length", len));

            remoteResponse.headers().firstValue("Content-Range")
                    .ifPresent(range -> response.setHeader("Content-Range", range));

            // 4. Truyền dữ liệu (sử dụng bộ đệm)
            remoteInputStream = remoteResponse.body();
            transferDataWithRetry(remoteInputStream, response.getOutputStream());

        } catch (Exception e) {
            log.error("Unexpected error during audio streaming", e);
            sendErrorIfNotCommitted(response, 500, "Unexpected error");
        } finally {
            // Đảm bảo đóng tài nguyên
            closeQuietly(remoteInputStream);
        }
    }

    private void transferDataWithRetry(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192]; // Bộ đệm 8KB
        int bytesRead;

        while ((bytesRead = in.read(buffer)) != -1) {
            try {
                out.write(buffer, 0, bytesRead);
                out.flush();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    private void sendErrorIfNotCommitted(HttpServletResponse response, int status, String message) {
        try {
            if (!response.isCommitted()) {
                response.sendError(status, message);
            }
        } catch (IOException e) {
            log.debug("Error sending error response", e);
        }
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                log.debug("Error closing resource", e);
            }
        }
    }

}
