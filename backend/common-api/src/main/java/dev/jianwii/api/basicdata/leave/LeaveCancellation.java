package dev.jianwii.api.basicdata.leave;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 撤回與整筆銷假都保留原因。 */
public record LeaveCancellation(@NotBlank @Size(max = 1000) String reason) {}
