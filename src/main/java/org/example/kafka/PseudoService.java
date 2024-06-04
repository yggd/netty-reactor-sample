package org.example.kafka;

import java.util.concurrent.TimeUnit;

/**
 * Kafkaの対向擬似サービス
 */
public class PseudoService implements AutoCloseable {

    private volatile boolean running = true;

    public void start() {
        try (KafkaPublisher publisher = new KafkaPublisher("downstream-topic");
            KafkaConsumer consumer = new KafkaConsumer("upstream-topic")) {
            while (running) {
                String upstreamMessage = consumer.pollMessage(1L, TimeUnit.SECONDS);
                if (upstreamMessage == null) {
                    continue;
                }
                publisher.sendMessage(String.format("そうだね、%sだね。", upstreamMessage));
            }
        }
    }

    @Override
    public void close() {
        this.running = false;
    }
}
