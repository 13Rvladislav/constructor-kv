
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class KvTableService {

    private static final String VERSION = "1.2";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final List<String> STATUS_OPTIONS = List.of(
            "Не сформирована",
            "В работе",
            "Запущена в производство",
            "Аннулирована"
    );

    private final KvTableRepository kvTableRepository;
    private final TemaRepository temaRepository;
    private final IzdelieRepository izdelieRepository;

    public KvTableResponseDto getKvTable(KvTableRequestDto request) {

        Integer temaId = request.temaId();
        Integer izdelieId = request.izdelieId();
        Integer groupeId = request.groupeId();

        int page = request.resolvedPage();
        int size = request.resolvedSize();

        String search = request.normalizedSearch();
        if (search.isBlank()) {
            search = null;
        }

        String status = request.normalizedStatus();
        Boolean hasKv = request.hasKv();

        log.info(
                "Получение таблицы КВ: temaId={}, izdelieId={}, groupeId={}, " +
                        "search={}, status={}, hasKv={}, page={}, size={}",
                temaId,
                izdelieId,
                groupeId,
                search,
                status,
                hasKv,
                page,
                size
        );

        KvTableResponseDto.GroupInfo groupInfo =
                buildGroupInfo(temaId, izdelieId, groupeId);

        if (!kvTableRepository.existsRootSpecification(
                temaId,
                izdelieId,
                groupeId
        )) {
            return KvTableResponseDto.empty(groupInfo);
        }

        long totalRows = kvTableRepository.countFilteredTableRows(
                temaId,
                izdelieId,
                groupeId,
                search,
                status,
                hasKv
        );

        long offset = (long) page * size;

        List<KvTableRowProjection> projections =
                kvTableRepository.findFilteredTableRows(
                        temaId,
                        izdelieId,
                        groupeId,
                        search,
                        status,
                        hasKv,
                        size,
                        offset
                );

        List<KvTableResponseDto.KvTableRowDto> rows =
                projections.stream()
                        .map(this::mapRow)
                        .toList();

        int totalPages = totalRows == 0
                ? 0
                : (int) Math.ceil((double) totalRows / size);

        log.info(
                "Таблица КВ сформирована: totalRows={}, page={}, " +
                        "size={}, totalPages={}, returnedRows={}",
                totalRows,
                page,
                size,
                totalPages,
                rows.size()
        );

        return new KvTableResponseDto(
                groupInfo,
                rows,
                Math.toIntExact(totalRows),
                VERSION,
                page,
                size,
                totalPages,
                STATUS_OPTIONS
        );
    }

    private KvTableResponseDto.KvTableRowDto mapRow(
            KvTableRowProjection row
    ) {
        return new KvTableResponseDto.KvTableRowDto(
                row.getNumber(),
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
        );
    }

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
