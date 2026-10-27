package dev.jianwii.api.basicdata.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

/** 成員名單由 web-server 檢查專案權限後產生，不接受瀏覽器指定人員。 */
public record LeaveCalendarQuery(
        @NotEmpty @Size(max = 100) Set<@NotNull @Pattern(regexp = "[1-9]\\d{0,17}") String> userIds,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate) {}
