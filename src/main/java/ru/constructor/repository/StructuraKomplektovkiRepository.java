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
 *
 * ID
 * ID_KOMLECTOVKA
 * ID_SPECIF
 * ID_DIVISION
 * ID_ROOT_SPECIF
 * ID_ORIGINAL_STRUCTURE
 *
 * ID генерируется существующей PostgreSQL sequence.
 * Остальные компоненты ключа задаются при сохранении.
 */
@Repository
public interface StructuraKomplektovkiRepository
        extends JpaRepository<StructuraKomplektovkiEntity, StructuraKomplektovkiId> {

    /**
     * Получить ID из sequence для одного элемента.
     *
     * @return следующий идентификатор
     */
    @Query(value = """
            SELECT nextval('"Konstructor"."STRUCTURA_KOMPLECTOVKI_ID_seq"')
            """, nativeQuery = true)
    Long getNextId();

    /**
     * Получить несколько ID одним запросом.
     *
     * @param count количество идентификаторов
     * @return список идентификаторов
     */
    @Query(value = """
            SELECT nextval('"Konstructor"."STRUCTURA_KOMPLECTOVKI_ID_seq"')
            FROM generate_series(1, :count)
            """, nativeQuery = true)
    List<Long> getNextIds(@Param("count") int count);

    /**
     * Получить все элементы структуры конкретной КВ.
     *
     * @param komplektovkaId ID комплектовки
     * @return элементы структуры
     */
    List<StructuraKomplektovkiEntity>
    findByIdKomlectovkaOrderByLevelAscIdAsc(Integer komplektovkaId);
}