package dev.jianwii.common.validation;

import jakarta.validation.groups.Default;

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
