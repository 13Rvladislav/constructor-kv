package ru.constructor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.constructor.dto.request.KvTableRequestDto;
import ru.constructor.dto.response.KvTableResponseDto;
import ru.constructor.entity.IzdelieEntity;
import ru.constructor.entity.TemaEntity;
import ru.constructor.repository.IzdelieRepository;
import ru.constructor.repository.KvTableRepository;
import ru.constructor.repository.TemaRepository;
import ru.constructor.repository.projection.KvTableRowProjection;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class KvTableService {

    private static final String VERSION = "1.0";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final KvTableRepository kvTableRepository;
    private final TemaRepository temaRepository;
    private final IzdelieRepository izdelieRepository;

    /**
     * Получить сводную таблицу КВ для выбранной группы.
     */
    public KvTableResponseDto getKvTable(KvTableRequestDto request) {

        Integer temaId = request.temaId();
        Integer izdelieId = request.izdelieId();
        Integer groupeId = request.groupeId();

        log.info(
                "Построение таблицы КВ: temaId={}, izdelieId={}, groupeId={}",
                temaId,
                izdelieId,
                groupeId
        );

        KvTableResponseDto.GroupInfo groupInfo =
                buildGroupInfo(temaId, izdelieId, groupeId);

        if (!kvTableRepository.existsRootSpecification(
                temaId,
                izdelieId,
                groupeId
        )) {
            log.info(
                    "Корневая спецификация не найдена: temaId={}, izdelieId={}, groupeId={}",
                    temaId,
                    izdelieId,
                    groupeId
            );

            return KvTableResponseDto.empty(groupInfo);
        }

        List<KvTableRowProjection> projections =
                kvTableRepository.findTableRows(
                        temaId,
                        izdelieId,
                        groupeId
                );

        AtomicInteger rowNumber = new AtomicInteger(0);

        List<KvTableResponseDto.KvTableRowDto> rows = projections.stream()
                .filter(Objects::nonNull)
                .map(row -> new KvTableResponseDto.KvTableRowDto(
                        rowNumber.incrementAndGet(),
                        row.getSlZapiskaId(),
                        row.getSlZapiskaObozn(),
                        row.getKomplektovkaDesign(),
                        row.getKomplektovkaId(),
                        row.getSpecifId(),
                        formatDate(row.getSlZapAgreedDate()),
                        row.getStatus(),
                        row.getParentKvDesign(),
                        row.getKvStatusId(),
                        Boolean.TRUE,
                        Boolean.TRUE.equals(row.getHasKv())
                ))
                .toList();

        log.info(
                "Таблица КВ сформирована: количество строк={}",
                rows.size()
        );

        return new KvTableResponseDto(
                groupInfo,
                rows,
                rows.size(),
                VERSION
        );
    }

    /**
     * Получить информацию о теме и изделии.
     */
    private KvTableResponseDto.GroupInfo buildGroupInfo(
            Integer temaId,
            Integer izdelieId,
            Integer groupeId
    ) {
        String temaName = temaRepository.findById(temaId)
                .map(TemaEntity::getNaim)
                .filter(name -> name != null && !name.isBlank())
                .orElse("Тема " + temaId);

        String izdelieName = izdelieRepository.findById(izdelieId)
                .map(this::resolveIzdelieName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("Изделие " + izdelieId);

        return new KvTableResponseDto.GroupInfo(
                temaId,
                temaName,
                izdelieId,
                izdelieName,
                groupeId
        );
    }

    private String resolveIzdelieName(IzdelieEntity izdelie) {

        if (izdelie.getNaimIzd() != null
                && !izdelie.getNaimIzd().isBlank()) {
            return izdelie.getNaimIzd();
        }

        return izdelie.getName() != null
                ? izdelie.getName()
                : "";
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null
                ? null
                : dateTime.format(DATE_FORMATTER);
    }
}