
package ru.constructor.repository;

import ru.constructor.entity.Specif22Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Specif22Repository extends JpaRepository<Specif22Entity, Integer> {

    @Query(value = """
            SELECT s."ID"
            FROM "Konstructor"."SPECIF22" s
            WHERE s."ID_TEMA" = :temaId
              AND s."ID_IZDELIE" = :izdelieId
              AND s."ID_GROUPE" = :groupeId
              AND s."ID_PARENT" IS NULL
            ORDER BY s."ID" DESC
            LIMIT 1
            """, nativeQuery = true)
    Integer findRootSpecifId(
            @Param("temaId") Integer temaId,
            @Param("izdelieId") Integer izdelieId,
            @Param("groupeId") Integer groupeId
    );

    List<Specif22Entity> findByIdParentOrderByIdAsc(Integer idParent);

    List<Specif22Entity> findByIdTemaAndIdIzdelieAndIdGroupeOrderByIdAsc(
            Integer idTema,
            Integer idIzdelie,
            Integer idGroupe
    );
}
