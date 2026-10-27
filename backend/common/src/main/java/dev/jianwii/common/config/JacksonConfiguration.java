package dev.jianwii.common.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.module.SimpleModule;

/** 共用 JSON 時間契約；Boot 會將 JacksonModule bean 套用到自動設定的 JsonMapper。 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfiguration {
    @Bean
    JacksonModule localTemporalModule() {
        return new SimpleModule("strict-local-temporals")
                .addDeserializer(
                        LocalDate.class,
                        strictLocalDeserializer(LocalDate.class, DateTimeFormatter.ISO_LOCAL_DATE, LocalDate::from))
                .addDeserializer(
                        LocalTime.class,
                        strictLocalDeserializer(LocalTime.class, DateTimeFormatter.ISO_LOCAL_TIME, LocalTime::from))
                .addDeserializer(
                        LocalDateTime.class,
                        strictLocalDeserializer(
                                LocalDateTime.class, DateTimeFormatter.ISO_LOCAL_DATE_TIME, LocalDateTime::from));
    }

    private static <T extends TemporalAccessor> ValueDeserializer<T> strictLocalDeserializer(
            Class<T> type, DateTimeFormatter formatter, TemporalQuery<T> query) {
        return new StdScalarDeserializer<T>(type) {
            @Override
            public T deserialize(JsonParser parser, DeserializationContext context) {
                if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                    return type.cast(context.handleUnexpectedToken(type, parser));
                }
                try {
                    return formatter.parse(parser.getString(), query);
                } catch (DateTimeParseException exception) {
                    return type.cast(context.handleWeirdStringValue(
                            type, parser.getString(), "%s 須為不帶 Z 或 offset 的 ISO local 格式", type.getSimpleName()));
                }
            }
        };
    }
}
