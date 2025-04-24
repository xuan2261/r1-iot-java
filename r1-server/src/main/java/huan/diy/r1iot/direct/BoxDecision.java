package huan.diy.r1iot.direct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import huan.diy.r1iot.model.Device;
import huan.diy.r1iot.model.MusicAiResp;
import huan.diy.r1iot.service.audio.IAudioService;
import huan.diy.r1iot.service.box.BoxControllerService;
import huan.diy.r1iot.service.dictionary.IDictionaryService;
import huan.diy.r1iot.service.news.INewsService;
import huan.diy.r1iot.service.hass.HassServiceImpl;
import huan.diy.r1iot.service.music.IMusicService;
import huan.diy.r1iot.service.radio.IRadioService;
import huan.diy.r1iot.service.weather.IWeatherService;
import huan.diy.r1iot.util.R1IotUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Data
public class BoxDecision {


    private static final ObjectMapper objectMapper;

    private static final ObjectNode intent;

    static {
        objectMapper = R1IotUtils.getObjectMapper();
        intent = objectMapper.createObjectNode();

        ObjectNode operationsObj = objectMapper.createObjectNode();
        ArrayNode operations = objectMapper.createArrayNode();
        operationsObj.set("operations", operations);
        intent.set("intent", operationsObj);
        ObjectNode operator = objectMapper.createObjectNode();
        operations.add(operator);

        operator.put("operator", "ACT_PLAY");

    }

    public BoxDecision(Device device,
                       Map<String, IMusicService> musicServiceMap,
                       Map<String, INewsService> newsServiceMap,
                       Map<String, IAudioService> audioServiceMap,
                       Map<String, IWeatherService> weatherServiceMap,
                       HassServiceImpl iotService,
                       BoxControllerService boxControllerService,
                       IRadioService radioService,
                       IDictionaryService dictionaryService) {
        this.device = device;
        this.musicServiceMap = musicServiceMap;
        this.newsServiceMap = newsServiceMap;
        this.audioServiceMap = audioServiceMap;
        this.weatherServiceMap = weatherServiceMap;
        this.iotService = iotService;
        this.boxControllerService = boxControllerService;
        this.radioService = radioService;
        this.dictionaryService = dictionaryService;
    }

    private Device device;
    private Map<String, IMusicService> musicServiceMap;
    private Map<String, INewsService> newsServiceMap;
    private Map<String, IAudioService> audioServiceMap;
    private Map<String, IWeatherService> weatherServiceMap;
    private HassServiceImpl iotService;
    private BoxControllerService boxControllerService;
    private IRadioService radioService;
    private IDictionaryService dictionaryService;

    @Tool("""
            Dùng để xử lý yêu cầu phát nhạc, như nhạc pop, nhạc thiếu nhi, v.v.
            Ví dụ: Tôi muốn nghe nhạc của Sơn Tùng, phát bài Hãy trao cho anh
            Sử dụng công cụ này khi người dùng muốn nghe nhạc hoặc bài hát cụ thể.
            """)
    void playMusic(@P(value = "Tác giả bài hát, có thể để trống", required = false) String author,
                   @P(value = "Tên bài hát, có thể để trống", required = false) String songName,
                   @P(value = "Từ khóa tìm kiếm bài hát, có thể để trống", required = false) String keyword) {
        log.info("author: {}, songName: {}, keyword: {}", author, songName, keyword);
        JsonNode musicResp = musicServiceMap.get(device.getMusicConfig().getChoice()).fetchMusics(new MusicAiResp(author, songName, keyword), device);
        JsonNode jsonNode = R1IotUtils.JSON_RET.get();
        ObjectNode ret = ((ObjectNode) jsonNode);
        ret.set("data", musicResp);
        ret.set("semantic", intent);
        ret.put("code", "SETTING_EXEC");
        ret.put("matchType", "FUZZY");

        ObjectNode general = objectMapper.createObjectNode();
        general.put("text", "Tốt, đang phát cho bạn");
        general.put("type", "T");
        ret.set("general", general);
        ret.put("service", "cn.yunzhisheng.music");


        ret.remove("taskName");
        R1IotUtils.JSON_RET.set(ret);
    }

    @Tool("""
            Cài đặt chung cho loa: chuyển đèn nền, âm lượng, dừng, ngủ, chế độ phát (lặp lại một bài, phát tuần tự) v.v.
            Sử dụng công cụ này khi người dùng muốn điều chỉnh cài đặt của loa như âm lượng, đèn nền, hoặc dừng phát.
            """)
    void voiceBoxSetting(@P(value = "Đối tượng điều khiển: đèn nền(lamp), tiến nhanh(faster), lùi(slower), nhảy đến thời điểm(jump), trả lời bằng tiếng Anh", required = false) String target,
                         @P(value = "Hành động thực hiện, ví dụ bật(on), tắt(off), chuyển hiệu ứng(change), thời gian (giá trị số, cần chuyển sang giây), trả lời bằng tiếng Anh", required = false) String action) {
        log.info("target: {}, action: {}", target, action);
        if (!StringUtils.hasLength(R1IotUtils.CLIENT_IP.get())) {
            return;
        }

        if (!StringUtils.hasLength(target)) {
            return;
        }

        boolean handle = boxControllerService.control(R1IotUtils.CLIENT_IP.get(), target, action);
        if (handle) {
            R1IotUtils.JSON_RET.set(R1IotUtils.sampleChatResp("Đã thực hiện thành công"));
        }
    }

