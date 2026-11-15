
package ru.constructor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Сущность структуры комплектовки.
 *
 * Отображает таблицу STRUCTURA_KOMPLECTOVKI в схеме Konstructor.
 *
 * Таблица хранит состав сформированной КВ:
 * - сборочные единицы;
 * - детали;
 * - стандартные изделия;
 * - покупные изделия;
 * - материалы.
 *
 * Иерархия элементов представлена через колонку PARENT.
 * Она содержит ID родительского элемента структуры.
 *
 * Колонка LEVEL определяет уровень вложенности элемента.
 *
 * Важно:
 * Таблица имеет составной первичный ключ из шести колонок.
 * Поэтому используется @IdClass, а не обычный одиночный @Id.
 */
@Entity
@Table(
        name = "STRUCTURA_KOMPLECTOVKI",
        schema = "Konstructor"
)
@IdClass(StructuraKomplektovkiId.class)
@Getter
@Setter
@NoArgsConstructor
public class StructuraKomplektovkiEntity {

    /**
     * Идентификатор строки структуры.
     *
     * В PostgreSQL для него существует последовательность
     * STRUCTURA_KOMPLECTOVKI_ID_seq.
     *
     * Получение значения будет выполняться явно через репозиторий,
     * поскольку у сущности составной первичный ключ.
     */
    @Id
    @Column(name = "ID", nullable = false)
    private Integer id;

    /**
     * Идентификатор комплектовки,
     * к которой относится текущий элемент структуры.
     */
    @Id
    @Column(name = "ID_KOMLECTOVKA", nullable = false)
    private Integer idKomlectovka;

    /**
     * Идентификатор спецификации,
     * которой принадлежит текущий элемент.
     */
    @Id
    @Column(name = "ID_SPECIF", nullable = false)
    private Integer idSpecif;

    /**
     * Обозначение элемента структуры.
     *
     * Например, обозначение сборочной единицы или детали.
     */
    @Column(name = "DESIGN", length = 100)
    private String design;

    /**
     * Наименование элемента структуры.
     */
    @Column(name = "NAME", length = 100)
    private String name;

    /**
     * Количество элементов.
     */
    @Column(name = "QUANTITY")
    private Integer quantity;

    /**
     * Масса элемента.
     */
    @Column(name = "MASSA")
    private Double massa;

    /**
     * Идентификатор подразделения/типа элемента.
     *
     * В отличие от ID_DIVISION в исходной таблице
     * STRUCTURE_SPECIF, здесь используется INTEGER.
     */
    @Id
    @Column(name = "ID_DIVISION", nullable = false)
    private Integer idDivision;

    /**
     * Материал, из которого изготовлен элемент.
     */
    @Column(name = "MATERIAL", length = 150)
    private String material;

    /**
     * Дополнительное примечание к элементу структуры.
     */
    @Column(name = "NOTE", length = 150)
    private String note;

    /**
     * Идентификатор родительской строки структуры.
     *
     * NULL используется для корневого элемента.
     *
     * Это поле формирует иерархию внутри комплектовки.
     */
    @Column(name = "PARENT")
    private Integer parent;

    /**
     * Уровень вложенности элемента в дереве структуры.
     *
     * Корневой элемент обычно имеет уровень 0.
     */
    @Column(name = "LEVEL")
    private Integer level;

    /**
     * Идентификатор корневой спецификации,
     * на основании которой сформирована комплектовка.
     */
    @Id
    @Column(name = "ID_ROOT_SPECIF", nullable = false)
    private Integer idRootSpecif;

    /**
     * Идентификатор исходной строки структуры спецификации.
     *
     * Позволяет связать сохранённый элемент КВ
     * с исходной записью в STRUCTURE_SPECIF.
     */
    @Id
    @Column(name = "ID_ORIGINAL_STRUCTURE", nullable = false)
    private Integer idOriginalStructure;
}
