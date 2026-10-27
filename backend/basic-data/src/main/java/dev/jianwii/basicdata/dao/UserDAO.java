package dev.jianwii.basicdata.dao;

import dev.jianwii.common.dao.BaseDAO;
import dev.jianwii.entity.basicdata.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDAO extends BaseDAO<User, Long> {

    /**
     * 有效帳號的權限 code；LEFT JOIN 保留沒有角色的帳號（回一筆 null），帳號不存在或已停用時回空清單。 原生 SQL 不會套用
     * @SoftDelete，要自己加 db_mark = 0。
     */
    @Query(value = """
            select rp.permission_code
            from sys_user u
            left join sys_user_role ur on ur.user_id = u.id
            left join sys_role r on r.id = ur.role_id and r.db_mark = 0
            left join sys_role_permission rp on rp.role_id = r.id
            where u.id = :id and u.db_mark = 0
            """, nativeQuery = true)
    List<String> authorization(@Param("id") Long id);

    Optional<User> findBySubject(String subject);

    /** 已停用（軟刪除）的帳號數，分辨「已停用」與「第一次登入」；JPQL 會被 @SoftDelete 過濾，所以用原生 SQL。 */
    @Query(value = "select count(*) from sys_user where subject = :subject and db_mark = 1", nativeQuery = true)
    long countDeletedBySubject(@Param("subject") String subject);

    /** 是否有有效使用者擁有此角色；刪除角色前檢查用。集合欄位用 Containing，會產生 MEMBER OF 條件。 */
    boolean existsByRoleIdsContaining(Long roleId);

    /** 沒有任何角色的使用者；補發預設角色用。 */
    List<User> findAllByRoleIdsIsEmpty();
}
