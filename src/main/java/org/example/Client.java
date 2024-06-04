package org.example;

import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.tcp.TcpClient;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        TcpClient client = TcpClient.create()
                .host("localhost")
                .port(12345);
        // inboundとoutboundの制御を切り離すため、handle()を使わない。
        // inboudはそのまま画面表示。outboundは標準入力スキャン&サーバ送信
        Connection connection = client.connectNow();
        connection.inbound().receive().asString(StandardCharsets.UTF_8).doOnNext(System.out::println).subscribe();

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.println("送りたい言葉を入力してEnter (byeで抜けるよ):");
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if ("bye".equals(line)) {
                break;
            }
            connection.outbound()
                    .sendString(Mono.just(line + "\r\n"))
                    .then()
                    .subscribe();
        }

        connection.disposeNow();
    }
}