    @Tool("""
            Điều khiển nhà thông minh, như bật đèn, bình nước nóng, điều hòa, điều chỉnh nhiệt độ, kiểm tra độ ẩm, v.v.
            Ví dụ: Điều chỉnh nhiệt độ điều hòa phòng khách lên 23 độ
            AI: target=điều hòa phòng khách parameter
            Sử dụng công cụ này khi người dùng muốn điều khiển các thiết bị nhà thông minh.
            """)
    String homeassistant(@P(value = "Đối tượng điều khiển: điều hòa phòng ngủ, bình nước nóng. Trả lời bằng tiếng Việt") String target,
                         @P(value = "Thuộc tính: nhiệt độ(temperature), tốc độ quạt. Trả lời bằng tiếng Anh", required = false) String parameter,
                         @P(value = "Hành động hoặc giá trị: bật(on), tắt(off), 23, không cần đơn vị.") String actValue) {
        log.info("target: {}, parameter: {}, actValue: {}", target, parameter, actValue);

        String tts = iotService.controlHass(target, parameter, actValue, device);
        if (tts.isEmpty()) {
            return "Điều khiển thất bại";
        }
        R1IotUtils.JSON_RET.set(R1IotUtils.sampleChatResp(tts));

        return tts;

    }


    @Tool("""
            Dùng để phát tin tức.
            Ví dụ: Phát tin tức, cho tôi nghe tin mới nhất, tin thời sự hôm nay
            Sử dụng công cụ này khi người dùng muốn nghe tin tức.
            """)
    void playNews(@P("Nội dung người dùng nhập") String userInput) {

        INewsService newsService = newsServiceMap.getOrDefault(device.getNewsConfig().getChoice(), newsServiceMap.get("chinaSound"));
        JsonNode musicResp = newsService.fetchNews(userInput, device);
        JsonNode jsonNode = R1IotUtils.JSON_RET.get();
        ObjectNode ret = ((ObjectNode) jsonNode);
        ret.set("data", musicResp);
        ret.set("semantic", intent);
        ret.put("code", "SETTING_EXEC");
        ret.put("matchType", "FUZZY");

        ObjectNode general = objectMapper.createObjectNode();
        general.put("text", "Tốt, đang phát cho bạn");
        general.put("type", "T");
        ret.set("general", general);
        ret.put("service", "cn.yunzhisheng.music");


        ret.remove("taskName");
        R1IotUtils.JSON_RET.set(ret);

        log.info("Called playNews with userInput={}", userInput);
    }

    @Tool("""
            Dùng để phát truyện, video, sách nói, v.v.
            Ví dụ: Tôi muốn xem Tam Thân, phát sách nói Tam Thân
            Sử dụng công cụ này khi người dùng muốn nghe sách nói hoặc truyện audio.
            """)
    void playAudio(@P("Từ khóa") String keyword, @P(value = "Hành động, có phải là xem không?", required = false) boolean look) {

        log.info("Called playAudio with keyword={}", keyword);
        JsonNode musicResp = audioServiceMap.get(device.getAudioConfig().getChoice()).search(keyword, look, device);
        JsonNode jsonNode = R1IotUtils.JSON_RET.get();
        ObjectNode ret = ((ObjectNode) jsonNode);
        ret.set("data", musicResp);
        ret.set("semantic", intent);
        ret.put("code", "SETTING_EXEC");
        ret.put("matchType", "FUZZY");

        ObjectNode general = objectMapper.createObjectNode();
        general.put("text", "Tốt, đang phát cho bạn");
        general.put("type", "T");
        ret.set("general", general);
        ret.put("service", "cn.yunzhisheng.music");


        ret.remove("taskName");
        R1IotUtils.JSON_RET.set(ret);

    }


    @Tool("""
            Dùng để phát radio
            Ví dụ: Tôi muốn nghe đài phát thanh VOV, mở đài VOV giao thông Hà Nội
            Sử dụng công cụ này khi người dùng muốn nghe radio hoặc đài phát thanh.
            """)
    void playRadio(@P("Tên đài phát thanh") String radioName, @P(value = "Tỉnh/thành phố", required = false) String province) {


        log.info("Called playAudio with radioName={}, province={}", radioName, province == null ? "" : province);

        JsonNode ret = radioService.fetchRadio(radioName, province, device);


        R1IotUtils.JSON_RET.set(ret);
    }


    @Tool("""
            Dùng để tra cứu thời tiết, có vị trí mặc định, có thể không cần nói tên thành phố
            Ví dụ: Thời tiết Hà Nội ngày kia thế nào, mai trời có mưa không
            AI: locationName=Hà Nội offsetDay=2
            Sử dụng công cụ này khi người dùng hỏi về thời tiết.
            """)
    void queryWeather(@P(value = "Tên địa điểm", required = false) String locationName, @P(value = "Số ngày tính từ hiện tại", required = false) int offsetDay) {

        log.info("Called queryWeather with locationName={}, offsetDay={}", locationName, offsetDay);

        String ret = weatherServiceMap.get(device.getWeatherConfig().getChoice()).getWeather(locationName, offsetDay, device);
        R1IotUtils.JSON_RET.set(R1IotUtils.sampleChatResp(ret));

    }

    @Tool("""
            Dùng để tra cứu từ điển tiếng Việt
            Ví dụ: Tra cứu từ "hạnh phúc", ý nghĩa của từ "tâm huyết" là gì
            Sử dụng công cụ này khi người dùng muốn tìm hiểu nghĩa của một từ hoặc cụm từ tiếng Việt.
            """)
    String lookupDictionary(@P("Từ cần tra cứu") String word) {

        log.info("Called lookupDictionary with word={}", word);

        String result = dictionaryService.lookup(word);
        R1IotUtils.JSON_RET.set(R1IotUtils.sampleChatResp(result));

        return result;
    }

}