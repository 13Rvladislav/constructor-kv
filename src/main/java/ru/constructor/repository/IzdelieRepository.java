
package ru.constructor.repository;

import ru.constructor.entity.IzdelieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IzdelieRepository extends JpaRepository<IzdelieEntity, Integer> {

    List<IzdelieEntity> findByIdTemaOrderByIdAsc(Integer idTema);

    List<IzdelieEntity> findAllByOrderByIdAsc();
}
