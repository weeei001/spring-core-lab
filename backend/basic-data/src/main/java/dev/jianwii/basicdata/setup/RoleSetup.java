package dev.jianwii.basicdata.setup;

import dev.jianwii.basicdata.dao.RoleDAO;
import dev.jianwii.basicdata.service.RoleService;
import dev.jianwii.basicdata.service.UserService;
import dev.jianwii.entity.basicdata.Permission;
import dev.jianwii.entity.basicdata.Role;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.jianwii.entity.basicdata.Permission.NOTIFICATION_SEND;
import static dev.jianwii.entity.basicdata.Permission.PROJECT_ADMIN;
import static dev.jianwii.entity.basicdata.Permission.PROJECT_READ;
import static dev.jianwii.entity.basicdata.Permission.PROJECT_WRITE;
import static dev.jianwii.entity.basicdata.Permission.ROLE_MANAGE;
import static dev.jianwii.entity.basicdata.Permission.SERIAL_NUMBER_READ;
import static dev.jianwii.entity.basicdata.Permission.SERIAL_NUMBER_WRITE;
import static dev.jianwii.entity.basicdata.Permission.TASK_WRITE;
import static dev.jianwii.entity.basicdata.Permission.WORK_LOG_WRITE;

/**
 * 啟動時建立預設角色；已存在的不覆蓋（可能已被管理者調整過，含已刪除的也不重建）。
 * 依職責拆分，一個人可同時擁有多個角色，例如系統管理員兼專案經理就指派兩個角色。
 * 之後新增的權限不會自動加進這些角色，要到角色管理畫面勾選。
 * 每個人都有預設角色（{@link Role#DEFAULT_CODE}），其他角色只放預設角色以外要多給的權限。
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
        // 所有人：只看得到自己參與的專案，新使用者第一次登入時自動擁有
        if (createIfAbsent(Role.DEFAULT_CODE, "一般使用者", PROJECT_READ)) {
            // 只在第一次建立時補發給之前登入過、還沒有任何角色的人；之後被拿掉所有角色的人不會再補
            int count = userService.assignDefaultRoleToUsersWithoutRoles();
            log.info("已將預設角色指派給 {} 位沒有角色的使用者", count);
        }
        // 系統設定：帳號角色、編號規則、公告；不含專案資料
        createIfAbsent("ADMIN", "系統管理員", ROLE_MANAGE, SERIAL_NUMBER_READ, SERIAL_NUMBER_WRITE,
                NOTIFICATION_SEND);
        // 跨專案管理：看得到並能維護所有專案，不必是成員
        createIfAbsent("PMO", "專案管理辦公室", PROJECT_READ, PROJECT_ADMIN, PROJECT_WRITE, TASK_WRITE);
        // 可建立專案；只在自己擔任經理的專案維護成員、里程碑
        createIfAbsent("PROJECT_MANAGER", "專案經理", PROJECT_READ, PROJECT_WRITE, TASK_WRITE, WORK_LOG_WRITE);
        // 在參與的專案做事：任務、留言、工時
        createIfAbsent("PROJECT_MEMBER", "專案成員", PROJECT_READ, TASK_WRITE, WORK_LOG_WRITE);
        // 主管、稽核：看得到所有專案，不能修改
        createIfAbsent("AUDITOR", "稽核", PROJECT_READ, PROJECT_ADMIN);
    }

    /** 回傳這次是否有建立。 */
    private boolean createIfAbsent(String code, String name, Permission... permissions) {
        if (roleDAO.existsByCode(code)) {
            return false;
        }
        Set<String> permissionCodes = Arrays.stream(permissions).map(Permission::getCode)
                .collect(Collectors.toCollection(HashSet::new));
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setPermissionCodes(permissionCodes);
        roleService.create(role);
        return true;
    }
}
