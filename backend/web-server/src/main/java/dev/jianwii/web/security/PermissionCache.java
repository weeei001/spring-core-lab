package dev.jianwii.web.security;

import dev.jianwii.api.basicdata.UserApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登入者的功能權限快取：每個使用者最多 1 分鐘向 basic-data 查一次。
 * 在這台 web-server 調整角色時會立即清掉相關快取；開多台時，其他台最晚 1 分鐘後生效。
 */
@Component
public class PermissionCache {

    private static final Logger log = LoggerFactory.getLogger(PermissionCache.class);
    private static final Duration TTL = Duration.ofMinutes(1);

    private final UserApi userApi;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public PermissionCache(UserApi userApi) {
        this.userApi = userApi;
    }

    public Set<String> get(String userId) {
        Entry entry = entries.get(userId);
        if (entry != null && entry.loadedAt().plus(TTL).isAfter(Instant.now())) {
            return entry.permissions();
        }
        try {
            Set<String> permissions = Set.copyOf(userApi.permissions(Long.valueOf(userId)));
            entries.put(userId, new Entry(permissions, Instant.now()));
            return permissions;
        } catch (RuntimeException e) {
            // basic-data 暫時連不上時沿用過期的權限，避免所有人突然沒權限
            if (entry == null) {
                throw e;
            }
            log.warn("載入權限失敗，沿用快取，userId：{}", userId, e);
            return entry.permissions();
        }
    }

    /** 某個使用者的角色改了。 */
    public void evict(String userId) {
        entries.remove(userId);
    }

    /** 角色的權限改了，影響的使用者不確定，全部清掉。 */
    public void clear() {
        entries.clear();
    }

    private record Entry(Set<String> permissions, Instant loadedAt) {
    }
}
