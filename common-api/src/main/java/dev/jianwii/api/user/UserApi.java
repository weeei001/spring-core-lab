package dev.jianwii.api.user;

import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.user.User;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * user-service 的內部 API，只給其他服務呼叫：成功直接回物件，不包 Result；失敗仍回 Result.failure 與 HTTP 狀態，
 * 由 common 的 RemoteErrorHandler 還原成 RemoteServiceException。
 *
 * <ul>
 * <li>提供端：user-service 的 UserController implements 此介面。</li>
 * <li>呼叫端：{@code @ImportHttpServices(group = "user-service", types = UserApi.class)}，
 * 群組名稱即 Eureka 服務名稱。</li>
 * </ul>
 * gateway 沒有轉發 /internal/**，外部無法經 gateway 存取。
 */
@HttpExchange("/internal/user")
public interface UserApi {

    /** 查無資料時對方回 404，呼叫端收到 RemoteServiceException。 */
    @GetExchange("/{id}")
    User getById(@PathVariable("id") Long id);

    /** 批次查詢，最多 100 筆；不存在的 id 直接略過。列表要顯示使用者時用這個，不要逐筆呼叫 getById。 */
    @PostExchange("/batch")
    List<User> getByIds(@RequestBody List<Long> ids);

    /** 依條件查詢，page 必須為 1、最多 size 筆。 */
    @PostExchange("/query")
    List<User> query(@Valid @RequestBody UserQueryParameter parameter);

    @PostExchange
    User create(@Validated(ValidGroup.Create.class) @RequestBody User user);
}
