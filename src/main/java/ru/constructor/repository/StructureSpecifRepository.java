package ru.constructor.repository;

import ru.constructor.entity.StructureSpecifEntity;
import ru.constructor.repository.projection.SpecificationStructureProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StructureSpecifRepository
        extends JpaRepository<StructureSpecifEntity, Integer> {

    /**
     * Получить все строки структуры спецификации
     * вместе с названием подразделения.
     *
     * ID_DIVISION связывается со справочником DIVISION,
     * название подразделения берётся из DIVISION.NAME.
     */
    @Query(value = """
            SELECT
                ss."ID" AS "id",
                ss."ID_SPECIF" AS "idSpecif",
                ss."ID_DIVISION" AS "idDivision",
                d."NAME" AS "divisionName",
                ss."ZONE" AS "zone",
                ss."FORMAT" AS "format",
                ss."POS" AS "pos",
                ss."OBOZN" AS "obozn",
                ss."NAIM" AS "naim",
                ss."QUANTITY" AS "quantity",
                ss."RMASSA" AS "massa",
                ss."MATERIAL" AS "material",
                ss."FLAG_PRODUCT_SERIES" AS "flagProductSeries",
                ss."ID_NOMENKLATURA" AS "idNomenklatura",
                ss."ID_SPECIF_22" AS "idSpecif22"
            FROM "Konstructor"."STRUCTURE_SPECIF" ss
            LEFT JOIN "Konstructor"."DIVISION" d
                ON ss."ID_DIVISION" = d."ID"
            WHERE ss."ID_SPECIF" = :specifId
            ORDER BY ss."ID" ASC
            """, nativeQuery = true)
    List<SpecificationStructureProjection> findWithDivisionBySpecifId(
            @Param("specifId") Integer specifId
    );

    List<StructureSpecifEntity> findByIdSpecifOrderByIdAsc(
            Integer idSpecif
    );

    List<StructureSpecifEntity> findByIdSpecif22OrderByIdAsc(
            Integer idSpecif22
    );
}