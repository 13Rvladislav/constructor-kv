
package ru.constructor.repository;

import ru.constructor.entity.StructureSpecifEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StructureSpecifRepository
        extends JpaRepository<StructureSpecifEntity, Integer> {

    List<StructureSpecifEntity> findByIdSpecifOrderByIdAsc(Integer idSpecif);

    List<StructureSpecifEntity> findByIdSpecif22OrderByIdAsc(Integer idSpecif22);
}
