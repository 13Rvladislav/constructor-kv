
package ru.constructor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.constructor.entity.Specif22Entity;
import ru.constructor.repository.projection.KvTableRowProjection;

import java.util.List;

@Repository
public interface KvTableRepository extends JpaRepository<Specif22Entity, Integer> {

    String TABLE_CTE = """
            WITH RECURSIVE root_spec AS (
                SELECT s."ID"
                FROM "Konstructor"."SPECIF22" s
                WHERE s."ID_TEMA" = :temaId
                  AND s."ID_IZDELIE" = :izdelieId
                  AND s."ID_GROUPE" = :groupeId
                  AND s."ID_PARENT" IS NULL
                ORDER BY s."ID" DESC
                LIMIT 1
            ),
            spec_tree AS (
                SELECT r."ID" AS specif_id
                FROM root_spec r

                UNION ALL

                SELECT child."ID"
                FROM "Konstructor"."SPECIF22" child
                INNER JOIN spec_tree parent
                    ON child."ID_PARENT" = parent.specif_id
                WHERE child."ID_TEMA" = :temaId
                  AND child."ID_IZDELIE" = :izdelieId
                  AND child."ID_GROUPE" = :groupeId
            ),
            matched_sl_zap AS (
                SELECT DISTINCT
                    sl."ID" AS sl_zap_id,
                    sl."OBOZN" AS sl_zap_obozn,
                    ssz."ID_SPECIF" AS specif_ids_raw
                FROM "Konstructor"."SBORKI_SL_ZAP" ssz
                INNER JOIN "Konstructor"."IZDEL_SL_ZAP" iz
                    ON iz."ID" = ssz."ID_IZDEL_SL_ZAP"
                INNER JOIN "Konstructor"."SL_ZAPISKA" sl
                    ON sl."ID" = iz."ID_SL_ZAP"
                WHERE EXISTS (
                    SELECT 1
                    FROM unnest(string_to_array(ssz."ID_SPECIF", ','))
                        AS raw_specif_id(value)
                    INNER JOIN spec_tree tree
                        ON tree.specif_id = CASE
                            WHEN btrim(raw_specif_id.value) ~ '^[0-9]+$'
                            THEN btrim(raw_specif_id.value)::integer
                            ELSE NULL
                        END
                )
            ),
            expanded_specif AS (
                SELECT DISTINCT
                    matched.sl_zap_id,
                    matched.sl_zap_obozn,
                    btrim(raw_specif_id.value)::integer AS specif_id
                FROM matched_sl_zap matched
                CROSS JOIN LATERAL unnest(
                    string_to_array(matched.specif_ids_raw, ',')
                ) AS raw_specif_id(value)
                WHERE btrim(raw_specif_id.value) ~ '^[0-9]+$'
            ),
            agreed_sl_zap AS (
                SELECT
                    sl."ID" AS sl_zap_id,
                    state."DATE_APPROVED" AS agreed_date
                FROM "Konstructor"."SL_ZAPISKA" sl
                LEFT JOIN "Konstructor"."STATE_SOGLAS" state
                    ON state."ID" = sl."SOST_SOGL_ID"
                WHERE sl."SOST_SOGL_ID" IS NOT NULL
                  AND state."DATE_APPROVED" IS NOT NULL
            ),
            table_rows AS (
                SELECT
                    expanded.sl_zap_id AS "slZapiskaId",
                    COALESCE(
                        expanded.sl_zap_obozn,
                        'Без обозн.'
                    ) AS "slZapiskaObozn",
                    expanded.specif_id AS "specifId",
                    specif."OBOZN" AS "specifObozn",
                    kv."ID" AS "komplektovkaId",
                    COALESCE(
                        NULLIF(kv."DESIGN", ''),
                        'КВ-' || COALESCE(
                            NULLIF(specif."OBOZN", ''),
                            'СБ-' || expanded.specif_id::text
                        )
                    ) AS "komplektovkaDesign",
                    CASE
                        WHEN kv."PARENT_KV_ID" IS NULL THEN NULL
                        ELSE COALESCE(
                            NULLIF(parent_kv."DESIGN", ''),
                            'КВ-' || parent_kv."ID"::text,
                            '—'
                        )
                    END AS "parentKvDesign",
                    kv."KV_STATUS_ID" AS "kvStatusId",
                    CASE
                        WHEN kv."ID" IS NULL
                            THEN 'Не сформирована'
                        WHEN kv."KV_STATUS_ID" = 1
                            THEN 'В работе'
                        WHEN kv."KV_STATUS_ID" = 4
                            THEN 'Запущена в производство'
                        WHEN kv."KV_STATUS_ID" = 5
                            THEN 'Аннулирована'
                        WHEN kv."DATE_ARCHIVING" IS NOT NULL
                            THEN 'Аннулирована'
                        WHEN kv."APPROVED_DATE" IS NOT NULL
                            THEN 'Запущена в производство'
                        WHEN kv."DATE_CREATE" IS NOT NULL
                            THEN 'В работе'
                        ELSE 'Не сформирована'
                    END AS "status",
                    agreed.agreed_date AS "slZapAgreedDate",
                    (kv."ID" IS NOT NULL) AS "hasKv"
                FROM expanded_specif expanded
                INNER JOIN agreed_sl_zap agreed
                    ON agreed.sl_zap_id = expanded.sl_zap_id
                LEFT JOIN "Konstructor"."SPECIF22" specif
                    ON specif."ID" = expanded.specif_id
                LEFT JOIN LATERAL (
                    SELECT k.*
                    FROM "Konstructor"."KOMPLECTOVKA" k
                    WHERE k."ID_SZ" = expanded.sl_zap_id
                      AND k."SBORKI_SZ" = expanded.specif_id::text
                    ORDER BY
                        k."DATE_CREATE" DESC NULLS LAST,
                        k."ID" DESC
                    LIMIT 1
                ) kv ON TRUE
                LEFT JOIN "Konstructor"."KOMPLECTOVKA" parent_kv
                    ON parent_kv."ID" = kv."PARENT_KV_ID"
            ),
            filtered_rows AS (
                SELECT *
                FROM table_rows r
                WHERE
                    (
                        CAST(:search AS text) IS NULL
                        OR LOWER(COALESCE(r."slZapiskaId"::text, ''))
                            LIKE '%' || LOWER(CAST(:search AS text)) || '%'
                        OR LOWER(COALESCE(r."slZapiskaObozn", ''))
                            LIKE '%' || LOWER(CAST(:search AS text)) || '%'
                        OR LOWER(COALESCE(r."specifId"::text, ''))
                            LIKE '%' || LOWER(CAST(:search AS text)) || '%'
                        OR LOWER(COALESCE(r."specifObozn", ''))
                            LIKE '%' || LOWER(CAST(:search AS text)) || '%'
                        OR LOWER(COALESCE(r."komplektovkaId"::text, ''))
                            LIKE '%' || LOWER(CAST(:search AS text)) || '%'
                        OR LOWER(COALESCE(r."komplektovkaDesign", ''))
                            LIKE '%' || LOWER(CAST(:search AS text)) || '%'
                        OR LOWER(COALESCE(r."parentKvDesign", ''))
                            LIKE '%' || LOWER(CAST(:search AS text)) || '%'
                    )
                    AND (
                        CAST(:status AS text) IS NULL
                        OR r."status" = CAST(:status AS text)
                    )
                    AND (
                        CAST(:hasKv AS boolean) IS NULL
                        OR r."hasKv" = CAST(:hasKv AS boolean)
                    )
            )
            """;

