package dev.jianwii.basicdata.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.basicdata.User;

import java.util.List;
import java.util.Optional;

public interface UserDAO extends BaseDAO<User, Long> {

    /** 含已軟刪除的資料，才分辨得出「已停用」與「第一次登入」。 */
    Optional<User> findBySubject(String subject);

    /** 是否有有效使用者擁有此角色；刪除角色前檢查用。集合欄位用 Containing，會產生 MEMBER OF 條件。 */
    boolean existsByRoleIdsContainingAndDbMark(Long roleId, Integer dbMark);

    /** 沒有任何角色的使用者；補發預設角色用。 */
    List<User> findAllByRoleIdsIsEmptyAndDbMark(Integer dbMark);
}
