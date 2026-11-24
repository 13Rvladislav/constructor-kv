package ru.constructor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.constructor.dto.response.ProductSeriesResponseDto;
import ru.constructor.dto.response.SpecificationStructureResponseDto;

import ru.constructor.entity.ProductSeriesEntity;

import ru.constructor.repository.ProductSeriesRepository;
import ru.constructor.repository.StructureSpecifRepository;
import ru.constructor.repository.projection.SpecificationStructureProjection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Сервис получения полной структуры спецификации.
 *
 * Получает:
 * - все строки STRUCTURE_SPECIF;
 * - название подразделения из DIVISION.NAME;
 * - все серии из PRODUCT_SERIES.
 *
 * Исходные таблицы используются исключительно для чтения.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SpecificationStructureService {

    private final StructureSpecifRepository structureSpecifRepository;

    private final ProductSeriesRepository productSeriesRepository;

    /**
     * Получить полную структуру спецификации вместе с подразделениями и сериями.
     *
     * @param specifId идентификатор спецификации
     * @return все строки структуры с подразделениями и сериями
     */
    public List<SpecificationStructureResponseDto> getSpecificationStructure(
            Integer specifId
    ) {

        log.info(
                "Получение полной структуры спецификации: specifId={}",
                specifId
        );

        /*
         * Шаг 1.
         *
         * Получаем все строки STRUCTURE_SPECIF
         * вместе с названием подразделения.
         *
         * Название подразделения приходит из:
         * DIVISION.NAME
         *
         * Связь:
         * STRUCTURE_SPECIF.ID_DIVISION = DIVISION.ID
         */
        List<SpecificationStructureProjection> structureRows =
                structureSpecifRepository.findWithDivisionBySpecifId(
                        specifId
                );

        if (structureRows.isEmpty()) {

            log.info(
                    "Структура спецификации не найдена: specifId={}",
                    specifId
            );

            return List.of();
        }

        /*
         * Шаг 2.
         *
         * Собираем ID всех строк STRUCTURE_SPECIF.
         *
         * Эти ID используются для получения серий
         * из PRODUCT_SERIES.SSID.
         */
        List<Integer> structureIds = structureRows.stream()
                .map(SpecificationStructureProjection::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        /*
         * Шаг 3.
         *
         * Загружаем все серии одним запросом.
         *
         * Фильтрации по FLAG_PRODUCT_SERIES нет:
         * если серии существуют в PRODUCT_SERIES,
         * они должны попасть в ответ.
         */
        List<ProductSeriesEntity> seriesRows =
                structureIds.isEmpty()
                        ? List.of()
                        : productSeriesRepository
                        .findBySsidInOrderBySsidAscMiniAsc(
                                structureIds
                        );

        /*
         * Шаг 4.
         *
         * Группируем серии по PRODUCT_SERIES.SSID.
         *
         * Ключ:
         * PRODUCT_SERIES.SSID
         *
         * Значение:
         * список серий соответствующей строки структуры.
         */
        Map<Integer, List<ProductSeriesResponseDto>> seriesByStructureId =
                seriesRows.stream()
                        .filter(series -> series.getSsid() != null)
                        .collect(Collectors.groupingBy(
                                ProductSeriesEntity::getSsid,
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        this::toSeriesResponse,
                                        Collectors.toList()
                                )
                        ));

        /*
         * Шаг 5.
         *
         * Формируем итоговый ответ.
         *
         * Для каждой строки:
         * - сохраняем ID подразделения;
         * - добавляем название подразделения;
         * - добавляем связанные серии;
         * - не исключаем строки, у которых нет серий.
         */
        List<SpecificationStructureResponseDto> result =
                new ArrayList<>(structureRows.size());

        for (SpecificationStructureProjection structure : structureRows) {

            List<ProductSeriesResponseDto> series =
                    seriesByStructureId.getOrDefault(
                            structure.getId(),
                            List.of()
                    );

            result.add(
                    toStructureResponse(
                            structure,
                            series
                    )
            );
        }

        log.info(
                "Структура спецификации сформирована: specifId={}, rows={}, series={}",
                specifId,
                result.size(),
                seriesRows.size()
        );

        return result;
    }

    /**
     * Преобразование проекции STRUCTURE_SPECIF в DTO.
     */
    private SpecificationStructureResponseDto toStructureResponse(
            SpecificationStructureProjection structure,
            List<ProductSeriesResponseDto> series
    ) {

        return new SpecificationStructureResponseDto(
                structure.getId(),
                structure.getIdSpecif(),
                structure.getZone(),
                structure.getFormat(),
                structure.getPos(),
                structure.getObozn(),
                structure.getNaim(),
                structure.getQuantity(),
                structure.getIdDivision(),
                structure.getDivisionName(),
                structure.getMassa(),
                structure.getMaterial(),
                structure.getFlagProductSeries(),
                structure.getIdNomenklatura(),
                structure.getIdSpecif22(),
                series
        );
    }

    /**
     * Преобразование строки PRODUCT_SERIES в DTO.
     */
    private ProductSeriesResponseDto toSeriesResponse(
            ProductSeriesEntity entity
    ) {

        return new ProductSeriesResponseDto(
                entity.getId(),
                entity.getSsid(),
                entity.getMini(),
                entity.getMaxi(),
                entity.getQuantity(),
                entity.getMassa(),
                entity.getDimensions()
        );
    }
}