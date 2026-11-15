package ru.constructor.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssemblyPreviewRequestDto(

        @NotNull(message = "Не указан ID спецификации")
        @Positive(message = "ID спецификации должен быть положительным")
        Integer specifId

) {
}