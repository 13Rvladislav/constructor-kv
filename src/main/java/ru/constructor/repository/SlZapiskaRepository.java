
package ru.constructor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.constructor.entity.Specif22Entity;

/**
 * Репозиторий проверок, связанных со служебными записками.
 *
 * Для текущего этапа используются только native-запросы:
 * - проверка существования и согласования СЗ;
 * - проверка принадлежности сборки служебной записке.
 *
 * В качестве базовой сущности используется Specif22Entity,
 * поскольку отдельная Entity для SL_ZAPISKA пока не создаётся.
 */
@Repository
public interface SlZapiskaRepository
        extends JpaRepository<Specif22Entity, Integer> {

    /**
     * Проверяет, существует ли служебная записка
     * и завершено ли её согласование.
     *
     * Связь таблиц:
     * SL_ZAPISKA.SOST_SOGL_ID -> STATE_SOGLAS.ID.
     *
     * На текущем этапе финальным признаком согласования
     * считается заполненная дата STATE_SOGLAS.DATE_APPROVED.
     *
     * Это правило необходимо сверить со старой реализацией.
     *
     * @param slZapiskaId идентификатор служебной записки
     * @return true, если СЗ существует и дата финального согласования заполнена
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM "Konstructor"."SL_ZAPISKA" sz
                JOIN "Konstructor"."STATE_SOGLAS" ss
                  ON ss."ID" = sz."SOST_SOGL_ID"
                WHERE sz."ID" = :slZapiskaId
                  AND ss."DATE_APPROVED" IS NOT NULL
            )
            """, nativeQuery = true)
    boolean isSlZapiskaAgreed(
            @Param("slZapiskaId") Integer slZapiskaId
    );

    /**
     * Проверяет, входит ли выбранная сборка в состав
     * указанной служебной записки.
     *
     * В SBORKI_SL_ZAP.ID_SPECIF может храниться несколько
     * идентификаторов через запятую, например:
     *
     * 3,35,4,9
     *
     * Поэтому строка разбивается на отдельные значения.
     * Пробелы удаляются до преобразования в integer.
     *
     * Проверка выполняется по всем строкам SBORKI_SL_ZAP,
     * принадлежащим указанной служебной записке.
     *
     * @param slZapiskaId идентификатор служебной записки
     * @param specifId идентификатор сборки
     * @return true, если сборка присутствует в СЗ
     */
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM "Konstructor"."SBORKI_SL_ZAP" s
            CROSS JOIN LATERAL
                unnest(
                    string_to_array(
                        regexp_replace(s."ID_SPECIF", '\\s', '', 'g'),
                        ','
                    )
                ) AS ids(assembly_id)
            WHERE s."ID_SL_ZAP" = :slZapiskaId
              AND ids.assembly_id <> ''
              AND ids.assembly_id::integer = :specifId
        )
        """, nativeQuery = true)
    boolean isAssemblyBelongsToSlZapiska(
            @Param("slZapiskaId") Integer slZapiskaId,
            @Param("specifId") Integer specifId
    );
}
