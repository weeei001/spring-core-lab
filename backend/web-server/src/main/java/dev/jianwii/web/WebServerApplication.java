package dev.jianwii.web;

import dev.jianwii.api.basicdata.RoleApi;
import dev.jianwii.api.basicdata.SerialNumberApi;
import dev.jianwii.api.basicdata.UploadFileApi;
import dev.jianwii.api.basicdata.UserApi;
import dev.jianwii.api.basicdata.leave.LeaveApi;
import dev.jianwii.api.notification.NotificationApi;
import dev.jianwii.api.project.MeetingApi;
import dev.jianwii.api.project.ProjectApi;
import dev.jianwii.api.project.ProjectLabelApi;
import dev.jianwii.api.project.ProjectMemberApi;
import dev.jianwii.api.project.TaskActivityApi;
import dev.jianwii.api.project.TaskApi;
import dev.jianwii.api.project.TaskAttachmentApi;
import dev.jianwii.api.project.TaskCommentApi;
import dev.jianwii.api.project.WorkLogApi;
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
