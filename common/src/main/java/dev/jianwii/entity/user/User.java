package dev.jianwii.entity.user;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.common.validation.ValidGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 使用者。資料表只在 user-service（@EntityScan 此 package）；其他服務透過 UserApi 取得。
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
public class User extends BaseEntity {

    /** 登入帳號，不可重複。 */
    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 50)
    @Pattern(regexp = "[A-Za-z0-9._-]+")
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @NotBlank(groups = ValidGroup.Create.class)
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Email
    @Size(max = 200)
    @Column(name = "email", length = 200)
    private String email;
}
