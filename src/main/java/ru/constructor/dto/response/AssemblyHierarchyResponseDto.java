package ru.constructor.dto.response;

import java.util.List;

/**
 * Узел дерева предпросмотра комплектовочной ведомости.
 */
public record AssemblyHierarchyResponseDto(
        Integer id,
        Integer idSpecif,
        Integer parentId,
        String obozn,
        String naim,
        Integer quantity,
        Double massa,
        Integer idDivision,
        String material,
        String note,
        Integer level,
        Integer structureId,
        Integer izdelieNumber,
        Integer izdelieId,
        Boolean fitsSeries,
        String fitReason,
        List<SeriesInfo> series,
        List<AssemblyHierarchyResponseDto> children
) {

    public record SeriesInfo(
            Integer mini,
            Integer maxi,
            Integer quantity,
            Double massa,
            String dimensions
    ) {
    }
}