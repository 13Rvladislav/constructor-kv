
package ru.constructor.repository;

import ru.constructor.entity.GroupeEntity;
import ru.constructor.repository.projection.GroupeTreeProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupeRepository extends JpaRepository<GroupeEntity, Integer> {

    @Query(value = """
            SELECT DISTINCT
                s."ID_TEMA" AS "temaId",
                s."ID_IZDELIE" AS "izdelieId",
                g."ID" AS "groupeId",
                g."NAIM" AS "groupeName"
            FROM "Konstructor"."SPECIF22" s
            INNER JOIN "Konstructor"."GROUPE" g
                ON g."ID" = s."ID_GROUPE"
            WHERE s."ID_TEMA" IS NOT NULL
              AND s."ID_IZDELIE" IS NOT NULL
              AND s."ID_GROUPE" IS NOT NULL
            ORDER BY s."ID_TEMA", s."ID_IZDELIE", g."ID"
            """, nativeQuery = true)
    List<GroupeTreeProjection> findAllGroupsForTree();
}
