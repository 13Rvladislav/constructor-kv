
package ru.constructor.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
        Integer groupeId,

        String search,

        String status,

        Boolean hasKv,

        @Min(value = 0, message = "Номер страницы не может быть отрицательным")
        Integer page,

        @Min(value = 1, message = "Размер страницы должен быть больше нуля")
        @Max(value = 200, message = "Размер страницы не может превышать 200")
        Integer size

) {
        public int resolvedPage() {
                return page == null ? 0 : page;
        }

        public int resolvedSize() {
                return size == null ? 20 : size;
        }

        public String normalizedSearch() {
                return search == null ? "" : search.trim();
        }

        public String normalizedStatus() {
                return status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                        ? null
                        : status.trim();
        }
}
