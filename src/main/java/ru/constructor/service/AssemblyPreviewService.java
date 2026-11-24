package ru.constructor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.constructor.dto.response.AssemblyHierarchyResponseDto;
import ru.constructor.entity.Specif22Entity;
import ru.constructor.repository.AssemblyPreviewRepository;
import ru.constructor.repository.projection.AssemblyContentProjection;
import ru.constructor.repository.projection.AssemblyHierarchyProjection;
import ru.constructor.repository.projection.ProductSeriesProjection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AssemblyPreviewService {

    private final AssemblyPreviewRepository assemblyPreviewRepository;

    /**
     * Получить дерево предпросмотра сборки.
     *
     * Выполняется не более трёх основных пакетных запросов:
     *
     * 1. Иерархия сборок с названиями подразделений.
     * 2. Содержимое всех сборок с названиями подразделений.
     * 3. Серии всех найденных структур.
     */
    public List<AssemblyHierarchyResponseDto> getAssemblyPreview(Integer specifId) {

        log.info(
                "Получение предпросмотра сборки: specifId={}",
                specifId
        );

        List<AssemblyHierarchyProjection> hierarchyRows =
                assemblyPreviewRepository.findAssemblyHierarchy(specifId);

        if (hierarchyRows.isEmpty()) {
            log.info(
                    "Иерархия сборки не найдена: specifId={}",
                    specifId
            );

            return createRootFallback(specifId);
        }

        Map<Integer, MutableNode> nodes = new LinkedHashMap<>();

        /*
         * Этап 1. Создаём все узлы сборок.
         */
        for (AssemblyHierarchyProjection row : hierarchyRows) {

            MutableNode node = new MutableNode();

            node.id = row.getId();
            node.idSpecif = row.getId();
            node.parentId = row.getParentId();
            node.obozn = defaultValue(row.getObozn(), "Без обозн.");
            node.naim = defaultValue(row.getNaim(), "Без названия");

            node.quantity = row.getQuantity() != null
                    ? row.getQuantity()
                    : 1;

            node.massa = row.getMassa() != null
                    ? row.getMassa()
                    : 0.0;

            node.idDivision = row.getIdDivision();
            node.divisionName = row.getDivisionName();

            node.level = row.getLevel() != null
                    ? row.getLevel()
                    : 0;

            node.structureId = row.getStructureId();
            node.izdelieNumber = row.getIzdelieNumber();
            node.izdelieId = row.getIzdelieId();

            nodes.put(node.id, node);
        }

        /*
         * Этап 2. Строим связи между сборками.
         */
        List<MutableNode> roots = new ArrayList<>();

        for (MutableNode node : nodes.values()) {

            if (node.parentId == null) {
                roots.add(node);
                continue;
            }

            MutableNode parent = nodes.get(node.parentId);

            if (parent != null) {
                parent.children.add(node);
            } else {
                roots.add(node);
            }
        }

        /*
         * Этап 3. Получаем все ID сборок для пакетной загрузки содержимого.
         */
        List<Integer> assemblyIds = nodes.values()
                .stream()
                .map(node -> node.id)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        /*
         * Этап 4. Загружаем содержимое всех сборок одним запросом.
         */
        List<AssemblyContentProjection> contentRows =
                assemblyIds.isEmpty()
                        ? List.of()
                        : assemblyPreviewRepository.findContentForAssemblies(
                        assemblyIds
                );

        /*
         * Этап 5. Собираем ID структур для пакетной загрузки серий.
         */
        List<Integer> structureIds = new ArrayList<>();

        nodes.values().stream()
                .map(node -> node.structureId)
                .filter(Objects::nonNull)
                .forEach(structureIds::add);

        contentRows.stream()
                .map(AssemblyContentProjection::getId)
                .filter(Objects::nonNull)
                .forEach(structureIds::add);

        structureIds = structureIds.stream()
                .distinct()
                .toList();

        /*
         * Этап 6. Загружаем серии одним запросом.
         */
        List<ProductSeriesProjection> seriesRows =
                structureIds.isEmpty()
                        ? List.of()
                        : assemblyPreviewRepository.findSeriesByStructureIds(
                        structureIds
                );

        Map<Integer, List<AssemblyHierarchyResponseDto.SeriesInfo>> seriesByStructure =
                mapSeries(seriesRows);

        /*
         * Этап 7. Назначаем серии узлам сборок.
         */
        for (MutableNode node : nodes.values()) {

            if (node.structureId != null) {
                node.series = seriesByStructure.getOrDefault(
                        node.structureId,
                        List.of()
                );
            }
        }

        /*
         * Этап 8. Проверяем серийность сборок.
         *
         * Если родитель не подходит, все его дочерние сборки
         * автоматически считаются неподходящими.
         */
        for (MutableNode root : roots) {
            checkAssemblySeries(root, true);
        }

        /*
         * Этап 9. Группируем содержимое по ID сборки.
         */
        Map<Integer, List<AssemblyContentProjection>> contentByAssembly =
                contentRows.stream()
                        .collect(Collectors.groupingBy(
                                AssemblyContentProjection::getIdSpecif,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        /*
         * Этап 10. Добавляем содержимое только в подходящие сборки.
         */
        for (MutableNode node : nodes.values()) {

            if (!Boolean.TRUE.equals(node.fitsSeries)) {
                continue;
            }

            List<AssemblyContentProjection> content =
                    contentByAssembly.getOrDefault(
                            node.id,
                            List.of()
                    );

            for (AssemblyContentProjection contentRow : content) {

                MutableNode contentNode = createContentNode(
                        contentRow,
                        node,
                        seriesByStructure
                );

                node.children.add(contentNode);
            }
        }

        /*
         * Этап 11. Сортируем дерево.
         */
        for (MutableNode root : roots) {
            sortChildren(root);
        }

        /*
         * Этап 12. Преобразуем внутреннее дерево в Response DTO.
         */
        List<AssemblyHierarchyResponseDto> result = roots.stream()
                .map(this::toResponse)
                .toList();

        log.info(
                "Предпросмотр сборки сформирован: specifId={}, корневых узлов={}, всего узлов={}",
                specifId,
                result.size(),
                countNodes(roots)
        );

        return result;
    }

    /**
     * Проверка серийности сборки.
     *
     * При отсутствии номера изделия поведение соответствует
     * старому AssemblyHierarchyDto.checkFitsSeries():
     * сборка считается подходящей.
     */
    private void checkAssemblySeries(
            MutableNode node,
            boolean parentFits
    ) {

        if (!parentFits) {
            node.fitsSeries = false;
            node.fitReason = "Родительский узел не прошёл по серийности";

            for (MutableNode child : node.children) {
                checkAssemblySeries(child, false);
            }

            return;
        }

        if (node.izdelieNumber == null) {
            node.fitsSeries = true;
            node.fitReason = "Нет номера изделия";

        } else if (node.series == null || node.series.isEmpty()) {
            node.fitsSeries = true;
            node.fitReason = "Нет серий (действует на все)";

        } else {

            AssemblyHierarchyResponseDto.SeriesInfo matchedSeries =
                    findMatchingSeries(
                            node.series,
                            node.izdelieNumber
                    );

            if (matchedSeries != null) {
                node.fitsSeries = true;
                node.fitReason = "Подходит (серия "
                        + matchedSeries.mini()
                        + "–"
                        + matchedSeries.maxi()
                        + ")";

            } else {
                node.fitsSeries = false;
                node.fitReason = "Не подходит (изделие "
                        + node.izdelieNumber
                        + " вне диапазона серий)";
            }
        }

        for (MutableNode child : node.children) {
            checkAssemblySeries(
                    child,
                    Boolean.TRUE.equals(node.fitsSeries)
            );
        }
    }

    /**
     * Создание элемента содержимого сборки.
     */
    private MutableNode createContentNode(
            AssemblyContentProjection row,
            MutableNode parent,
            Map<Integer, List<AssemblyHierarchyResponseDto.SeriesInfo>> seriesByStructure
    ) {

        MutableNode node = new MutableNode();

        node.id = row.getId();
        node.idSpecif = row.getIdSpecif();
        node.parentId = parent.id;

        node.obozn = defaultValue(
                row.getObozn(),
                "Без обозн."
        );

        node.naim = defaultValue(
                row.getNaim(),
                "Без названия"
        );

        node.quantity = row.getQuantity() != null
                ? row.getQuantity()
                : 1;

        node.massa = row.getMassa() != null
                ? row.getMassa()
                : 0.0;

        node.idDivision = row.getIdDivision();
        node.divisionName = row.getDivisionName();

        node.material = row.getMaterial();

        node.note = row.getPos() != null
                ? "Поз." + row.getPos()
                : "";

        node.level = parent.level + 1;
        node.structureId = row.getId();

        node.izdelieNumber = parent.izdelieNumber;
        node.izdelieId = parent.izdelieId;

        node.series = seriesByStructure.getOrDefault(
                row.getId(),
                List.of()
        );

        /*
         * Сохраняем старую логику проверки серийности элементов.
         */
        if (node.series.isEmpty()) {

            node.fitsSeries = true;
            node.fitReason = "Нет серий (действует на все)";

        } else {

            AssemblyHierarchyResponseDto.SeriesInfo matchedSeries =
                    node.izdelieNumber == null
                            ? null
                            : findMatchingSeries(
                            node.series,
                            node.izdelieNumber
                    );

            if (matchedSeries == null) {

                node.fitsSeries = false;
                node.fitReason = "Не подходит по сериям";

            } else {

                node.fitsSeries = true;
                node.fitReason = "Подходит по сериям "
                        + matchedSeries.mini()
                        + "–"
                        + matchedSeries.maxi();

                if (matchedSeries.quantity() != null) {
                    node.quantity = matchedSeries.quantity();
                }

                if (matchedSeries.massa() != null) {
                    node.massa = matchedSeries.massa();
                }
            }
        }

        return node;
    }

    /**
     * Получить серию, диапазон которой содержит номер изделия.
     */
    private AssemblyHierarchyResponseDto.SeriesInfo findMatchingSeries(
            List<AssemblyHierarchyResponseDto.SeriesInfo> series,
            Integer productNumber
    ) {

        if (productNumber == null || series == null) {
            return null;
        }

        return series.stream()
                .filter(item ->
                        item.mini() != null
                                && item.maxi() != null
                                && productNumber >= item.mini()
                                && productNumber <= item.maxi()
                )
                .findFirst()
                .orElse(null);
    }

    /**
     * Преобразование результата SQL-запроса в карту серий.
     */
    private Map<Integer, List<AssemblyHierarchyResponseDto.SeriesInfo>> mapSeries(
            List<ProductSeriesProjection> rows
    ) {

        Map<Integer, List<AssemblyHierarchyResponseDto.SeriesInfo>> result =
                new HashMap<>();

        for (ProductSeriesProjection row : rows) {

            if (row.getSsid() == null) {
                continue;
            }

            AssemblyHierarchyResponseDto.SeriesInfo series =
                    new AssemblyHierarchyResponseDto.SeriesInfo(
                            row.getMini(),
                            row.getMaxi(),
                            row.getQuantity(),
                            row.getMassa(),
                            row.getDimensions()
                    );

            result.computeIfAbsent(
                    row.getSsid(),
                    ignored -> new ArrayList<>()
            ).add(series);
        }

        return result;
    }

    /**
     * Сортировка дочерних элементов по обозначению.
     */
    private void sortChildren(MutableNode node) {

        node.children.sort(
                Comparator.comparing(
                        child -> child.obozn,
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        for (MutableNode child : node.children) {
            sortChildren(child);
        }
    }

    /**
     * Преобразование внутреннего узла в Response DTO.
     */
    private AssemblyHierarchyResponseDto toResponse(MutableNode node) {

        List<AssemblyHierarchyResponseDto> children =
                node.children.stream()
                        .map(this::toResponse)
                        .toList();

        return new AssemblyHierarchyResponseDto(
                node.id,
                node.idSpecif,
                node.parentId,
                node.obozn,
                node.naim,
                node.quantity,
                node.massa,
                node.idDivision,
                node.divisionName,
                node.material,
                node.note,
                node.level,
                node.structureId,
                node.izdelieNumber,
                node.izdelieId,
                node.fitsSeries,
                node.fitReason,
                node.series,
                children
        );
    }

    /**
     * Резервный вариант для спецификации без структуры сборок.
     */
    private List<AssemblyHierarchyResponseDto> createRootFallback(
            Integer specifId
    ) {

        return assemblyPreviewRepository.findById(specifId)
                .map(this::createRootNode)
                .map(List::of)
                .orElse(List.of());
    }

    private AssemblyHierarchyResponseDto createRootNode(
            Specif22Entity specif
    ) {

        String divisionName = assemblyPreviewRepository
                .findDivisionNameById(3)
                .orElse(null);

        return new AssemblyHierarchyResponseDto(
                specif.getId(),
                specif.getId(),
                null,
                defaultValue(specif.getObozn(), "Без обозн."),
                defaultValue(specif.getNaim(), "Без названия"),
                1,
                specif.getMassa() != null
                        ? specif.getMassa()
                        : 0.0,
                3,
                divisionName,
                null,
                null,
                0,
                null,
                null,
                null,
                true,
                "Корневая сборка",
                List.of(),
                List.of()
        );
    }

    private int countNodes(List<MutableNode> nodes) {

        int count = 0;

        for (MutableNode node : nodes) {
            count++;
            count += countNodes(node.children);
        }

        return count;
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank()
                ? fallback
                : value;
    }

    /**
     * Внутренняя изменяемая модель дерева.
     * Не используется в API.
     */
    private static class MutableNode {

        private Integer id;
        private Integer idSpecif;
        private Integer parentId;

        private String obozn;
        private String naim;

        private Integer quantity;
        private Double massa;

        private Integer idDivision;
        private String divisionName;

        private String material;
        private String note;

        private Integer level;
        private Integer structureId;

        private Integer izdelieNumber;
        private Integer izdelieId;

        private Boolean fitsSeries;
        private String fitReason;

        private List<AssemblyHierarchyResponseDto.SeriesInfo> series =
                new ArrayList<>();

        private List<MutableNode> children =
                new ArrayList<>();
    }
}