package dev.jianwii.common.remote;

import dev.jianwii.common.web.Operator;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * 呼叫其他服務時，把目前操作者放進 {@value Operator#HEADER} header 一起送出，對方存檔時的建立／修改者才會是同一個人。
 * 由 CommonAutoConfiguration 掛到 RestClient 上。
 */
public class OperatorForwardingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().set(Operator.HEADER, Operator.encode(Operator.current()));
        return execution.execute(request, body);
    }
}
