package dev.jianwii.common.remote;

import dev.jianwii.common.web.Operator;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * 呼叫其他服務時帶上目前操作者，對方的 BaseEntity 審計欄位才會記到同一個人。
 */
public class OperatorForwardingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().set(Operator.HEADER, Operator.encode(Operator.current()));
        return execution.execute(request, body);
    }
}
