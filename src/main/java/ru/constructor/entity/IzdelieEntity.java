package ru.constructor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

@Entity
@Immutable
@Table(
        name = "IZDELIE",
        schema = "Konstructor"
)
@Getter
@NoArgsConstructor
public class IzdelieEntity {

    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "ID_TEMA", nullable = false)
    private Integer idTema;

    @Column(name = "NAIM_IZD", length = 50)
    private String naimIzd;

    @Column(name = "NAME", length = 400)
    private String name;

    @Column(name = "DATE_ADD")
    private OffsetDateTime dateAdd;
}