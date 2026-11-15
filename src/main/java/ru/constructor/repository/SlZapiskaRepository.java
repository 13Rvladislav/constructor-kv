
package ru.constructor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.constructor.entity.Specif22Entity;

/**
 * Репозиторий проверок, связанных со служебными записками.
 *
 * Используется для чтения SL_ZAPISKA,
 * STATE_SOGLAS и SBORKI_SL_ZAP.
 */
@Repository
public interface SlZapiskaRepository
        extends JpaRepository<Specif22Entity, Integer> {

    /**
     * Проверяет завершённость согласования служебной записки.
     *
     * Для успешного согласования должны быть заполнены:
     * - SOST_SOGL_ID;
     * - DATE_DEVELOPER;
     * - DATE_CHECKED_IT_OUT;
     * - DATE_HEAD_DEPARTMENT;
     * - DATE_NORM_CONTROL;
     * - DATE_APPROVED.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM "Konstructor"."SL_ZAPISKA" sz
                JOIN "Konstructor"."STATE_SOGLAS" ss
                    ON ss."ID" = sz."SOST_SOGL_ID"
                WHERE sz."ID" = :slZapiskaId
                  AND sz."SOST_SOGL_ID" IS NOT NULL
                  AND ss."DATE_DEVELOPER" IS NOT NULL
                  AND ss."DATE_CHECKED_IT_OUT" IS NOT NULL
                  AND ss."DATE_HEAD_DEPARTMENT" IS NOT NULL
                  AND ss."DATE_NORM_CONTROL" IS NOT NULL
                  AND ss."DATE_APPROVED" IS NOT NULL
            )
            """, nativeQuery = true)
    boolean isSlZapiskaAgreed(
            @Param("slZapiskaId") Integer slZapiskaId
    );

    /**
     * Проверяет, входит ли выбранная сборка
     * в состав указанной служебной записки.
     *
     * ID_SPECIF может содержать несколько ID через запятую.
     * Некорректные значения отбрасываются до преобразования
     * в integer.
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
                  AND ids.assembly_id ~ '^[0-9]+$'
                  AND ids.assembly_id::integer = :specifId
            )
            """, nativeQuery = true)
    boolean isAssemblyBelongsToSlZapiska(
            @Param("slZapiskaId") Integer slZapiskaId,
            @Param("specifId") Integer specifId
    );
}
