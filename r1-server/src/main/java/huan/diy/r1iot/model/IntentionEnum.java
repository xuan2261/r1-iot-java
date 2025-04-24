package huan.diy.r1iot.model;

public enum IntentionEnum {
    CHAT("chat", "cn.yunzhisheng.chat", "Trò chuyện"),
    SMART_HOME("smart home", "cn.yunzhisheng.setting", "Nhà thông minh"),
    MUSIC("music", "cn.yunzhisheng.music", "Phát nhạc"),
    NEWS("news", "cn.yunzhisheng.news", "Phát tin tức");
    private String name;
    private String serviceName;
    private String description;

    IntentionEnum(String name, String serviceName, String description) {
        this.name = name;
        this.serviceName = serviceName;
        this.description = description;
    }


}
