package net.samitkumar.springboot_ping_pong.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Worker Lambda: S3 ObjectCreated notifications arrive through SQS, one notification per message body (optionally
 * wrapped in an SNS envelope when S3 publishes to a topic that fans out to queues).
 *
 * <p>Failed messages are reported individually through {@link SQSBatchResponse}, so one bad message does not make SQS
 * redeliver the whole batch; this needs {@code ReportBatchItemFailures} on the event source mapping.
 */
public class S3EventWorkerHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {
    private static final Logger log = LoggerFactory.getLogger(S3EventWorkerHandler.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        var failures = new ArrayList<SQSBatchResponse.BatchItemFailure>();
        List<SQSEvent.SQSMessage> messages = event.getRecords() == null ? List.of() : event.getRecords();
        for (SQSEvent.SQSMessage message : messages) {
            try {
                for (String key : objectKeys(message.getBody())) process(key);
            } catch (Exception e) {
                log.error("Message {} failed and will be retried", message.getMessageId(), e);
                failures.add(new SQSBatchResponse.BatchItemFailure(message.getMessageId()));
            }
        }
        return new SQSBatchResponse(failures);
    }

    void process(String objectKey) {
        log.info("Object created: {}", objectKey);
    }

    /**
     * Created object keys in an S3 event notification, URL-decoded (S3 encodes them like form values, spaces as
     * {@code +}). The {@code s3:TestEvent} S3 sends when the notification is configured has none.
     */
    static List<String> objectKeys(String body) {
        JsonNode root = JSON.readTree(body);
        if ("Notification".equals(root.path("Type").asString(null)) && root.path("Message").isString()) {
            root = JSON.readTree(root.path("Message").asString());
        }
        if ("s3:TestEvent".equals(root.path("Event").asString(null))) return List.of();
        var keys = new ArrayList<String>();
        for (JsonNode record : root.path("Records")) {
            if (!record.path("eventName").asString("").startsWith("ObjectCreated")) continue;
            String key = record.path("s3").path("object").path("key").asString(null);
            if (key != null) keys.add(URLDecoder.decode(key, StandardCharsets.UTF_8));
        }
        return keys;
    }
}