    @Query(
            value = TABLE_CTE + """
                    SELECT
                        ROW_NUMBER() OVER (
                            ORDER BY
                                r."slZapiskaId",
                                r."specifId"
                        ) AS "number",
                        r."slZapiskaId",
                        r."slZapiskaObozn",
                        r."specifId",
                        r."specifObozn",
                        r."komplektovkaId",
                        r."komplektovkaDesign",
                        r."parentKvDesign",
                        r."kvStatusId",
                        r."status",
                        r."slZapAgreedDate",
                        r."hasKv"
                    FROM filtered_rows r
                    ORDER BY
                        r."slZapiskaId",
                        r."specifId"
                    LIMIT :limit
                    OFFSET :offset
                    """,
            nativeQuery = true
    )
    List<KvTableRowProjection> findFilteredTableRows(
            @Param("temaId") Integer temaId,
            @Param("izdelieId") Integer izdelieId,
            @Param("groupeId") Integer groupeId,
            @Param("search") String search,
            @Param("status") String status,
            @Param("hasKv") Boolean hasKv,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    @Query(
            value = TABLE_CTE + """
                    SELECT COUNT(*)
                    FROM filtered_rows
                    """,
            nativeQuery = true
    )
    long countFilteredTableRows(
            @Param("temaId") Integer temaId,
            @Param("izdelieId") Integer izdelieId,
            @Param("groupeId") Integer groupeId,
            @Param("search") String search,
            @Param("status") String status,
            @Param("hasKv") Boolean hasKv
    );

    @Query(
            value = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM "Konstructor"."SPECIF22" s
                        WHERE s."ID_TEMA" = :temaId
                          AND s."ID_IZDELIE" = :izdelieId
                          AND s."ID_GROUPE" = :groupeId
                          AND s."ID_PARENT" IS NULL
                    )
                    """,
            nativeQuery = true
    )
    boolean existsRootSpecification(
            @Param("temaId") Integer temaId,
            @Param("izdelieId") Integer izdelieId,
            @Param("groupeId") Integer groupeId
    );
}
