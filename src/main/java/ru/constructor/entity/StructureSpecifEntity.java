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
        name = "STRUCTURE_SPECIF",
        schema = "Konstructor"
)
@Getter
@NoArgsConstructor
public class StructureSpecifEntity {

    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "ID_SPECIF", nullable = false)
    private Integer idSpecif;

    @Column(name = "ZONE", length = 11)
    private String zone;

    @Column(name = "FORMAT", length = 11)
    private String format;

    @Column(name = "POS", length = 11)
    private String pos;

    @Column(name = "OBOZN", length = 200)
    private String obozn;

    @Column(name = "NAIM", length = 250)
    private String naim;

    @Column(name = "QUANTITY")
    private Integer quantity;

    @Column(name = "ID_DIVISION", nullable = false)
    private Short idDivision;

    @Column(name = "RMASSA")
    private Double massa;

    @Column(name = "MATERIAL", length = 150)
    private String material;

    @Column(name = "FLAG_PRODUCT_SERIES")
    private Boolean flagProductSeries;

    @Column(name = "ID_NOMENKLATURA")
    private Integer idNomenklatura;

    @Column(name = "ID_SPECIF_22")
    private Integer idSpecif22;
}