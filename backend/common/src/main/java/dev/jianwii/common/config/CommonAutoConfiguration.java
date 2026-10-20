package dev.jianwii.common.config;

import dev.jianwii.common.remote.OperatorForwardingInterceptor;
import dev.jianwii.common.remote.RemoteErrorHandler;
import dev.jianwii.common.web.ExceptionHandleController;
import dev.jianwii.common.web.Operator;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.HttpStatusCode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

/**
 * 服務只要依賴 common 就會自動套用：全域例外處理、JPA Auditing（自動填 BaseEntity 的建立／修改者與時間）、 呼叫其他服務時的操作者傳遞與錯誤轉換。 跟 Spring
 * Boot 各 starter 的自動設定同一套機制：類別名稱列在
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports，
 * 因此服務的 @SpringBootApplication 不必掃描 dev.jianwii.common。
 */
// 排在 JPA 自動設定之後，下面的 @ConditionalOnBean(EntityManagerFactory) 才判斷得準
@AutoConfiguration(
        afterName = "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration")
// 註冊全域例外處理
@Import(ExceptionHandleController.class)
public class CommonAutoConfiguration {

    /** JPA Auditing：存檔時自動填入 @CreatedBy、@CreatedDate 等欄位。有資料庫的服務才啟用，web-server 不套用。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(EntityManagerFactory.class)
    @EnableJpaAuditing
    static class JpaAuditingConfiguration {

        /**
         * 告訴 JPA Auditing 目前操作者是誰：取自 {@link Operator#HEADER}，沒有時記為 system；服務自行定義 AuditorAware bean
         * 即可覆蓋。
         */
        @Bean
        @ConditionalOnMissingBean(AuditorAware.class)
        AuditorAware<String> operatorAuditor() {
            return () -> Optional.of(Operator.current());
        }
    }

    /**
     * 呼叫其他服務時：送出前帶上操作者 header，收到 4xx/5xx 時轉成 RemoteServiceException。 套用到 Spring Boot 提供的
     * RestClient.Builder 與 @ImportHttpServices 產生的 XxxApi； 自己用 RestClient.builder() 建立的不會套用。
     */
    @Bean
    RestClientCustomizer commonRestClientCustomizer(JsonMapper jsonMapper) {
        RemoteErrorHandler errorHandler = new RemoteErrorHandler(jsonMapper);
        return builder -> builder.requestInterceptor(new OperatorForwardingInterceptor())
                .defaultStatusHandler(HttpStatusCode::isError, errorHandler);
    }
}
