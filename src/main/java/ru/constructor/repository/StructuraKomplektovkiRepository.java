package ru.constructor.repository;

import ru.constructor.entity.StructuraKomplektovkiEntity;
import ru.constructor.entity.StructuraKomplektovkiId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий структуры комплектовок.
 *
 * Таблица STRUCTURA_KOMPLECTOVKI имеет составной первичный ключ:
 * ID, ID_KOMLECTOVKA, ID_SPECIF, ID_DIVISION,
 * ID_ROOT_SPECIF, ID_ORIGINAL_STRUCTURE.
 */
@Repository
public interface StructuraKomplektovkiRepository
        extends JpaRepository<StructuraKomplektovkiEntity, StructuraKomplektovkiId> {

    @Query(value = """
            SELECT nextval('"Konstructor"."STRUCTURA_KOMPLECTOVKI_ID_seq"')
            """, nativeQuery = true)
    Long getNextId();

    @Query(value = """
            SELECT nextval('"Konstructor"."STRUCTURA_KOMPLECTOVKI_ID_seq"')
            FROM generate_series(1, :count)
            """, nativeQuery = true)
    List<Long> getNextIds(@Param("count") int count);

    /**
     * Получить все элементы структуры конкретной КВ.
     */
    List<StructuraKomplektovkiEntity>
    findByIdKomlectovkaOrderByLevelAscIdAsc(Integer komplektovkaId);

    /**
     * Найти строки сформированной структуры, связанные
     * с исходными строками STRUCTURE_SPECIF.
     *
     * Нужен для перехода:
     * ID_ORIGINAL_STRUCTURE -> ID -> PRODUCT_SERIES.SSID.
     */
    List<StructuraKomplektovkiEntity> findByIdOriginalStructureIn(
            List<Integer> originalStructureIds
    );
}
