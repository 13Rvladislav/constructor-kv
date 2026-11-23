package ru.constructor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.constructor.entity.StructureSpecifEntity;
import ru.constructor.repository.projection.SpecificationStructureProjection;

import java.util.List;

@Repository
public interface SpecificationStructureRepository
        extends JpaRepository<StructureSpecifEntity, Integer> {

    /**
     * Получает все строки исходной спецификации без фильтрации
     * по ID_DIVISION и без проверки серийности.
     *
     * Серии связываются по фактической цепочке:
     *
     * STRUCTURE_SPECIF.ID
     *   -> STRUCTURA_KOMPLECTOVKI.ID_ORIGINAL_STRUCTURE
     *   -> STRUCTURA_KOMPLECTOVKI.ID
     *   -> PRODUCT_SERIES.SSID
     *
     * LEFT JOIN сохраняет строки спецификации, даже если у них
     * нет соответствующей записи о сериях.
     */
    @Query(value = """
            SELECT
                st."ID" AS "id",
                st."ID_SPECIF" AS "idSpecif",
                st."ID_DIVISION" AS "idDivision",
                st."ZONE" AS "zone",
                st."FORMAT" AS "format",
                st."POS" AS "pos",
                st."OBOZN" AS "obozn",
                st."NAIM" AS "naim",
                st."QUANTITY" AS "quantity",
                st."RMASSA" AS "massa",
                st."MATERIAL" AS "material",
                st."FLAG_PRODUCT_SERIES" AS "flagProductSeries",
                st."ID_NOMENKLATURA" AS "idNomenklatura",
                st."ID_SPECIF_22" AS "idSpecif22",

                ps."MINI" AS "seriesMini",
                ps."MAXI" AS "seriesMaxi",
                ps."QUANTITY" AS "seriesQuantity",
                ps."MASSA" AS "seriesMassa",
                ps."DIMENSIONS" AS "seriesDimensions"

            FROM "Konstructor"."STRUCTURE_SPECIF" st

            LEFT JOIN "Konstructor"."STRUCTURA_KOMPLECTOVKI" sk
                ON sk."ID_ORIGINAL_STRUCTURE" = st."ID"

            LEFT JOIN "Konstructor"."PRODUCT_SERIES" ps
                ON ps."SSID" = sk."ID"

            WHERE st."ID_SPECIF" = :specifId

            ORDER BY
                st."ID_DIVISION",
                st."POS",
                st."ID",
                ps."MINI",
                ps."MAXI"
            """, nativeQuery = true)
    List<SpecificationStructureProjection> findFullStructure(
            @Param("specifId") Integer specifId
    );
}