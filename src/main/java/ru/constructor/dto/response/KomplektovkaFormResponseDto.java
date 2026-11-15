
package ru.constructor.dto.response;

import java.time.LocalDateTime;

/**
 * Результат успешного формирования комплектовки.
 *
 * Возвращается после создания записи KOMPLECTOVKA
 * и сохранения её структуры.
 */
public record KomplektovkaFormResponseDto(

        /**
         * ID созданной комплектовки.
         */
        Integer id,

        /**
         * Обозначение созданной комплектовки.
         */
        String design,

        /**
         * ID служебной записки.
         */
        Integer slZapiskaId,

        /**
         * ID сборки.
         */
        Integer specifId,

        /**
         * Дата создания комплектовки.
         */
        LocalDateTime dateCreate,

        /**
         * Количество сохранённых элементов структуры.
         */
        Integer structureElementsCount

) {
}
