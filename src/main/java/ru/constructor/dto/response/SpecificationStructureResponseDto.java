
package ru.constructor.dto.response;

import java.util.List;

/**
 * Строка полной структуры спецификации.
 *
 * Вложенный список series содержит все серии,
 * связанные с STRUCTURE_SPECIF.ID через PRODUCT_SERIES.SSID.
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
        Double massa,
        String material,
        Boolean flagProductSeries,
        Integer idNomenklatura,
        Integer idSpecif22,
        List<ProductSeriesResponseDto> series
) {
}
