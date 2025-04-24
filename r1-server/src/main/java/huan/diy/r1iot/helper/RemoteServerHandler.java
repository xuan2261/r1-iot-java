package huan.diy.r1iot.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import huan.diy.r1iot.model.AsrResult;
import huan.diy.r1iot.util.R1IotUtils;
import huan.diy.r1iot.util.TcpChannelUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
public class RemoteServerHandler extends ChannelInboundHandlerAdapter {

    private static final ObjectMapper objectMapper = R1IotUtils.getObjectMapper();

    private final AsrServerHandler asrServerHandler;
    private final KeepClientTCPAlive keepClientTCPAlive;

    public RemoteServerHandler(AsrServerHandler asrServerHandler, KeepClientTCPAlive keepClientTCPAlive) {
        super();
        this.asrServerHandler = asrServerHandler;
        this.keepClientTCPAlive = keepClientTCPAlive;
    }

    private Future<?> taskFuture = null;
    private AtomicInteger delayMs = new AtomicInteger(860);

    private StringBuffer accumulatedData = new StringBuffer();
    private StringBuffer asrText = new StringBuffer();
    private AtomicBoolean handling = new AtomicBoolean(false);
    private AtomicReference<String> stopRef = new AtomicReference<>("zz");

    private static final Pattern pattern = Pattern.compile("PN:\\s*(\\S+)");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            ByteBuf responseData = (ByteBuf) msg;
            String data = responseData.toString(StandardCharsets.UTF_8);
//            log.info("each data from remote server: {}", data);

            Channel clientChannel = ctx.channel().attr(TcpChannelUtils.CLIENT_CHANNEL).get();
            if (clientChannel == null) {
                return;
            }

            String deviceId = clientChannel.attr(TcpChannelUtils.DEVICE_ID).get();

            if (!data.contains("PN: q") && taskFuture == null) {
                // Tạo nhiệm vụ
                clientChannel.writeAndFlush(msg);
                taskFuture = keepClientTCPAlive.startKeepAliveTask(data, clientChannel, delayMs, stopRef);
            }

            AsrResult asrResult = asrServerHandler.handle(data);
//            log.info("asr result: {}", asrResult);

            switch (asrResult.getType()) {
                case DROPPED, SKIP:
                    return;
                case APPEND:
                    accumulatedData.append(asrResult.getFixedData());
                    try {
                        String[] lines = accumulatedData.toString().split("\n");
                        JsonNode node = objectMapper.readTree(lines[lines.length - 1]);
                        if (node.has("responseId")) {
                            handling.compareAndSet(false, true);
                            break;
                        }
                        return;
                    } catch (Exception e) {
                        return;
                    }
                case END:
                    handling.compareAndSet(false, true);
                    accumulatedData.append(asrResult.getFixedData());
                    break;
                case PREFIX:
                    asrText.append(asrResult.getFixedData());
                    return;

            }

            if (!handling.get()) {
                return;
            }

            if (clientChannel.attr(TcpChannelUtils.END).get() != Boolean.TRUE) {
                asrText.append(asrResult.getFixedData());
                return;
            }

            String rawData = accumulatedData.toString();

            rawData = extractLastEligible(rawData);

            log.info("from R1: {} {}", asrText.toString(), rawData);

            String clientIp = clientChannel.attr(TcpChannelUtils.CLIENT_IP).get();
            if (clientIp != null) {
                R1IotUtils.CLIENT_IP.set(clientIp);
            }

            String aiReply = asrServerHandler.enhance(asrText.toString(), rawData, deviceId);

            Matcher matcher = pattern.matcher(rawData);
            String pnValue = null;
            if (matcher.find()) {
                pnValue = matcher.group(1);
            }

            if (taskFuture != null) {
                // Điều khiển tăng tốc
                stopRef.set(pnValue);
                delayMs.set(1);
//                log.info("speed up {}", delayMs.get());
                try {
                    taskFuture.get(); // 同步等待
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            if (aiReply == null) {
                clientChannel.writeAndFlush(ctx.alloc().buffer().writeBytes(accumulatedData.toString().getBytes()));
                return;
            }

            log.info("from AI: {}", aiReply);

            clientChannel.writeAndFlush(ctx.alloc().buffer().writeBytes(aiReply.getBytes()));
            accumulatedData.setLength(0);  // Xóa dữ liệu tích lũy

        }
    }

    public String extractLastEligible(String httpResponse) {
        // Split the response by "HTTP/1.1 200" to get all blocks
        String[] blocks = httpResponse.split("(?=HTTP/1.1 200)");

        // The last block will be the one we want
        String lastBlock = blocks[blocks.length - 1];

        return lastBlock;
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            log.info("Sending heartbeat to remote server");
            ctx.writeAndFlush(ctx.alloc().buffer().writeBytes("HEARTBEAT".getBytes()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Remote server handler error", cause);
        ctx.close();
    }
}