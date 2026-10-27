package dev.jianwii.api.basicdata.api;

import dev.jianwii.api.basicdata.dto.UserAuthorization;
import dev.jianwii.api.basicdata.dto.UserQueryParameter;
import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.entity.basicdata.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

/**
 * basic-data 的使用者 API（Spring HTTP Interface），用法同 {@link SerialNumberApi}。 使用者由登入自動建立（帶預設角色
 * {@code Role.DEFAULT_CODE}），所以沒有新增、修改；權限透過指派角色（{@link #updateRoles}）管理。
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
     * 登入同步，只給 web-server 在登入成功時呼叫：依 subject 找使用者，沒有就建立， 有就以 OIDC 提供者的資料更新
     * username、name、email。已停用（軟刪除）的使用者回 403。
     */
    @PostExchange("/login")
    User login(@Valid @RequestBody User user);

    /** 指派角色：整批取代為 roleIds（給空集合為移除全部角色）；角色必須存在。 */
    @PutExchange("/{id}/roles")
    User updateRoles(@PathVariable("id") Long id, @RequestBody Set<Long> roleIds);

    /**
     * 使用者所有角色的功能權限 code（聯集），只給 web-server 載入權限用。 使用者不存在或已停用時回空集合。
     */
    @GetExchange("/{id}/permissions")
    Set<String> permissions(@PathVariable("id") Long id);

    /** 內部授權查詢；帳號不存在或停用時 active 為 false。 */
    @GetExchange("/{id}/authorization")
    UserAuthorization authorization(@PathVariable("id") Long id);
}
