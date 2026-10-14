package dev.jianwii.entity.basicdata;

import dev.jianwii.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 使用者。authentik 只負責驗證身分，使用者資料以這張表為準；
 * 各表的 createId、transId 存的是這裡的 id，前端再以 id 查名字。
 * 登入時由 web-server 呼叫 UserApi.login 建立或同步，不提供手動新增、修改。
 */
@Entity
// user 是 MySQL 關鍵字，避開
@Table(name = "sys_user")
@Getter
@Setter
public class User extends BaseEntity {

    /** authentik 的使用者識別（登入 token 的 sub），登入時用來對應這筆資料；不可修改。 */
    @NotBlank
    @Size(max = 255)
    @Column(name = "subject", nullable = false, unique = true, updatable = false)
    private String subject;

    /** 登入帳號（authentik 的 preferred_username）。 */
    @Size(max = 150)
    @Column(name = "username", length = 150)
    private String username;

    /** 顯示名稱；authentik 沒給時以帳號代替。 */
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 254)
    @Column(name = "email", length = 254)
    private String email;
}
