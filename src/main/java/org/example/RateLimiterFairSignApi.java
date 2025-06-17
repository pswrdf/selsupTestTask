package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.time.Duration;

import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

@Slf4j
public class RateLimiterFairSignApi {

    private static final Duration DEADLINE_TIMEOUT = Duration.ofDays(1L);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RateLimiter rateLimiter;

    public RateLimiterFairSignApi(Duration timeRange, int requestLimit) {
        notNull(timeRange, "Поле timeRange должно быть заполнено: {}", timeRange);
        isTrue(requestLimit > 0, "Поле requestLimit должно быть >0: {}", requestLimit);

        RateLimiterConfig limiterConfig = RateLimiterConfig.custom()
                .limitForPeriod(requestLimit)
                .limitRefreshPeriod(timeRange)
                .timeoutDuration(DEADLINE_TIMEOUT)
                .build();
        rateLimiter = RateLimiterRegistry.of(limiterConfig).rateLimiter("rl");
    }

    //Тут по постановке не ясно зачем второй параметр - подпись.
    public void createDocument(FairSignDocument document) throws JsonProcessingException {
        if (rateLimiter.acquirePermission()) {
            ClassicRequestBuilder.post("/api/v3/lk/documents/create")
                    .addParameter("pg", document.getProductGroup())
                    .setEntity(objectMapper.writeValueAsBytes(document), ContentType.APPLICATION_JSON);
        } else {
            log.warn("Время ожидания истекло:  {}", rateLimiter.getRateLimiterConfig().getTimeoutDuration());
        }
    }

    @RequiredArgsConstructor
    @Getter
    public static class FairSignDocument {
        private final String documentFormat;
        private final String productDocument;
        private final String productGroup;
        private final String signature;
        private final String type;
    }
}
