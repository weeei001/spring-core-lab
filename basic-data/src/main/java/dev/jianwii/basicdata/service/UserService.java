package dev.jianwii.basicdata.service;

import dev.jianwii.api.basicdata.UserQueryParameter;
import dev.jianwii.basicdata.dao.UserDAO;
import dev.jianwii.common.exception.ForbiddenException;
import dev.jianwii.common.service.BaseService;
import dev.jianwii.entity.basicdata.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 使用者查詢與登入同步。 */
@Service
public class UserService extends BaseService<User, UserQueryParameter> {
    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        super(userDAO);
        this.userDAO = userDAO;
    }

    /**
     * 登入同步：依 subject 找使用者，沒有就建立；有就以 authentik 的資料更新。
     * 資料沒變時 JPA 不會送出 UPDATE，transTime 也不會變。
     */
    @Transactional
    public User login(User login) {
        String name = StringUtils.hasText(login.getName()) ? login.getName()
                : StringUtils.hasText(login.getUsername()) ? login.getUsername() : login.getSubject();
        User user = userDAO.findBySubject(login.getSubject()).orElse(null);
        if (user == null) {
            login.setName(name);
            return create(login);
        }
        if (user.getDbMark() != User.VALID) {
            throw new ForbiddenException("帳號已停用");
        }
        user.setUsername(login.getUsername());
        user.setName(name);
        user.setEmail(login.getEmail());
        return userDAO.saveAndFlush(user);
    }
}
