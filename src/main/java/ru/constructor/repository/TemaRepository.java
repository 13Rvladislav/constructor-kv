
package ru.constructor.repository;

import ru.constructor.entity.TemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemaRepository extends JpaRepository<TemaEntity, Integer> {

    List<TemaEntity> findAllByOrderByIdAsc();

    List<TemaEntity> findByNaimContainingIgnoreCaseOrderByIdAsc(String naim);
}
