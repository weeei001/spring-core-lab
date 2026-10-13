package dev.jianwii.api.user;

import dev.jianwii.common.query.NameLikeQuery;
import dev.jianwii.common.query.QueryIgnore;
import dev.jianwii.common.query.QueryParameter;
import dev.jianwii.entity.user.User;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * username 完全相同；namePrefix 比對 name 前綴。
 */
@Getter
@Setter
public class UserQueryParameter extends QueryParameter<User> implements NameLikeQuery {

    @Size(max = 50)
    private String username;

    @QueryIgnore
    @Size(max = 100)
    @Pattern(regexp = "(?sU).*\\S.*")
    private String namePrefix;
}
