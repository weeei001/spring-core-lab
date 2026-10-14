package dev.jianwii.common.remote;

import dev.jianwii.common.exception.RemoteServiceException;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * 對方回 4xx/5xx 時，解析 {@code Result.failure} 的 errorCode、message，轉成 {@link RemoteServiceException}。
 * 回應不是 Result 格式（例如服務沒啟動、gateway 回的錯誤頁）時改用通用訊息。
 * 由 CommonAutoConfiguration 掛到 RestClient 上，呼叫端不必自己檢查 HTTP 狀態。
 */
public class RemoteErrorHandler implements RestClient.ResponseSpec.ErrorHandler {

    private final JsonMapper jsonMapper;

    public RemoteErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
        int statusCode = response.getStatusCode().value();
        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        String errorCode = "遠端服務錯誤";
        String message = "呼叫 " + request.getURI().getHost() + " 失敗（HTTP " + statusCode + "）";
        try {
            JsonNode body = jsonMapper.readTree(response.getBody().readAllBytes());
            if (body != null && body.isObject()) {
                errorCode = body.path("errorCode").stringValue(errorCode);
                message = body.path("message").stringValue(message);
            }
        } catch (JacksonException ignored) {
            // 不是 JSON，保留通用訊息
        }
        throw new RemoteServiceException(status, errorCode, message);
    }
}
