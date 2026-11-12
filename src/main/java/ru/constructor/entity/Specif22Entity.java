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
        name = "SPECIF22",
        schema = "Konstructor"
)
@Getter
@NoArgsConstructor
public class Specif22Entity {

    @Id
    @Column(name = "ID")
    private Integer id;

    /**
     * ID родительской спецификации.
     * NULL означает корневую спецификацию.
     */
    @Column(name = "ID_PARENT")
    private Integer idParent;

    @Column(name = "OBOZN", length = 150)
    private String obozn;

    @Column(name = "NAIM", length = 200)
    private String naim;

    @Column(name = "ID_TEMA")
    private Integer idTema;

    @Column(name = "ID_IZDELIE")
    private Integer idIzdelie;

    @Column(name = "PERV_PRIM", length = 50)
    private String pervPrim;

    @Column(name = "MASSA")
    private Double massa;

    /**
     * Ссылка на заимствованную спецификацию.
     */
    @Column(name = "LINK_IMPLEMETATION")
    private Integer linkImplementation;

    /**
     * В БД SMALLINT, поэтому Java Short.
     */
    @Column(name = "FLAG")
    private Short flag;

    @Column(name = "LETTERS", length = 50)
    private String letters;

    @Column(name = "ID_GROUPE")
    private Integer idGroupe;

    @Column(name = "MASKA", length = 100)
    private String maska;

    @Column(name = "SHABLON", length = 100)
    private String shablon;

    @Column(name = "GROUPE_SPECIF", length = 50)
    private String groupeSpecif;

    @Column(name = "PERV_PRIM_ID")
    private Integer pervPrimId;
}