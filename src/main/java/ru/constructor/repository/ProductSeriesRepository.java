package ru.constructor.repository;

import ru.constructor.entity.ProductSeriesEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий серий комплектовок.
 *
 * Работает с существующей таблицей PRODUCT_SERIES.
 */
@Repository
public interface ProductSeriesRepository
        extends JpaRepository<ProductSeriesEntity, Integer> {

    /**
     * Получить серии одного элемента структуры.
     *
     * @param ssid ID элемента структуры
     * @return серии в порядке возрастания MINI
     */
    List<ProductSeriesEntity> findBySsidOrderByMiniAsc(Integer ssid);

    /**
     * Получить серии нескольких элементов структуры.
     *
     * Используется для пакетного чтения.
     *
     * @param ssids идентификаторы элементов структуры
     * @return найденные серии
     */
    List<ProductSeriesEntity> findBySsidInOrderBySsidAscMiniAsc(
            List<Integer> ssids
    );

    /**
     * Удалить серии элементов конкретной КВ.
     *
     * Удаляются только те записи PRODUCT_SERIES,
     * SSID которых соответствует ID элементов структуры
     * указанной комплектовки.
     *
     * Исходные серии PDM не должны затрагиваться.
     *
     * @param komplektovkaId ID комплектовки
     */
    @Modifying
    @Query(value = """
            DELETE FROM "Konstructor"."PRODUCT_SERIES" ps
            WHERE ps."SSID" IN (
                SELECT sk."ID"
                FROM "Konstructor"."STRUCTURA_KOMPLECTOVKI" sk
                WHERE sk."ID_KOMLECTOVKA" = :komplektovkaId
            )
            """, nativeQuery = true)
    void deleteByKomplektovkaId(
            @Param("komplektovkaId") Integer komplektovkaId
    );
}