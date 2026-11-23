
package ru.constructor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.constructor.dto.response.ProductSeriesResponseDto;
import ru.constructor.dto.response.SpecificationStructureResponseDto;

import ru.constructor.entity.ProductSeriesEntity;
import ru.constructor.entity.StructureSpecifEntity;

import ru.constructor.repository.ProductSeriesRepository;
import ru.constructor.repository.StructureSpecifRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Сервис получения полной структуры спецификации.
 *
 * Важно:
 * - загружаются все строки STRUCTURE_SPECIF;
 * - отсутствует фильтрация по ID_DIVISION;
 * - отсутствует фильтрация по FLAG_PRODUCT_SERIES;
 * - серии загружаются из PRODUCT_SERIES;
 * - PRODUCT_SERIES.SSID связывается с STRUCTURE_SPECIF.ID;
 * - исходные таблицы используются исключительно для чтения.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SpecificationStructureService {

    private final StructureSpecifRepository structureSpecifRepository;

    private final ProductSeriesRepository productSeriesRepository;

    /**
     * Получить полную структуру спецификации вместе с сериями.
     *
     * @param specifId идентификатор спецификации
     * @return все строки структуры с соответствующими сериями
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
         * Получаем ВСЕ строки спецификации.
         *
         * Никаких ограничений по ID_DIVISION,
         * FLAG_PRODUCT_SERIES и другим признакам нет.
         */
        List<StructureSpecifEntity> structureRows =
                structureSpecifRepository.findByIdSpecifOrderByIdAsc(
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
         * Собираем ID всех строк STRUCTURE_SPECIF.
         *
         * Именно эти ID должны совпадать с PRODUCT_SERIES.SSID.
         */
        List<Integer> structureIds = structureRows.stream()
                .map(StructureSpecifEntity::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        /*
         * Шаг 3.
         * Загружаем все серии одним запросом.
         *
         * ВАЖНО:
         * Здесь намеренно отсутствует фильтрация
         * по flagProductSeries.
         *
         * Даже если flagProductSeries = false,
         * существующие записи PRODUCT_SERIES должны попасть в ответ.
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
         * Группируем серии по SSID.
         *
         * Ключ:
         * PRODUCT_SERIES.SSID
         *
         * Значение:
         * все серии соответствующей строки STRUCTURE_SPECIF.
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
         * Формируем ответ.
         *
         * Для каждой строки используем STRUCTURE_SPECIF.ID
         * как ключ поиска серий.
         *
         * Если серий нет, возвращаем пустой список.
         * Сама строка структуры при этом не удаляется.
         */
        List<SpecificationStructureResponseDto> result =
                new ArrayList<>(structureRows.size());

        for (StructureSpecifEntity structure : structureRows) {

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

        int totalSeries = seriesRows.size();

        log.info(
                "Структура спецификации сформирована: specifId={}, rows={}, series={}",
                specifId,
                result.size(),
                totalSeries
        );

        return result;
    }

    /**
     * Преобразование строки STRUCTURE_SPECIF в DTO.
     */
    private SpecificationStructureResponseDto toStructureResponse(
            StructureSpecifEntity entity,
            List<ProductSeriesResponseDto> series
    ) {

        return new SpecificationStructureResponseDto(
                entity.getId(),
                entity.getIdSpecif(),
                entity.getZone(),
                entity.getFormat(),
                entity.getPos(),
                entity.getObozn(),
                entity.getNaim(),
                entity.getQuantity(),
                entity.getIdDivision(),
                entity.getMassa(),
                entity.getMaterial(),
                entity.getFlagProductSeries(),
                entity.getIdNomenklatura(),
                entity.getIdSpecif22(),
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
