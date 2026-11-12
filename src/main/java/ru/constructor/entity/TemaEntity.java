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
        name = "TEMA",
        schema = "Konstructor"
)
@Getter
@NoArgsConstructor
public class TemaEntity {

    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "NAIM", length = 50)
    private String naim;

    @Column(name = "INDEKSKD")
    private Integer indeksKd;
}