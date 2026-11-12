package ru.constructor.dto.response;

import java.util.List;

/**
 * Ответ со сводной таблицей комплектовочных ведомостей.
 */
public record KvTableResponseDto(
        GroupInfo groupInfo,
        List<KvTableRowDto> rows,
        int totalRows,
        String version
) {

    public static KvTableResponseDto empty(GroupInfo groupInfo) {
        return new KvTableResponseDto(
                groupInfo,
                List.of(),
                0,
                "1.0"
        );
    }

    public record GroupInfo(
            Integer temaId,
            String temaName,
            Integer izdelieId,
            String izdelieName,
            Integer groupeId
    ) {
    }

    public record KvTableRowDto(
            Integer number,
            Integer slZapiskaId,
            String slZapiskaObozn,
            String komplektovkaDesign,
            Integer komplektovkaId,
            Integer specifId,
            String slZapAgreedDate,
            String status,
            String parentKvDesign,
            Integer kvStatusId,
            Boolean isSlZapAgreed,
            Boolean hasKv
    ) {
    }
}