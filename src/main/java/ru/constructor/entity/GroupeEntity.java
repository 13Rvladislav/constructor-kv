package ru.constructor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(
        name = "GROUPE",
        schema = "Konstructor"
)
@Getter
@NoArgsConstructor
public class GroupeEntity {

    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "NAIM", length = 50)
    private String naim;
}