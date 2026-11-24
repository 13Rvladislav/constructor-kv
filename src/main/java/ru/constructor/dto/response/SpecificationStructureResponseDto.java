package ru.constructor.dto.response;

import java.util.List;

/**
 * Строка полной структуры спецификации.
 *
 * divisionName — название подразделения из справочника DIVISION.
 *
 * series — список серий, связанных с данной строкой структуры.
 */
public record SpecificationStructureResponseDto(

        Integer id,

        Integer idSpecif,

        String zone,

        String format,

        String pos,

        String obozn,

        String naim,

        Integer quantity,

        Short idDivision,

        String divisionName,

        Double massa,

        String material,

        Boolean flagProductSeries,

        Integer idNomenklatura,

        Integer idSpecif22,

        List<ProductSeriesResponseDto> series

) {
}