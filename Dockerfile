# Giai đoạn 1: Xây dựng file jar
FROM eclipse-temurin:17.0.14_7-jdk as builder

ENV MAVEN_VERSION=3.9.9
ENV MAVEN_HOME=/opt/maven
ENV PATH=${MAVEN_HOME}/bin:${PATH}

RUN apt-get update && \
    apt-get install -y curl tar && \
    curl -fsSL https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -o /tmp/maven.tar.gz && \
    mkdir -p ${MAVEN_HOME} && \
    tar -xzf /tmp/maven.tar.gz -C ${MAVEN_HOME} --strip-components=1 && \
    rm -rf /tmp/maven.tar.gz && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /workspace
COPY . .

# Xây dựng dự án, tạo file jar
RUN mvn clean package -DskipTests

# Giai đoạn 2: Chạy file jar
FROM eclipse-temurin:17.0.14_7-jdk

WORKDIR /app

# Cài đặt công cụ cơ bản và ffmpeg
RUN apt-get update && \
    apt-get install -y wget ffmpeg ca-certificates && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Tải xuống các file nhị phân yt-dlp và cloudflared tương ứng với kiến trúc hệ thống
RUN ARCH=$(uname -m) && \
    echo "Phát hiện kiến trúc hệ thống: $ARCH" && \
    if [ "$ARCH" = "x86_64" ]; then \
        # yt-dlp
        YT_URL="https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux"; \
        # cloudflared
        CF_URL="https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb"; \
    elif [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "arm64" ]; then \
        YT_URL="https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux_aarch64"; \
        CF_URL="https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64.deb"; \
    else \
        echo "Kiến trúc không được hỗ trợ: $ARCH"; exit 1; \
    fi && \
    # Tải xuống và cài đặt yt-dlp
    echo "Tải xuống yt-dlp URL: $YT_URL" && \
    wget "$YT_URL" -O /usr/local/bin/yt-dlp && \
    chmod a+rx /usr/local/bin/yt-dlp && \
    yt-dlp --version && \
    # Tải xuống và cài đặt cloudflared
    echo "Tải xuống cloudflared URL: $CF_URL" && \
    wget "$CF_URL" -O /tmp/cloudflared.deb && \
    dpkg -i /tmp/cloudflared.deb || apt-get install -f -y && \
    rm -f /tmp/cloudflared.deb && \
    cloudflared --version

# Sao chép file jar từ giai đoạn xây dựng
COPY --from=builder /workspace/r1-server/target/*.jar app.jar

COPY r1-server/src/main/resources/scripts/manage_cloudflared.sh /manage_cloudflared.sh
RUN chmod +x /manage_cloudflared.sh

ENTRYPOINT ["java", "-jar", "app.jar"]