package ru.constructor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Сущность серии элемента комплектовки.
 *
 * Использует существующую таблицу PRODUCT_SERIES.
 *
 * SSID содержит ID элемента из STRUCTURA_KOMPLECTOVKI,
 * к которому относится диапазон серии.
 *
 * В одной строке хранится один диапазон:
 * MINI — начало диапазона;
 * MAXI — конец диапазона.
 */
@Entity
@Table(
        name = "PRODUCT_SERIES",
        schema = "Konstructor"
)
@Getter
@Setter
@NoArgsConstructor
public class ProductSeriesEntity {

    /**
     * Первичный ключ записи серии.
     *
     * Генерируется существующей последовательностью
     * PRODUCT_SERIES_ID_seq.
     */
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "product_series_id_generator"
    )
    @SequenceGenerator(
            name = "product_series_id_generator",
            sequenceName = "PRODUCT_SERIES_ID_seq",
            schema = "Konstructor",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Integer id;

    /**
     * ID элемента структуры КВ.
     *
     * Соответствует STRUCTURA_KOMPLECTOVKI.ID.
     */
    @Column(name = "SSID")
    private Integer ssid;

    /**
     * Начало диапазона серии.
     */
    @Column(name = "MINI")
    private Integer mini;

    /**
     * Конец диапазона серии.
     */
    @Column(name = "MAXI")
    private Integer maxi;

    /**
     * Количество изделий в серии.
     */
    @Column(name = "QUANTITY")
    private Integer quantity;

    /**
     * Масса изделия в данной серии.
     */
    @Column(name = "MASSA")
    private Double massa;

    /**
     * Габариты изделия.
     */
    @Column(name = "DIMENSIONS", length = 100)
    private String dimensions;
}