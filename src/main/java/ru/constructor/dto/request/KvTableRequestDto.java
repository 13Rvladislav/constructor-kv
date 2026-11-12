package ru.constructor.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record KvTableRequestDto(

        @NotNull(message = "Не указан идентификатор темы")
        @Positive(message = "Идентификатор темы должен быть положительным")
        Integer temaId,

        @NotNull(message = "Не указан идентификатор изделия")
        @Positive(message = "Идентификатор изделия должен быть положительным")
        Integer izdelieId,

        @NotNull(message = "Не указан идентификатор группы")
        @Positive(message = "Идентификатор группы должен быть положительным")
        Integer groupeId

) {
}