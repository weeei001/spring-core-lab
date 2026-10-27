package dev.jianwii.web;

import dev.jianwii.api.basicdata.api.RoleApi;
import dev.jianwii.api.basicdata.api.SerialNumberApi;
import dev.jianwii.api.basicdata.api.UploadFileApi;
import dev.jianwii.api.basicdata.api.UserApi;
import dev.jianwii.api.basicdata.api.LeaveApi;
import dev.jianwii.api.notification.api.NotificationApi;
import dev.jianwii.api.project.api.ManualQaBatchApi;
import dev.jianwii.api.project.api.ManualQaExecutionApi;
import dev.jianwii.api.project.api.ManualQaItemApi;
import dev.jianwii.api.project.api.MeetingApi;
import dev.jianwii.api.project.api.ProjectApi;
import dev.jianwii.api.project.api.ProjectLabelApi;
import dev.jianwii.api.project.api.ProjectMemberApi;
import dev.jianwii.api.project.api.TaskActivityApi;
import dev.jianwii.api.project.api.TaskApi;
import dev.jianwii.api.project.api.TaskAttachmentApi;
import dev.jianwii.api.project.api.TaskCommentApi;
import dev.jianwii.api.project.api.WorkLogApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.web.service.registry.ImportHttpServices;

// web-server 只透過 JDBC 保存 session；業務資料仍呼叫各服務，不啟用 JPA。
@SpringBootApplication(exclude = {HibernateJpaAutoConfiguration.class, DataJpaRepositoriesAutoConfiguration.class})
// 產生各服務 XxxApi 的實作 bean，注入後呼叫方法即送出 HTTP 請求；
// group 即 Eureka 上的服務名稱，由 Spring Cloud LoadBalancer 挑選實例
@ImportHttpServices(
        group = "basic-data",
        types = {SerialNumberApi.class, UserApi.class, RoleApi.class, UploadFileApi.class, LeaveApi.class})
@ImportHttpServices(
        group = "project-service",
        types = {
            ProjectApi.class,
            ManualQaBatchApi.class,
            ManualQaItemApi.class,
            ManualQaExecutionApi.class,
            MeetingApi.class,
            ProjectMemberApi.class,
            ProjectLabelApi.class,
            TaskApi.class,
            TaskCommentApi.class,
            TaskAttachmentApi.class,
            TaskActivityApi.class,
            WorkLogApi.class
        })
@ImportHttpServices(group = "notification-service", types = NotificationApi.class)
public class WebServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebServerApplication.class, args);
    }
}
