package dev.jianwii.project.service;

import dev.jianwii.common.entity.BaseEntity;
import dev.jianwii.entity.project.TaskActivity;
import dev.jianwii.entity.project.TaskActivityType;
import dev.jianwii.project.dao.TaskActivityDAO;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 寫入任務活動紀錄，由任務、留言、成員的 Service 在同一個交易內呼叫，異動失敗時紀錄一起 rollback。 操作者與時間由 JPA Auditing 填入
 * createId、createTime。
 */
@Component
@AllArgsConstructor
public class TaskActivityRecorder {
    private final TaskActivityDAO taskActivityDAO;

    public void created(Long taskId) {
        save(taskId, TaskActivityType.CREATED, null, null, null);
    }

    public void commented(Long taskId) {
        save(taskId, TaskActivityType.COMMENTED, null, null, null);
    }

    /** 欄位變更；新舊值相同時不記錄。值轉成文字：enum 為名稱、日期為 yyyy-MM-dd。 */
    public void changed(Long taskId, String field, Object oldValue, Object newValue) {
        String oldText = text(oldValue);
        String newText = text(newValue);
        if (!Objects.equals(oldText, newText)) {
            save(taskId, TaskActivityType.CHANGED, field, oldText, newText);
        }
    }

    private void save(Long taskId, TaskActivityType type, String field, String oldValue, String newValue) {
        TaskActivity activity = new TaskActivity();
        activity.setTaskId(taskId);
        activity.setType(type);
        activity.setField(field);
        activity.setOldValue(oldValue);
        activity.setNewValue(newValue);
        activity.setDbMark(BaseEntity.VALID);
        taskActivityDAO.save(activity);
    }

    /** 欄位上限 500 字；標題最長 200，不會被截斷，這裡只是保險。 */
    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.length() > 500 ? text.substring(0, 500) : text;
    }
}
