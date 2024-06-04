package org.example.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import reactor.core.Disposable;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class KafkaConsumer implements AutoCloseable {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    private final Disposable disposable;

    // イケてない。
    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

    public KafkaConsumer(String topicName) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, topicName + "-consumer");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "GroupSample");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ReceiverOptions<Integer, String> options = ReceiverOptions
                .<Integer, String>create(props)
                .subscription(Collections.singleton(topicName));
        this.disposable = KafkaReceiver.create(options).receive().onErrorStop().subscribe(r -> {
            ReceiverOffset offset = r.receiverOffset();
            queue.add(r.value());
            offset.acknowledge();
        });
    }

    public String consumeMessage() {
        String message = null;
        try {
            message = queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return message;
    }

    public String pollMessage(long timeout, TimeUnit unit) {
        String message = null;
        try {
            message = queue.poll(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return message;
    }

    @Override
    public void close() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
