package ru.constructor.repository;

import ru.constructor.entity.Specif22Entity;
import ru.constructor.repository.projection.AssemblyContentProjection;
import ru.constructor.repository.projection.AssemblyHierarchyProjection;
import ru.constructor.repository.projection.ProductSeriesProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssemblyPreviewRepository
        extends JpaRepository<Specif22Entity, Integer> {

    /**
     * Получает иерархию сборок от корневой спецификации.
     *
     * Название подразделения берётся непосредственно из DIVISION.NAME.
     *
     * Для корневой спецификации используется подразделение ID = 3,
     * для вложенных сборок — ID_DIVISION из STRUCTURE_SPECIF.
     */
    @Query(value = """
            WITH RECURSIVE assembly_hierarchy AS (

                SELECT
                    s."ID" AS id,
                    CAST(NULL AS integer) AS parent_id,
                    s."OBOZN" AS obozn,
                    s."NAIM" AS naim,
                    1 AS quantity,
                    COALESCE(s."MASSA", 0) AS massa,
                    0 AS level,
                    CAST(NULL AS integer) AS structure_id,

                    CASE
                        WHEN btrim(izd."NAIM_IZD") ~ '^[0-9]+$'
                        THEN btrim(izd."NAIM_IZD")::integer
                        ELSE NULL
                    END AS izdelie_number,

                    izd."ID" AS izdelie_id,
                    3 AS id_division,
                    root_division."NAME" AS division_name,

                    ARRAY[s."ID"] AS path_ids

                FROM "Konstructor"."SPECIF22" s

                LEFT JOIN "Konstructor"."IZDELIE" izd
                    ON izd."ID" = s."ID_IZDELIE"

                LEFT JOIN "Konstructor"."DIVISION" root_division
                    ON root_division."ID" = 3

                WHERE s."ID" = :specifId

                UNION ALL

                SELECT
                    COALESCE(child."LINK_IMPLEMETATION", child."ID") AS id,

                    parent.id AS parent_id,

                    COALESCE(linked."OBOZN", child."OBOZN") AS obozn,

                    COALESCE(linked."NAIM", child."NAIM") AS naim,

                    COALESCE(st."QUANTITY", 1) AS quantity,

                    COALESCE(st."RMASSA", 0) AS massa,

                    parent.level + 1 AS level,

                    st."ID" AS structure_id,

                    parent.izdelie_number AS izdelie_number,

                    child."ID_IZDELIE" AS izdelie_id,

                    st."ID_DIVISION" AS id_division,

                    d."NAME" AS division_name,

                    parent.path_ids ||
                        COALESCE(child."LINK_IMPLEMETATION", child."ID")
                        AS path_ids

                FROM assembly_hierarchy parent

                INNER JOIN "Konstructor"."STRUCTURE_SPECIF" st
                    ON st."ID_SPECIF" = parent.id
                   AND st."ID_DIVISION" = 3

                INNER JOIN "Konstructor"."SPECIF22" child
                    ON child."OBOZN" = st."OBOZN"

                LEFT JOIN "Konstructor"."SPECIF22" linked
                    ON linked."ID" = child."LINK_IMPLEMETATION"

                LEFT JOIN "Konstructor"."DIVISION" d
                    ON d."ID" = st."ID_DIVISION"

                WHERE st."OBOZN" IS NOT NULL
                  AND btrim(st."OBOZN") <> ''

                  AND NOT (
                      COALESCE(child."LINK_IMPLEMETATION", child."ID")
                      = ANY(parent.path_ids)
                  )
            )

            SELECT
                id AS "id",
                parent_id AS "parentId",
                obozn AS "obozn",
                naim AS "naim",
                quantity AS "quantity",
                massa AS "massa",
                level AS "level",
                structure_id AS "structureId",
                izdelie_number AS "izdelieNumber",
                izdelie_id AS "izdelieId",
                id_division AS "idDivision",
                division_name AS "divisionName"

            FROM assembly_hierarchy

            ORDER BY level, obozn
            """, nativeQuery = true)
    List<AssemblyHierarchyProjection> findAssemblyHierarchy(
            @Param("specifId") Integer specifId
    );

    /**
     * Загружает содержимое сразу для всех сборок.
     *
     * ID_SPECIF — ID спецификации-владельца структуры.
     * Название подразделения берётся из DIVISION.NAME.
     */
    @Query(value = """
            SELECT
                st."ID" AS "id",
                st."ID_SPECIF" AS "idSpecif",
                st."ID_DIVISION" AS "idDivision",
                d."NAME" AS "divisionName",
                st."OBOZN" AS "obozn",
                st."NAIM" AS "naim",
                COALESCE(st."QUANTITY", 1) AS "quantity",
                COALESCE(st."RMASSA", 0) AS "massa",
                st."MATERIAL" AS "material",
                st."POS" AS "pos",
                st."FORMAT" AS "format",
                st."ZONE" AS "zone"

            FROM "Konstructor"."STRUCTURE_SPECIF" st

            LEFT JOIN "Konstructor"."DIVISION" d
                ON d."ID" = st."ID_DIVISION"

            WHERE st."ID_SPECIF" IN (:assemblyIds)
              AND st."ID_DIVISION" IN (4, 5, 6, 7)

            ORDER BY
                st."ID_SPECIF",
                st."ID_DIVISION",
                st."POS",
                st."OBOZN"
            """, nativeQuery = true)
    List<AssemblyContentProjection> findContentForAssemblies(
            @Param("assemblyIds") List<Integer> assemblyIds
    );

    /**
     * Загружает серии сразу для всех переданных ID структур.
     *
     * SSID в PRODUCT_SERIES ссылается на STRUCTURE_SPECIF.ID.
     */
    @Query(value = """
            SELECT
                ps."SSID" AS "ssid",
                ps."MINI" AS "mini",
                ps."MAXI" AS "maxi",
                ps."QUANTITY" AS "quantity",
                ps."MASSA" AS "massa",
                ps."DIMENSIONS" AS "dimensions"

            FROM "Konstructor"."PRODUCT_SERIES" ps

            WHERE ps."SSID" IN (:structureIds)

            ORDER BY
                ps."SSID",
                ps."MINI",
                ps."MAXI"
            """, nativeQuery = true)
    List<ProductSeriesProjection> findSeriesByStructureIds(
            @Param("structureIds") List<Integer> structureIds
    );

    /**
     * Получить название подразделения по ID.
     * Используется для резервного корневого узла.
     */
    @Query(value = """
            SELECT d."NAME"
            FROM "Konstructor"."DIVISION" d
            WHERE d."ID" = :divisionId
            """, nativeQuery = true)
    Optional<String> findDivisionNameById(
            @Param("divisionId") Integer divisionId
    );
}