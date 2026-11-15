package ru.constructor.repository.projection;

public interface AssemblyHierarchyProjection {

    Integer getId();

    Integer getParentId();

    String getObozn();

    String getNaim();

    Integer getQuantity();

    Double getMassa();

    Integer getLevel();

    Integer getStructureId();

    Integer getIzdelieNumber();

    Integer getIzdelieId();

    Integer getIdDivision();
}