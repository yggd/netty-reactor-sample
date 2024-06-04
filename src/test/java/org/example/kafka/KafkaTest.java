package org.example.kafka;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class KafkaTest {

    private static final String TOPIC_NAME= "upstream-topic";

    @Test
    void publishAndConsumeTest() {
        try (KafkaPublisher publisher = new KafkaPublisher(TOPIC_NAME);
            KafkaConsumer consumer = new KafkaConsumer(TOPIC_NAME)) {

            // pub -> consume test.
            publisher.sendMessage("hello world");
            String consumed = consumer.consumeMessage();

            assertThat(consumed, is("hello world"));
        }
    }
}
