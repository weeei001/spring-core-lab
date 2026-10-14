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
 * 引用 common 的服務自動套用：全域例外處理、JPA Auditing（BaseEntity 的建立／修改者與時間）、
 * 呼叫其他服務時的操作者傳遞與錯誤還原。
 * 透過 META-INF/spring/...AutoConfiguration.imports 載入，服務的 @SpringBootApplication 不必掃描 dev.jianwii.common。
 */
// 排在 JPA 之後，才判斷得到有沒有 EntityManagerFactory
@AutoConfiguration(afterName = "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration")
@Import(ExceptionHandleController.class)
public class CommonAutoConfiguration {

    /** 有資料庫的服務才啟用；web-server 沒有 DataSource，不套用。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(EntityManagerFactory.class)
    @EnableJpaAuditing
    static class JpaAuditingConfiguration {

        /** 操作者取自 {@link Operator#HEADER}，沒有時記為 system；服務自行定義 AuditorAware bean 即可覆蓋。 */
        @Bean
        @ConditionalOnMissingBean(AuditorAware.class)
        AuditorAware<String> operatorAuditor() {
            return () -> Optional.of(Operator.current());
        }
    }

    /**
     * 套用到 Spring Boot 提供的 RestClient.Builder 與 HTTP Interface 服務群組；
     * 自己用 RestClient.builder() 建立的不會套用。
     */
    @Bean
    RestClientCustomizer commonRestClientCustomizer(JsonMapper jsonMapper) {
        RemoteErrorHandler errorHandler = new RemoteErrorHandler(jsonMapper);
        return builder -> builder.requestInterceptor(new OperatorForwardingInterceptor())
                .defaultStatusHandler(HttpStatusCode::isError, errorHandler);
    }
}
