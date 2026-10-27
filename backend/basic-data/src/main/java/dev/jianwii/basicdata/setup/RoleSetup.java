package dev.jianwii.basicdata.setup;

import static dev.jianwii.entity.basicdata.Permission.LEAVE_APPLY;
import static dev.jianwii.entity.basicdata.Permission.LEAVE_APPROVE;
import static dev.jianwii.entity.basicdata.Permission.MANUAL_QA_WRITE;
import static dev.jianwii.entity.basicdata.Permission.MEETING_WRITE;
import static dev.jianwii.entity.basicdata.Permission.PROJECT_READ;
import static dev.jianwii.entity.basicdata.Permission.PROJECT_WRITE;
import static dev.jianwii.entity.basicdata.Permission.TASK_WRITE;
import static dev.jianwii.entity.basicdata.Permission.WORK_LOG_WRITE;

import dev.jianwii.basicdata.dao.RoleDAO;
import dev.jianwii.basicdata.service.RoleService;
import dev.jianwii.basicdata.service.UserService;
import dev.jianwii.entity.basicdata.Permission;
import dev.jianwii.entity.basicdata.Role;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 啟動時建立預設角色；已存在的不覆蓋（可能已被管理者調整過，含已刪除的也不重建）。 預設只提供一般成員、主管與系統管理員；各角色包含完整的工作權限，不必疊加。 功能權限與資料範圍仍由各 API
 * 檢查，主管不會因此取得所有專案或所有請假資料。 新增權限不會自動寫入既有角色，由管理員調整。
 */
@Component
@AllArgsConstructor
public class RoleSetup implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(RoleSetup.class);

    private final RoleDAO roleDAO;
    private final RoleService roleService;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        // ponytail: 三個日常角色就足夠；細部授權沿用 Permission，不再替每項功能建立角色。
        if (createIfAbsent(
                Role.DEFAULT_CODE,
                "一般成員",
                PROJECT_READ,
                TASK_WRITE,
                MEETING_WRITE,
                MANUAL_QA_WRITE,
                WORK_LOG_WRITE,
                LEAVE_APPLY)) {
            // 只在第一次建立時補發給之前登入過、還沒有任何角色的人；之後被拿掉所有角色的人不會再補
            int count = userService.assignDefaultRoleToUsersWithoutRoles();
            log.info("已將預設角色指派給 {} 位沒有角色的使用者", count);
        }
        // 保留既有代碼；主管只管理自己擔任經理的專案、審核指定給自己的請假。
        createIfAbsent(
                "PROJECT_MANAGER",
                "主管",
                PROJECT_READ,
                PROJECT_WRITE,
                TASK_WRITE,
                MEETING_WRITE,
                MANUAL_QA_WRITE,
                WORK_LOG_WRITE,
                LEAVE_APPLY,
                LEAVE_APPROVE);
        createIfAbsent("ADMIN", "系統管理員", Permission.values());
    }

    /** 回傳這次是否有建立。 */
    private boolean createIfAbsent(String code, String name, Permission... permissions) {
        if (roleDAO.countIncludingDeletedByCode(code) > 0) {
            return false;
        }
        Set<String> permissionCodes =
                Arrays.stream(permissions).map(Permission::getCode).collect(Collectors.toCollection(HashSet::new));
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setPermissionCodes(permissionCodes);
        roleService.create(role);
        return true;
    }
}
