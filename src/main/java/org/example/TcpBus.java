package org.example;

import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import org.example.kafka.KafkaConsumer;
import org.example.kafka.KafkaPublisher;
import org.example.kafka.PseudoService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.tcp.TcpServer;

import java.nio.charset.StandardCharsets;

public class TcpBus implements AutoCloseable {

    private final KafkaPublisher upstreamPublisher;
    private final KafkaConsumer downstreamConsumer;

    public TcpBus() {
        this.upstreamPublisher = new KafkaPublisher("upstream-topic");
        this.downstreamConsumer = new KafkaConsumer("downstream-topic");
    }

    public static void main(String[] args) {
        try (PseudoService service = new PseudoService();
             TcpBus tcpBus = new TcpBus()){
            // 擬似サービスは起動したらここでブロックしてしまうのでスレッド越しで起動する。
            new Thread(service::start).start();
            // TCPサーバの起動
            tcpBus.launch();
        }
    }

    public void launch() {
        DisposableServer server = TcpServer.create()
                .port(12345)
                .doOnConnection(this::onConnection)
                .handle(this::tcpHandle)
                .bindNow();
        server.onDispose().block();
    }

    private Publisher<Void> tcpHandle(NettyInbound in, NettyOutbound out) {
        Flux<String> upstreamFlux = in.receive().asString(StandardCharsets.UTF_8).map(txt -> {
            upstreamPublisher.sendMessage(txt);
            // ここでブロッキングされる。(ダウンストリームTopicの到着待ち)
            return downstreamConsumer.consumeMessage();
        });
        return out.sendString(upstreamFlux);
    }

    private void onConnection(Connection con) {
        // 1024バイト長までの改行をフレーム区切りと見做している.
        con.addHandlerLast(
            new DelimiterBasedFrameDecoder(1024, Delimiters.lineDelimiter())
        );
    }

    @Override
    public void close() {
        upstreamPublisher.close();
        downstreamConsumer.close();
    }
}
