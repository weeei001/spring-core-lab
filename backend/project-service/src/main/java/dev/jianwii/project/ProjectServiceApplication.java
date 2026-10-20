package dev.jianwii.project;

import dev.jianwii.api.basicdata.SerialNumberApi;
import dev.jianwii.api.basicdata.UploadFileApi;
import dev.jianwii.api.basicdata.UserApi;
import dev.jianwii.api.notification.NotificationApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.web.service.registry.ImportHttpServices;

@SpringBootApplication
// Entity 都放在 common，這裡只掃自己負責的 package，才不會連其他服務的資料表也建出來
@EntityScan("dev.jianwii.entity.project")
// SerialNumberApi：新增專案、任務時取系統編號；UserApi：加入成員時確認使用者存在。
// group 即 Eureka 上的服務名稱，由 Spring Cloud LoadBalancer 找到實際位址
@ImportHttpServices(group = "basic-data", types = {SerialNumberApi.class, UserApi.class,
        UploadFileApi.class})
// NotificationApi：任務指派、狀態變更、新留言時發通知
@ImportHttpServices(group = "notification-service", types = NotificationApi.class)
public class ProjectServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectServiceApplication.class, args);
    }
}
