
package ru.constructor.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Запрос на формирование комплектовки.
 *
 * Передаётся клиентом при нажатии кнопки формирования КВ.
 *
 * Для формирования необходимо указать:
 * - служебную записку;
 * - сборку, для которой создаётся комплектовка;
 * - пользователя, выполняющего операцию.
 */
public record KomplektovkaFormRequestDto(

        /**
         * Идентификатор служебной записки.
         */
        @NotNull(message = "Необходимо указать служебную записку")
        @Positive(message = "ID служебной записки должен быть положительным")
        Integer slZapiskaId,

        /**
         * Идентификатор сборки из спецификации.
         */
        @NotNull(message = "Необходимо указать сборку")
        @Positive(message = "ID сборки должен быть положительным")
        Integer specifId,

        /**
         * Идентификатор пользователя, формирующего КВ.
         */
        @NotNull(message = "Необходимо указать пользователя")
        @Positive(message = "ID пользователя должен быть положительным")
        Integer formedBy

) {
}
