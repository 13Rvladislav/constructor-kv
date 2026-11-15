
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

import java.time.LocalDateTime;

/**
 * Сущность комплектовки.
 *
 * Отображает таблицу KOMPLECTOVKA в схеме Konstructor.
 *
 * Запись содержит основные сведения о сформированной комплектовке:
 * - обозначение комплектовки;
 * - идентификатор служебной записки;
 * - идентификатор сборки;
 * - дату создания и архивирования;
 * - статус;
 * - сведения о пользователях, сформировавших и согласовавших КВ.
 *
 * Важно:
 * Структура таблицы соответствует SQL-дампу fresh(2).sql.
 * Поля PARENT_KV_ID, ID_PARENT_KV и KV_STATUS_ID здесь отсутствуют,
 * поскольку в подтверждённом DDL таблицы таких колонок нет.
 */
@Entity
@Table(
        name = "KOMPLECTOVKA",
        schema = "Konstructor"
)
@Getter
@Setter
@NoArgsConstructor
public class KomplektovkaEntity {

    /**
     * Первичный ключ комплектовки.
     *
     * Значение генерируется существующей последовательностью
     * KOMPLECTOVKA_ID_seq в схеме Konstructor.
     *
     * allocationSize = 1 означает получение идентификаторов
     * по одному, без предварительного резервирования диапазона.
     */
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "komplektovka_id_generator"
    )
    @SequenceGenerator(
            name = "komplektovka_id_generator",
            sequenceName = "KOMPLECTOVKA_ID_seq",
            schema = "Konstructor",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Integer id;

    /**
     * Обозначение комплектовки.
     *
     * Например: КВ-12345.
     */
    @Column(name = "DESIGN", length = 30)
    private String design;

    /**
     * Идентификатор служебной записки,
     * в рамках которой была сформирована комплектовка.
     */
    @Column(name = "ID_SZ", nullable = false)
    private Integer idSz;

    /**
     * Идентификатор сборки, для которой сформирована КВ.
     *
     * В БД хранится строкой, хотя бизнес-значение является
     * идентификатором сборки.
     */
    @Column(name = "SBORKI_SZ", length = 100)
    private String sborkiSz;

    /**
     * Дата и время создания комплектовки.
     */
    @Column(name = "DATE_CREATE")
    private LocalDateTime dateCreate;

    /**
     * Дата и время архивирования комплектовки.
     *
     * NULL означает, что комплектовка не архивирована.
     */
    @Column(name = "DATE_ARCHIVING")
    private LocalDateTime dateArchiving;

    /**
     * Статус комплектовки.
     *
     * Тип short соответствует SMALLINT в PostgreSQL.
     */
    @Column(name = "STATUS")
    private Short status;

    /**
     * Идентификатор пользователя,
     * который сформировал комплектовку.
     */
    @Column(name = "FORMED_BY")
    private Integer formedBy;

    /**
     * Идентификатор пользователя,
     * который согласовал комплектовку.
     */
    @Column(name = "APPROVED_BY")
    private Integer approvedBy;

    /**
     * Дата и время согласования комплектовки.
     */
    @Column(name = "APPROVED_DATE")
    private LocalDateTime approvedDate;

    /**
     * Дата согласования связанной служебной записки.
     */
    @Column(name = "SL_ZAP_AGREED_DATE")
    private LocalDateTime slZapAgreedDate;
}
