
package ru.constructor.repository;

import ru.constructor.entity.ProductSeriesEntity;

import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Репозиторий только для чтения PRODUCT_SERIES.
 *
 * PRODUCT_SERIES относится к конструкторскому блоку.
 * Изменение исходных данных через этот репозиторий запрещено.
 *
 * Доступны исключительно операции чтения.
 */
public interface ProductSeriesRepository
        extends Repository<ProductSeriesEntity, Integer> {

    /**
     * Получить серии одного исходного элемента.
     *
     * @param ssid идентификатор исходного элемента
     * @return серии в порядке возрастания MINI
     */
    List<ProductSeriesEntity> findBySsidOrderByMiniAsc(
            Integer ssid
    );

    /**
     * Получить серии нескольких исходных элементов.
     *
     * @param ssids идентификаторы исходных элементов
     * @return найденные серии
     */
    List<ProductSeriesEntity> findBySsidInOrderBySsidAscMiniAsc(
            List<Integer> ssids
    );
}
