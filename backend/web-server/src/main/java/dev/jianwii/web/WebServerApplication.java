package dev.jianwii.web;

import dev.jianwii.api.basicdata.RoleApi;
import dev.jianwii.api.basicdata.SerialNumberApi;
import dev.jianwii.api.basicdata.UserApi;
import dev.jianwii.api.project.MilestoneApi;
import dev.jianwii.api.project.ProjectApi;
import dev.jianwii.api.project.ProjectMemberApi;
import dev.jianwii.api.project.TaskActivityApi;
import dev.jianwii.api.project.TaskApi;
import dev.jianwii.api.project.TaskCommentApi;
import dev.jianwii.api.project.WorkLogApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.web.service.registry.ImportHttpServices;

// common 會帶入 JPA（Entity、QueryParameter 需要），但 web-server 沒有自己的資料庫，
// 排除 DataSource 自動設定，啟動時才不會要求資料庫連線
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
// 產生各服務 XxxApi 的實作 bean，注入後呼叫方法即送出 HTTP 請求；
// group 即 Eureka 上的服務名稱，由 Spring Cloud LoadBalancer 挑選實例
@ImportHttpServices(group = "basic-data", types = {SerialNumberApi.class, UserApi.class, RoleApi.class})
@ImportHttpServices(group = "project-service", types = {ProjectApi.class, ProjectMemberApi.class,
        MilestoneApi.class, TaskApi.class, TaskCommentApi.class, TaskActivityApi.class,
        WorkLogApi.class})
public class WebServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebServerApplication.class, args);
    }
}
