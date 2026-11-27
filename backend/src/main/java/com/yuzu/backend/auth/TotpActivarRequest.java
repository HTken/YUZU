package com.yuzu.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record TotpActivarRequest(
        @NotBlank String secreto,
        @NotBlank String codigo) {
}
