
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
import ru.constructor.entity.StructuraKomplektovkiEntity;

import ru.constructor.repository.KomplektovkaRepository;
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
 * Исходные таблицы конструкторского блока используются
 * исключительно для чтения.
 *
 * При формировании КВ запись выполняется только в:
 * - KOMPLECTOVKA;
 * - STRUCTURA_KOMPLECTOVKI.
 *
 * PRODUCT_SERIES не изменяется.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KomplektovkaFormService {

    private static final short DEFAULT_STATUS = 0;

    private static final int DEFAULT_ASSEMBLY_DIVISION = 3;

    private final KomplektovkaRepository komplektovkaRepository;

    private final StructuraKomplektovkiRepository structureRepository;

    private final SlZapiskaRepository slZapiskaRepository;

    private final Specif22Repository specif22Repository;

    private final AssemblyPreviewService assemblyPreviewService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Формирует комплектовку.
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

        acquireFormationLock(slZapiskaId, specifId);

        if (!slZapiskaRepository.isSlZapiskaAgreed(slZapiskaId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Служебная записка не согласована"
            );
        }

        if (!slZapiskaRepository.isAssemblyBelongsToSlZapiska(
                slZapiskaId,
                specifId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Выбранная сборка не относится к указанной служебной записке"
            );
        }

        if (komplektovkaRepository
                .findActiveBySlZapiskaAndSpecif(slZapiskaId, specifId)
                .isPresent()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Для этой сборки уже существует действующая КВ"
            );
        }

        var rootSpecif = specif22Repository.findById(specifId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Спецификация не найдена: " + specifId
                ));

        List<AssemblyHierarchyResponseDto> hierarchy =
                assemblyPreviewService.getAssemblyPreview(specifId);

        if (hierarchy == null || hierarchy.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Структура выбранной сборки пуста"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        KomplektovkaEntity komplektovka = new KomplektovkaEntity();

        komplektovka.setIdSz(slZapiskaId);
        komplektovka.setSborkiSz(String.valueOf(specifId));
        komplektovka.setDateCreate(now);
        komplektovka.setStatus(DEFAULT_STATUS);
        komplektovka.setFormedBy(request.formedBy());

        String designation = rootSpecif.getObozn();

        if (designation == null || designation.isBlank()) {
            designation = "СБ-" + specifId;
        }

        komplektovka.setDesign("КВ-" + designation);

        KomplektovkaEntity savedKomplektovka =
                komplektovkaRepository.save(komplektovka);

        Integer komplektovkaId = savedKomplektovka.getId();

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

        List<Long> generatedIds =
                structureRepository.getNextIds(nodes.size());

        if (generatedIds.size() != nodes.size()) {
            throw new IllegalStateException(
                    "Не удалось получить ID для всех элементов структуры"
            );
        }

        List<StructuraKomplektovkiEntity> structureEntities =
                buildStructureEntities(
                        nodes,
                        generatedIds,
                        komplektovkaId,
                        specifId
                );

        structureRepository.saveAll(structureEntities);

        log.info(
                "КВ сформирована: id={}, designation={}, structureElements={}",
                komplektovkaId,
                savedKomplektovka.getDesign(),
                structureEntities.size()
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
     * Блокировка параллельного формирования одной КВ.
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
     * Собирает подходящие узлы дерева.
     *
     * Если узел не подходит по сериям, он и его потомки
     * не попадают в структуру комплектовки.
     */
    private void collectSuitableNodes(
            AssemblyHierarchyResponseDto dto,
            StructureNodeData parent,
            List<StructureNodeData> result
    ) {
        if (Boolean.FALSE.equals(dto.fitsSeries())) {
            return;
        }

        StructureNodeData current = new StructureNodeData(dto, parent);

        result.add(current);

        if (dto.children() != null) {
            for (AssemblyHierarchyResponseDto child : dto.children()) {
                collectSuitableNodes(child, current, result);
            }
        }
    }

    /**
     * Создаёт сущности STRUCTURA_KOMPLECTOVKI.
     *
     * Для текущей Entity с @IdClass все шесть компонентов
     * первичного ключа заполняются непосредственно.
     */
    private List<StructuraKomplektovkiEntity> buildStructureEntities(
            List<StructureNodeData> nodes,
            List<Long> generatedIds,
            Integer komplektovkaId,
            Integer rootSpecifId
    ) {
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

            Integer parentId = nodeData.parent() == null
                    ? null
                    : assignedIds.get(nodeData.parent());

            StructuraKomplektovkiEntity entity =
                    new StructuraKomplektovkiEntity();

            entity.setId(id);
            entity.setIdKomlectovka(komplektovkaId);
            entity.setIdSpecif(dto.id());

            entity.setIdDivision(
                    dto.idDivision() != null
                            ? dto.idDivision()
                            : DEFAULT_ASSEMBLY_DIVISION
            );

            entity.setIdRootSpecif(rootSpecifId);

            entity.setIdOriginalStructure(
                    dto.structureId() != null
                            ? dto.structureId()
                            : 0
            );

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
     * Временный узел дерева.
     * Не является JPA-сущностью.
     */
    private record StructureNodeData(
            AssemblyHierarchyResponseDto dto,
            StructureNodeData parent
    ) {
    }
}
