
package ru.constructor.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Составной первичный ключ сущности StructuraKomplektovkiEntity.
 *
 * В таблице STRUCTURA_KOMPLECTOVKI первичный ключ состоит
 * не из одной колонки ID, а из шести колонок:
 *
 * 1. ID;
 * 2. ID_KOMLECTOVKA;
 * 3. ID_SPECIF;
 * 4. ID_DIVISION;
 * 5. ID_ROOT_SPECIF;
 * 6. ID_ORIGINAL_STRUCTURE.
 *
 * Hibernate должен учитывать все эти поля при идентификации
 * записи таблицы.
 *
 * Класс используется совместно с аннотацией @IdClass.
 *
 * Требования JPA к классу составного ключа:
 * - реализация Serializable;
 * - конструктор без аргументов;
 * - equals() и hashCode();
 * - поля с именами и типами, соответствующими полям @Id сущности.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StructuraKomplektovkiId implements Serializable {

    /**
     * Идентификатор строки структуры комплектовки.
     */
    private Integer id;

    /**
     * Идентификатор комплектовки.
     */
    private Integer idKomlectovka;

    /**
     * Идентификатор спецификации.
     */
    private Integer idSpecif;

    /**
     * Идентификатор подразделения/типа элемента структуры.
     */
    private Integer idDivision;

    /**
     * Идентификатор корневой спецификации.
     */
    private Integer idRootSpecif;

    /**
     * Идентификатор исходной строки структуры спецификации.
     */
    private Integer idOriginalStructure;

    /**
     * Сравнение составных ключей.
     *
     * Две записи считаются одинаковыми только тогда,
     * когда совпадают все шесть компонентов ключа.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof StructuraKomplektovkiId that)) {
            return false;
        }

        return Objects.equals(id, that.id)
                && Objects.equals(idKomlectovka, that.idKomlectovka)
                && Objects.equals(idSpecif, that.idSpecif)
                && Objects.equals(idDivision, that.idDivision)
                && Objects.equals(idRootSpecif, that.idRootSpecif)
                && Objects.equals(idOriginalStructure, that.idOriginalStructure);
    }

    /**
     * Вычисление хеш-кода на основании всех компонентов ключа.
     *
     * Реализация согласована с equals().
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                idKomlectovka,
                idSpecif,
                idDivision,
                idRootSpecif,
                idOriginalStructure
        );
    }
}
