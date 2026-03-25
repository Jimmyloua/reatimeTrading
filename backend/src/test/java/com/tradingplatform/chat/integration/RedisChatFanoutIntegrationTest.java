package com.tradingplatform.chat.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Redis chat fan-out contract tests")
class RedisChatFanoutIntegrationTest {

    private static final String MESSAGE_CHANNEL = "chat:realtime:message";
    private static final String PRESENCE_CHANNEL = "chat:realtime:presence";
    private static final Path CHAT_SOURCE_ROOT = Path.of("src", "main", "java", "com", "tradingplatform", "chat");

    @Test
    @DisplayName("message events are wired from the Redis message channel into /queue/messages fan-out")
    void messageEventsAreWiredFromRedisChannelIntoQueueMessagesFanout() throws IOException {
        String source = readChatSources();

        assertTrue(
            source.contains(MESSAGE_CHANNEL),
            "Expected backend chat sources to subscribe or publish the Redis channel " + MESSAGE_CHANNEL
        );
        assertTrue(
            source.contains("convertAndSendToUser") && source.contains("\"/queue/messages\""),
            "Expected Redis message events to fan out via convertAndSendToUser(recipientUserId.toString(), \"/queue/messages\", ...)"
        );
    }

    @Test
    @DisplayName("presence events are wired from the Redis presence channel into /topic/presence.{userId} fan-out")
    void presenceEventsAreWiredFromRedisPresenceChannelIntoTopicPresenceFanout() throws IOException {
        String source = readChatSources();

        assertTrue(
            source.contains(PRESENCE_CHANNEL),
            "Expected backend chat sources to subscribe or publish the Redis channel " + PRESENCE_CHANNEL
        );
        assertTrue(
            source.contains("convertAndSend") && source.contains("\"/topic/presence.\""),
            "Expected Redis presence events to fan out via convertAndSend(\"/topic/presence.{userId}\", ...)"
        );
    }

    private String readChatSources() throws IOException {
        try (Stream<Path> paths = Files.walk(CHAT_SOURCE_ROOT)) {
            return paths
                .filter(path -> path.toString().endsWith(".java"))
                .map(this::readFile)
                .reduce("", (left, right) -> left + "\n" + right);
        }
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
    }
}
