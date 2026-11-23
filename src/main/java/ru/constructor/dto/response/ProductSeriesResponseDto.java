
package ru.constructor.dto.response;

/**
 * Серия элемента спецификации.
 *
 * Данные берутся из PRODUCT_SERIES.
 */
public record ProductSeriesResponseDto(
        Integer id,
        Integer ssid,
        Integer mini,
        Integer maxi,
        Integer quantity,
        Double massa,
        String dimensions
) {
}
