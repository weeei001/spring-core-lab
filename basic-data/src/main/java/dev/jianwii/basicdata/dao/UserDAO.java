package dev.jianwii.basicdata.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.basicdata.User;

import java.util.Optional;

public interface UserDAO extends BaseDAO<User, Long> {

    /** 含已軟刪除的資料，才分辨得出「已停用」與「第一次登入」。 */
    Optional<User> findBySubject(String subject);
}
