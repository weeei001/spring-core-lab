package dev.jianwii.api.project;

import dev.jianwii.common.dto.PageResponse;
import dev.jianwii.common.validation.ValidGroup;
import dev.jianwii.entity.project.Project;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * project-service 的專案 API（Spring HTTP Interface），提供端與呼叫端共用同一個介面：
 *
 * <ul>
 * <li>提供端：project-service 的 ProjectController implements 此介面，這裡的路徑與驗證註解直接生效。</li>
 * <li>呼叫端：啟動類別加 {@code @ImportHttpServices(group = "project-service", types = ProjectApi.class)}
 * 後即可注入 ProjectApi；前端經 web-server 的 WebProjectController 呼叫，由它檢查權限並包成 Result。</li>
 * </ul>
 *
 * project-service 不檢查權限與成員資格，由 web-server 負責（見 ProjectAccessGuard）。 成功時直接回傳物件，不包 Result；失敗時呼叫端會收到
 * RemoteServiceException。
 */
@HttpExchange("/api/project")
public interface ProjectApi {

    /** 不分頁查詢：page 必須為 1，最多回傳 size 筆。結果帶任務數、完成數、逾期數。 */
    @PostExchange("/query")
    List<Project> query(@Valid @RequestBody ProjectQueryParameter parameter);

    /** 分頁查詢，page 從 1 起算。結果帶任務數、完成數、逾期數。 */
    @PostExchange("/queryPage")
    PageResponse<Project> queryPage(@Valid @RequestBody ProjectQueryParameter parameter);

    /** 新增；code 由系統取號，狀態不給為 PLANNING。建立者（X-Operator）自動成為專案經理。 */
    @PostExchange
    Project create(@Validated(ValidGroup.Create.class) @RequestBody Project project);

    /** 部分更新：只更新有給值（非 null）的欄位。結案或取消的專案只能改回 ACTIVE（重新開啟）。 */
    @PatchExchange("/{id}")
    Project update(@PathVariable("id") Long id,
            @Validated(ValidGroup.Update.class) @RequestBody Project project);

    /** 軟刪除；仍有任務時回 409。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}
