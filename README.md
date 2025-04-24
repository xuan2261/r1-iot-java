# R1-IOT-Java - Loa thông minh hỗ trợ tiếng Việt

<p align="center">
  <img src="https://github.com/xuan2261/r1-iot-java/raw/master/docs/images/logo.png" alt="R1-IOT-Java Logo" width="200"/>
</p>

<p align="center">
  <a href="https://github.com/xuan2261/r1-iot-java/actions/workflows/docker-publish.yml">
    <img src="https://github.com/xuan2261/r1-iot-java/actions/workflows/docker-publish.yml/badge.svg" alt="Docker Build Status"/>
  </a>
  <a href="https://github.com/xuan2261/r1-iot-java/releases">
    <img src="https://img.shields.io/github/v/release/xuan2261/r1-iot-java" alt="Latest Release"/>
  </a>
  <a href="https://github.com/xuan2261/r1-iot-java/blob/master/LICENSE">
    <img src="https://img.shields.io/github/license/xuan2261/r1-iot-java" alt="License"/>
  </a>
</p>

> [English](#english) | [Tiếng Việt](#tiếng-việt)

## Tiếng Việt

R1-IOT-Java là một dự án mã nguồn mở giúp biến loa PHICOMM R1 thành một loa thông minh đa chức năng với hỗ trợ đầy đủ cho tiếng Việt. Dự án này là một fork từ dự án gốc với các tính năng bổ sung để hỗ trợ người dùng Việt Nam.

### Tính năng

- 🎵 **Phát nhạc**: Tìm kiếm và phát nhạc từ nhiều nguồn
- 📰 **Tin tức**: Đọc tin tức tiếng Việt từ các nguồn uy tín
- 📻 **Radio**: Nghe các đài phát thanh Việt Nam
- 📚 **Từ điển**: Tra cứu từ điển tiếng Việt
- 🤖 **AI thông minh**: Tích hợp với Claude AI để trả lời câu hỏi bằng tiếng Việt
- 🏠 **Điều khiển nhà thông minh**: Tích hợp với Home Assistant
- 🌐 **Giao diện web**: Quản lý và cấu hình dễ dàng

### Cài đặt

#### Sử dụng Docker (Khuyến nghị)

```bash
# Pull image từ GitHub Container Registry
docker pull ghcr.io/xuan2261/r1-iot-java:latest

# Chạy container
docker run -d --name r1-iot-java \
  -p 8080:8080 \
  -v ~/.r1iot:/root/.r1iot \
  --restart=always \
  ghcr.io/xuan2261/r1-iot-java:latest
```

#### Cài đặt thủ công

1. Cài đặt các yêu cầu:
   - Java 17+
   - Maven
   - Node.js và npm
   - yt-dlp

2. Clone repository:
   ```bash
   git clone https://github.com/xuan2261/r1-iot-java.git
   cd r1-iot-java
   ```

3. Build dự án:
   ```bash
   # Build server
   cd r1-server
   mvn clean package -DskipTests

   # Build web
   cd ../r1-web
   npm install
   npm run build
   ```

4. Chạy ứng dụng:
   ```bash
   cd ../r1-server
   java -jar target/r1-server-0.0.1-SNAPSHOT.jar
   ```

### Cấu hình

1. Truy cập giao diện web tại `http://localhost:8080`
2. Cấu hình máy chủ:
   - Nhập địa chỉ IP của máy chủ
   - Cấu hình yt-dlp endpoint (nếu cần)
3. Thêm thiết bị loa R1:
   - Nói "Xiao Xun Xiao Xun" để kích hoạt loa
   - Làm mới trang để lấy ID thiết bị
   - Cấu hình AI, tin tức, nhạc, v.v.

### Sử dụng

#### Tra cứu từ điển

- "Tra cứu từ 'hạnh phúc'"
- "Ý nghĩa của từ 'tâm huyết' là gì?"

#### Nghe tin tức

- "Phát tin tức"
- "Cho tôi nghe tin mới nhất"

#### Nghe radio

- "Tôi muốn nghe đài VOV"
- "Mở đài VOV giao thông Hà Nội"

#### Phát nhạc

- "Phát bài hát 'Hãy trao cho anh'"
- "Tôi muốn nghe nhạc của Sơn Tùng"

#### Điều khiển loa

- "Tăng âm lượng"
- "Giảm âm lượng"
- "Bật đèn nền"
- "Dừng phát"

### Đóng góp

Chúng tôi rất hoan nghênh mọi đóng góp! Hãy tạo issue hoặc pull request trên GitHub.

---

## English

R1-IOT-Java is an open-source project that transforms the PHICOMM R1 speaker into a versatile smart speaker with full Vietnamese language support. This project is a fork of the original project with additional features to support Vietnamese users.

### Features

- 🎵 **Music Playback**: Search and play music from various sources
- 📰 **News**: Read Vietnamese news from reliable sources
- 📻 **Radio**: Listen to Vietnamese radio stations
- 📚 **Dictionary**: Look up Vietnamese words and phrases
- 🤖 **Smart AI**: Integration with Claude AI to answer questions in Vietnamese
- 🏠 **Smart Home Control**: Integration with Home Assistant
- 🌐 **Web Interface**: Easy management and configuration

### Installation

#### Using Docker (Recommended)

```bash
# Pull image from GitHub Container Registry
docker pull ghcr.io/xuan2261/r1-iot-java:latest

# Run container
docker run -d --name r1-iot-java \
  -p 8080:8080 \
  -v ~/.r1iot:/root/.r1iot \
  --restart=always \
  ghcr.io/xuan2261/r1-iot-java:latest
```

#### Manual Installation

1. Install requirements:
   - Java 17+
   - Maven
   - Node.js and npm
   - yt-dlp

2. Clone repository:
   ```bash
   git clone https://github.com/xuan2261/r1-iot-java.git
   cd r1-iot-java
   ```

3. Build project:
   ```bash
   # Build server
   cd r1-server
   mvn clean package -DskipTests

   # Build web
   cd ../r1-web
   npm install
   npm run build
   ```

4. Run application:
   ```bash
   cd ../r1-server
   java -jar target/r1-server-0.0.1-SNAPSHOT.jar
   ```

### Configuration

1. Access the web interface at `http://localhost:8080`
2. Configure server:
   - Enter server IP address
   - Configure yt-dlp endpoint (if needed)
3. Add R1 speaker device:
   - Say "Xiao Xun Xiao Xun" to activate the speaker
   - Refresh the page to get the device ID
   - Configure AI, news, music, etc.

### Usage

#### Dictionary Lookup

- "Look up the word 'happiness'"
- "What is the meaning of 'dedication'?"

#### Listen to News

- "Play news"
- "Let me hear the latest news"

#### Listen to Radio

- "I want to listen to VOV radio"
- "Open VOV traffic radio Hanoi"

#### Play Music

- "Play the song 'Give It To Me'"
- "I want to listen to Son Tung's music"

#### Control Speaker

- "Increase volume"
- "Decrease volume"
- "Turn on backlight"
- "Stop playback"

### Contributing

Contributions are welcome! Please create an issue or pull request on GitHub.

---

## Acknowledgements

- Original project: [ring1012/r1-iot-java](https://github.com/ring1012/r1-iot-java)
- [Claude AI](https://www.anthropic.com/claude) for Vietnamese language support
- All contributors who helped improve this project

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.