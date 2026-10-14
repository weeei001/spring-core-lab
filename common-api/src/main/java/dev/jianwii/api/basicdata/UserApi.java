package dev.jianwii.api.basicdata;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.basicdata.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * basic-data 的使用者 API（Spring HTTP Interface），用法同 {@link SerialNumberApi}。
 * 使用者由登入自動建立，所以只有查詢與登入同步。
 */
@HttpExchange("/api/basic-data/user")
public interface UserApi {

    /** 不分頁查詢（例如依 ids 把 createId、transId 換成名字）：page 必須為 1，最多回傳 size 筆。 */
    @PostExchange("/query")
    List<User> query(@Valid @RequestBody UserQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。 */
    @PostExchange("/queryPage")
    PageResponse<User> queryPage(@Valid @RequestBody UserQueryParameter parameter);

    /**
     * 登入同步，只給 web-server 在登入成功時呼叫：依 subject 找使用者，沒有就建立，
     * 有就以 authentik 的資料更新 username、name、email。已停用（軟刪除）的使用者回 403。
     */
    @PostExchange("/login")
    User login(@Valid @RequestBody User user);
}
