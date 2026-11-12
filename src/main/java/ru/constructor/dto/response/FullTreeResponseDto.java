
package ru.constructor.dto.response;

import java.util.List;

public record FullTreeResponseDto(
        List<TemaNode> temas,
        long totalNodes,
        String version
) {

    public static FullTreeResponseDto empty() {
        return new FullTreeResponseDto(List.of(), 0, "1.0");
    }

    public record TemaNode(
            Integer id,
            String name,
            Integer indeksKd,
            List<IzdelieNode> izdelia
    ) {
    }

    public record IzdelieNode(
            Integer id,
            String name,
            String fullName,
            String dateAdd,
            List<GroupeNode> groupes
    ) {
    }

    public record GroupeNode(
            Integer id,
            String name
    ) {
    }
}
