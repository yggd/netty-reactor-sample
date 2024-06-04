package org.example.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.util.HashMap;
import java.util.Map;

public class KafkaPublisher implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(KafkaPublisher.class);

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    private final String topicName;
    private final KafkaSender<Integer, String> sender;

    public KafkaPublisher(String topicName) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, topicName + "-publisher");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "GroupSample");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        SenderOptions<Integer, String> senderOptions = SenderOptions.create(props);

        this.topicName = topicName;
        this.sender = KafkaSender.create(senderOptions);
    }

    public void sendMessage(String message) {
        sender.send(Mono.just(SenderRecord.create(new ProducerRecord<>(this.topicName, 1, message), 1)))
                .doOnError(e -> logger.error("Send failed", e))
                .subscribe();
    }

    @Override
    public void close() {
        sender.close();
    }
}
