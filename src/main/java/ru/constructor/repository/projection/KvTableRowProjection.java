
package ru.constructor.repository.projection;

import java.time.LocalDateTime;

public interface KvTableRowProjection {

    Integer getNumber();

    Integer getSlZapiskaId();

    String getSlZapiskaObozn();

    Integer getSpecifId();

    String getSpecifObozn();

    Integer getKomplektovkaId();

    String getKomplektovkaDesign();

    String getParentKvDesign();

    Integer getKvStatusId();

    String getStatus();

    LocalDateTime getSlZapAgreedDate();

    Boolean getHasKv();
}
