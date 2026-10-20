package dev.jianwii.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.core.env.Profiles;

/**
 * 服務註冊中心：各服務啟動時向這裡登記自己的位址，呼叫端用服務名稱（例如 project-service）就能找到實際位址。
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(EurekaServerApplication.class);
        application.addInitializers(context -> {
            var env = context.getEnvironment();
            if (env.acceptsProfiles(Profiles.of("deploy"))) {
                if (env.acceptsProfiles(Profiles.of("local"))) {
                    throw new IllegalStateException("deploy 不可同時啟用 local");
                }
                if (env.getProperty("INTERNAL_BIND_ADDRESS", "").isBlank()) {
                    throw new IllegalStateException("部署設定缺漏：INTERNAL_BIND_ADDRESS");
                }
            }
        });
        application.run(args);
    }
}
