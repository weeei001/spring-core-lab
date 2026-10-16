package dev.jianwii.notification;

import dev.jianwii.api.basicdata.UserApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.service.registry.ImportHttpServices;

@SpringBootApplication
// Entity 都放在 common，這裡只掃自己負責的 package，才不會連其他服務的資料表也建出來
@EntityScan("dev.jianwii.entity.notification")
// UserApi：確認收件人存在、取得 Email
@ImportHttpServices(group = "basic-data", types = UserApi.class)
// @Async：commit 後在背景執行緒寄信，不讓呼叫端等 SMTP；執行緒池用 Spring Boot 自動建立的 applicationTaskExecutor
@EnableAsync
// @Scheduled：定期重試寄送失敗的信
@EnableScheduling
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
