package com.wonder.electricals.inventory.dto;

import jakarta.validation.constraints.NotNull;

/** Toggles the {@code active} flag without sending the whole record back. */
public class StatusUpdateRequest {

    @NotNull(message = "active is required")
    private Boolean active;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
