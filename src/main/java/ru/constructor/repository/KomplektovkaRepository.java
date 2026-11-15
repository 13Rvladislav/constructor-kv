
package ru.constructor.repository;

import ru.constructor.entity.KomplektovkaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с таблицей KOMPLECTOVKA.
 *
 * Предоставляет операции:
 * - сохранение комплектовок;
 * - получение комплектовок;
 * - поиск комплектовки по служебной записке;
 * - проверка существования комплектовки для сборки.
 *
 * JpaRepository уже предоставляет стандартные операции CRUD,
 * поэтому вручную реализовывать их не требуется.
 */
@Repository
public interface KomplektovkaRepository
        extends JpaRepository<KomplektovkaEntity, Integer> {

    /**
     * Получить все комплектовки по идентификатору служебной записки.
     *
     * Результат сортируется по дате создания:
     * сначала новые комплектовки.
     *
     * Spring Data JPA автоматически формирует SQL
     * на основании названия метода.
     *
     * @param idSz идентификатор служебной записки
     * @return список комплектовок
     */
    List<KomplektovkaEntity> findByIdSzOrderByDateCreateDesc(Integer idSz);

    /**
     * Найти действующую комплектовку для конкретной сборки
     * в рамках указанной служебной записки.
     *
     * Комплектовка считается действующей, если DATE_ARCHIVING IS NULL.
     *
     * Используется native SQL, поскольку:
     * - необходимо явно обратиться к схеме Konstructor;
     * - SBORKI_SZ хранится в БД как VARCHAR;
     * - требуется ограничить результат одной записью.
     *
     * @param slZapiskaId идентификатор служебной записки
     * @param specifId идентификатор сборки
     * @return найденная комплектовка или Optional.empty()
     */
    @Query(value = """
            SELECT k.*
            FROM "Konstructor"."KOMPLECTOVKA" k
            WHERE k."ID_SZ" = :slZapiskaId
              AND k."SBORKI_SZ" = CAST(:specifId AS varchar)
              AND k."DATE_ARCHIVING" IS NULL
            ORDER BY k."ID" DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<KomplektovkaEntity> findActiveBySlZapiskaAndSpecif(
            @Param("slZapiskaId") Integer slZapiskaId,
            @Param("specifId") Integer specifId
    );

    /**
     * Получить идентификаторы сборок, для которых уже сформированы
     * действующие комплектовки в рамках служебной записки.
     *
     * Используется при построении таблицы комплектовок
     * и проверке повторного формирования КВ.
     *
     * Из результата исключаются:
     * - архивированные комплектовки;
     * - записи без идентификатора сборки;
     * - пустые значения SBORKI_SZ.
     *
     * В текущей реализации SBORKI_SZ содержит один ID сборки
     * в виде строки. Если в актуальной БД встречаются значения
     * с несколькими ID через запятую, этот запрос необходимо
     * будет заменить на разбор string_to_array() и unnest().
     *
     * @param slZapiskaId идентификатор служебной записки
     * @return список идентификаторов сборок с существующими КВ
     */
    @Query(value = """
            SELECT DISTINCT CAST(k."SBORKI_SZ" AS integer)
            FROM "Konstructor"."KOMPLECTOVKA" k
            WHERE k."ID_SZ" = :slZapiskaId
              AND k."DATE_ARCHIVING" IS NULL
              AND k."SBORKI_SZ" IS NOT NULL
              AND k."SBORKI_SZ" <> ''
            """, nativeQuery = true)
    List<Integer> findExistingSpecifIdsBySlZap(
            @Param("slZapiskaId") Integer slZapiskaId
    );
}
