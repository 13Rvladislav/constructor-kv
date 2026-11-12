
package ru.constructor.dto.response;

import java.util.List;

/**
 * Ответ с информацией о темах.
 *
 * Используется для получения списка тем и поиска по ним.
 */
public record TemaInfoResponseDto(

        List<TemaInfoNode> temas,

        int totalNodes,

        String version

) {

    /**
     * Элемент списка тем.
     */
    public record TemaInfoNode(

            Integer id,

            String name,

            Integer indeksKd

    ) {
    }

    /**
     * Возвращает пустой ответ.
     */
    public static TemaInfoResponseDto empty() {
        return new TemaInfoResponseDto(
                List.of(),
                0,
                "1.0"
        );
    }
}
