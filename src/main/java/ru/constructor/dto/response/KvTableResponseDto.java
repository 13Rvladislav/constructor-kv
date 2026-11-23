
package ru.constructor.dto.response;

import java.util.List;

public record KvTableResponseDto(
        GroupInfo groupInfo,
        List<KvTableRowDto> rows,
        int totalRows,
        String version,
        int page,
        int size,
        int totalPages,
        List<String> statusOptions
) {

    public static KvTableResponseDto empty(GroupInfo groupInfo) {
        return new KvTableResponseDto(
                groupInfo,
                List.of(),
                0,
                "1.0",
                0,
                20,
                0,
                List.of()
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
