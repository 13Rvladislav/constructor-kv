package ru.constructor.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ru.constructor.dto.request.KomplektovkaFormRequestDto;
import ru.constructor.dto.response.AssemblyHierarchyResponseDto;
import ru.constructor.dto.response.KomplektovkaFormResponseDto;

import ru.constructor.entity.KomplektovkaEntity;
import ru.constructor.entity.ProductSeriesEntity;
import ru.constructor.entity.StructuraKomplektovkiEntity;
import ru.constructor.entity.StructuraKomplektovkiId;

import ru.constructor.repository.KomplektovkaRepository;
import ru.constructor.repository.ProductSeriesRepository;
import ru.constructor.repository.SlZapiskaRepository;
import ru.constructor.repository.Specif22Repository;
import ru.constructor.repository.StructuraKomplektovkiRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис формирования комплектовок.
 *
 * Основные этапы:
 *
 * 1. Проверка согласования служебной записки.
 * 2. Проверка принадлежности сборки служебной записке.
 * 3. Проверка отсутствия действующей КВ.
 * 4. Получение рассчитанной структуры сборки.
 * 5. Создание записи KOMPLECTOVKA.
 * 6. Подготовка дерева элементов структуры.
 * 7. Получение ID из sequence.
 * 8. Сохранение STRUCTURA_KOMPLECTOVKI.
 * 9. Сохранение серий в PRODUCT_SERIES.
 *
 * Все операции выполняются в одной транзакции.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KomplektovkaFormService {

    /**
     * Начальный статус записи КВ.
     *
     * Соответствует старой реализации.
     */
    private static final short DEFAULT_STATUS = 0;

    /**
     * Подразделение корневой сборки.
     */
    private static final int DEFAULT_ASSEMBLY_DIVISION = 3;

    /**
     * Репозиторий комплектовок.
     */
    private final KomplektovkaRepository komplektovkaRepository;

    /**
     * Репозиторий структуры КВ.
     */
    private final StructuraKomplektovkiRepository structureRepository;

    /**
     * Репозиторий серий.
     */
    private final ProductSeriesRepository productSeriesRepository;

    /**
     * Репозиторий служебных записок.
     */
    private final SlZapiskaRepository slZapiskaRepository;

    /**
     * Репозиторий исходных спецификаций.
     */
    private final Specif22Repository specif22Repository;

    /**
     * Сервис предварительного просмотра структуры сборки.
     */
    private final AssemblyPreviewService assemblyPreviewService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Сформировать комплектовку.
     *
     * @param request параметры формирования
     * @return результат создания КВ
     */
    @Transactional
    public KomplektovkaFormResponseDto formKomplektovka(
            KomplektovkaFormRequestDto request
    ) {
        Integer slZapiskaId = request.slZapiskaId();
        Integer specifId = request.specifId();

        log.info(
                "Начало формирования КВ: slZapiskaId={}, specifId={}, formedBy={}",
                slZapiskaId,
                specifId,
                request.formedBy()
        );

        /*
         * Блокируем параллельное формирование одной и той же КВ.
         *
         * Блокировка автоматически снимается при завершении
         * текущей транзакции.
         */
        acquireFormationLock(slZapiskaId, specifId);

        /*
         * Проверяем, что служебная записка согласована.
         */
        if (!slZapiskaRepository.isSlZapiskaAgreed(slZapiskaId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Служебная записка не согласована"
            );
        }

        /*
         * Проверяем принадлежность сборки служебной записке.
         */
        if (!slZapiskaRepository.isAssemblyBelongsToSlZapiska(
                slZapiskaId,
                specifId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Выбранная сборка не относится к указанной служебной записке"
            );
        }

        /*
         * Проверяем, что действующая КВ ещё не создана.
         */
        if (komplektovkaRepository
                .findActiveBySlZapiskaAndSpecif(slZapiskaId, specifId)
                .isPresent()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Для этой сборки уже существует действующая КВ"
            );
        }

        /*
         * Получаем исходную спецификацию.
         */
        var rootSpecif = specif22Repository.findById(specifId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Спецификация не найдена: " + specifId
                ));

        /*
         * Получаем структуру, уже рассчитанную сервисом preview.
         */
        List<AssemblyHierarchyResponseDto> hierarchy =
                assemblyPreviewService.getAssemblyPreview(specifId);

        if (hierarchy == null || hierarchy.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Структура выбранной сборки пуста"
            );
        }

        /*
         * Создаём основную запись КВ.
         */
        LocalDateTime now = LocalDateTime.now();

        KomplektovkaEntity komplektovka = new KomplektovkaEntity();

        komplektovka.setIdSz(slZapiskaId);
        komplektovka.setSborkiSz(String.valueOf(specifId));
        komplektovka.setDateCreate(now);
        komplektovka.setStatus(DEFAULT_STATUS);
        komplektovka.setFormedBy(request.formedBy());

        /*
         * Формируем обозначение по старой бизнес-логике.
         */
        String designation = rootSpecif.getObozn();

        if (designation == null || designation.isBlank()) {
            designation = "СБ-" + specifId;
        }

        komplektovka.setDesign("КВ-" + designation);

        /*
         * Сохраняем запись и получаем её ID.
         */
        KomplektovkaEntity savedKomplektovka =
                komplektovkaRepository.save(komplektovka);

        Integer komplektovkaId = savedKomplektovka.getId();

        /*
         * Подготавливаем дерево для сохранения.
         *
         * Если узел не подходит по сериям, он и его потомки
         * не попадают в структуру комплектовки.
         */
        List<StructureNodeData> nodes = new ArrayList<>();

        for (AssemblyHierarchyResponseDto root : hierarchy) {
            collectSuitableNodes(root, null, nodes);
        }

        if (nodes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Нет элементов, подходящих для формирования КВ"
            );
        }

        /*
         * Получаем ID для всех элементов структуры одним запросом.
         */
        List<Long> generatedIds =
                structureRepository.getNextIds(nodes.size());

        if (generatedIds.size() != nodes.size()) {
            throw new IllegalStateException(
                    "Не удалось получить ID для всех элементов структуры"
            );
        }

        /*
         * Строим JPA-сущности с составным первичным ключом.
         */
        List<StructuraKomplektovkiEntity> structureEntities =
                buildStructureEntities(
                        nodes,
                        generatedIds,
                        komplektovkaId,
                        specifId
                );

        /*
         * Сохраняем структуру.
         */
        structureRepository.saveAll(structureEntities);

        /*
         * Сохраняем серии.
         *
         * SSID указывает на ID сохранённого элемента структуры КВ.
         */
        List<ProductSeriesEntity> seriesEntities =
                buildProductSeries(nodes, generatedIds);

        if (!seriesEntities.isEmpty()) {
            productSeriesRepository.saveAll(seriesEntities);
        }

        log.info(
                "КВ сформирована: id={}, designation={}, structureElements={}, series={}",
                komplektovkaId,
                savedKomplektovka.getDesign(),
                structureEntities.size(),
                seriesEntities.size()
        );

        return new KomplektovkaFormResponseDto(
                komplektovkaId,
                savedKomplektovka.getDesign(),
                slZapiskaId,
                specifId,
                savedKomplektovka.getDateCreate(),
                structureEntities.size()
        );
    }

    /**
     * Получить advisory-блокировку PostgreSQL.
     *
     * Используется для защиты от одновременного создания
     * одинаковой комплектовки несколькими запросами.
     */
    private void acquireFormationLock(
            Integer slZapiskaId,
            Integer specifId
    ) {
        String lockKey = "komplektovka:" + slZapiskaId + ":" + specifId;

        entityManager.createNativeQuery(
                        "SELECT pg_advisory_xact_lock(hashtext(?1))"
                )
                .setParameter(1, lockKey)
                .getSingleResult();
    }

    /**
     * Рекурсивно собирает подходящие элементы дерева.
     *
     * Ссылка на родителя сохраняется как ссылка на временный объект.
     * Благодаря этому после получения ID можно корректно заполнить PARENT.
     */
    private void collectSuitableNodes(
            AssemblyHierarchyResponseDto dto,
            StructureNodeData parent,
            List<StructureNodeData> result
    ) {
        /*
         * Не сохраняем узел и его потомков,
         * если он не подходит по серийности.
         */
        if (Boolean.FALSE.equals(dto.fitsSeries())) {
            return;
        }

        StructureNodeData current = new StructureNodeData(dto, parent);

        result.add(current);

        /*
         * Обрабатываем дочерние элементы.
         */
        if (dto.children() != null) {
            for (AssemblyHierarchyResponseDto child : dto.children()) {
                collectSuitableNodes(child, current, result);
            }
        }
    }

    /**
     * Создаёт сущности структуры комплектовки.
     *
     * ID родителя определяется по уже назначенным ID временных узлов.
     *
     * ВАЖНО:
     * ID_SPECIF соответствует node.id, как в старой реализации.
     */
    private List<StructuraKomplektovkiEntity> buildStructureEntities(
            List<StructureNodeData> nodes,
            List<Long> generatedIds,
            Integer komplektovkaId,
            Integer rootSpecifId
    ) {
        /*
         * Сопоставление временного объекта и его ID.
         */
        Map<StructureNodeData, Integer> assignedIds =
                new IdentityHashMap<>();

        for (int i = 0; i < nodes.size(); i++) {
            assignedIds.put(
                    nodes.get(i),
                    Math.toIntExact(generatedIds.get(i))
            );
        }

        List<StructuraKomplektovkiEntity> result =
                new ArrayList<>(nodes.size());

        for (StructureNodeData nodeData : nodes) {

            AssemblyHierarchyResponseDto dto = nodeData.dto();

            Integer id = assignedIds.get(nodeData);

            /*
             * ID родительского элемента структуры КВ.
             *
             * Для корневого элемента PARENT = NULL.
             */
            Integer parentId = nodeData.parent() == null
                    ? null
                    : assignedIds.get(nodeData.parent());

            StructuraKomplektovkiEntity entity =
                    new StructuraKomplektovkiEntity();

            /*
             * Составной первичный ключ.
             *
             * Все шесть компонентов соответствуют PK таблицы.
             */
            StructuraKomplektovkiId primaryKey =
                    new StructuraKomplektovkiId();

            primaryKey.setId(id);
            primaryKey.setIdKomlectovka(komplektovkaId);
            primaryKey.setIdSpecif(dto.id());
            primaryKey.setIdDivision(
                    dto.idDivision() != null
                            ? dto.idDivision()
                            : DEFAULT_ASSEMBLY_DIVISION
            );
            primaryKey.setIdRootSpecif(rootSpecifId);
            primaryKey.setIdOriginalStructure(
                    dto.structureId() != null
                            ? dto.structureId()
                            : 0
            );

            entity.setId(primaryKey);

            /*
             * Данные элемента.
             */
            entity.setDesign(dto.obozn());
            entity.setName(dto.naim());

            entity.setQuantity(
                    dto.quantity() != null
                            ? dto.quantity()
                            : 1
            );

            entity.setMassa(
                    dto.massa() != null
                            ? dto.massa()
                            : 0.0
            );

            entity.setMaterial(dto.material());
            entity.setNote(dto.note());
            entity.setParent(parentId);

            entity.setLevel(
                    dto.level() != null
                            ? dto.level()
                            : 0
            );

            result.add(entity);
        }

        return result;
    }

    /**
     * Создаёт записи серий для сохранённых элементов структуры.
     *
     * Для каждого узла:
     * - берёт серии из DTO;
     * - присваивает SSID ID сохранённого элемента структуры;
     * - переносит диапазон, количество, массу и габариты.
     */
    private List<ProductSeriesEntity> buildProductSeries(
            List<StructureNodeData> nodes,
            List<Long> generatedIds
    ) {
        List<ProductSeriesEntity> result = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {

            AssemblyHierarchyResponseDto dto = nodes.get(i).dto();

            if (dto.series() == null || dto.series().isEmpty()) {
                continue;
            }

            Integer structureId = Math.toIntExact(generatedIds.get(i));

            for (AssemblyHierarchyResponseDto.SeriesInfo seriesInfo : dto.series()) {

                ProductSeriesEntity series = new ProductSeriesEntity();

                /*
                 * Связь с элементом STRUCTURA_KOMPLECTOVKI.
                 */
                series.setSsid(structureId);

                series.setMini(seriesInfo.mini());
                series.setMaxi(seriesInfo.maxi());
                series.setQuantity(seriesInfo.quantity());
                series.setMassa(seriesInfo.massa());
                series.setDimensions(seriesInfo.dimensions());

                result.add(series);
            }
        }

        return result;
    }

    /**
     * Временный узел для подготовки структуры.
     *
     * Не является Entity и не сохраняется напрямую в БД.
     */
    private record StructureNodeData(
            AssemblyHierarchyResponseDto dto,
            StructureNodeData parent
    ) {
    }
}