package dev.jianwii.basicdata;

import dev.jianwii.api.notification.NotificationApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.web.service.registry.ImportHttpServices;

@SpringBootApplication
// Entity 都放在 common，這裡只掃自己負責的 package，才不會連其他服務的資料表也建出來
@EntityScan("dev.jianwii.entity.basicdata")
@ImportHttpServices(group = "notification-service", types = NotificationApi.class)
public class BasicDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicDataApplication.class, args);
    }
}
