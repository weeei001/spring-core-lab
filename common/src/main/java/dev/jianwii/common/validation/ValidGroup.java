package dev.jianwii.common.validation;

import jakarta.validation.groups.Default;

/**
 * Bean Validation 分組，讓同一個 Entity 在新增與更新時套用不同規則。
 *
 * <ul>
 * <li>{@code @NotBlank(groups = ValidGroup.Create.class)}：只在新增時檢查（必填）。</li>
 * <li>沒寫 groups 的規則（@Size、@Pattern 等）屬於 Default；各分組都繼承 Default，所以新增、更新都會檢查。</li>
 * <li>Controller 以 {@code @Validated(ValidGroup.Create.class)} 指定這次要套哪一組。</li>
 * </ul>
 * 大部分規則遇到 null 會略過，所以更新時欄位不給（null）就是不改，不會被 @Size 等擋下。
 */
public interface ValidGroup {

    interface Create extends Default {
    }

    interface Update extends Default {
    }

    interface Delete extends Default {
    }

    interface Submit extends Default {
    }
}
