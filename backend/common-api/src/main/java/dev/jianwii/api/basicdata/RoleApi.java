package dev.jianwii.api.basicdata;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.basicdata.Role;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * basic-data 的角色 API（Spring HTTP Interface），用法同 {@link SerialNumberApi}。
 * 角色是一組功能權限（{@link dev.jianwii.entity.basicdata.Permission}），再透過 {@link UserApi#updateRoles}
 * 指派給使用者。
 */
@HttpExchange("/api/basic-data/role")
public interface RoleApi {

    /** 不分頁查詢（例如指派角色時的選單）：page 必須為 1，最多回傳 size 筆。 */
    @PostExchange("/query")
    List<Role> query(@Valid @RequestBody RoleQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。 */
    @PostExchange("/queryPage")
    PageResponse<Role> queryPage(@Valid @RequestBody RoleQueryParameter parameter);

    /** 新增；code 不可與既有角色重複（含已刪除），permissionCodes 必須是已定義的權限。 */
    @PostExchange
    Role create(@Validated(ValidGroup.Create.class) @RequestBody Role role);

    /** 部分更新：只更新有給值（非 null）的欄位；code 不可修改，permissionCodes 給了就整批取代。 */
    @PatchExchange("/{id}")
    Role update(@PathVariable("id") Long id, @Validated(ValidGroup.Update.class) @RequestBody Role role);

    /** 軟刪除；仍有使用者擁有此角色時回 409。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}
