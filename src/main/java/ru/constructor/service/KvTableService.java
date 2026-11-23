
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
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class KvTableService {

    private static final String VERSION = "1.1";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final KvTableRepository kvTableRepository;
    private final TemaRepository temaRepository;
    private final IzdelieRepository izdelieRepository;

    public KvTableResponseDto getKvTable(KvTableRequestDto request) {

        Integer temaId = request.temaId();
        Integer izdelieId = request.izdelieId();
        Integer groupeId = request.groupeId();

        int page = request.resolvedPage();
        int size = request.resolvedSize();

        log.info(
                "Получение таблицы КВ: temaId={}, izdelieId={}, groupeId={}, " +
                        "search={}, status={}, hasKv={}, page={}, size={}",
                temaId,
                izdelieId,
                groupeId,
                request.normalizedSearch(),
                request.normalizedStatus(),
                request.hasKv(),
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

        List<KvTableRowProjection> allRows =
                kvTableRepository.findTableRows(
                        temaId,
                        izdelieId,
                        groupeId
                );

        List<String> statusOptions = allRows.stream()
                .filter(Objects::nonNull)
                .map(KvTableRowProjection::getStatus)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        String search = request.normalizedSearch()
                .toLowerCase(Locale.ROOT);

        String status = request.normalizedStatus();

        List<KvTableRowProjection> filteredRows = allRows.stream()
                .filter(Objects::nonNull)
                .filter(row -> matchesSearch(row, search))
                .filter(row -> matchesStatus(row, status))
                .filter(row -> matchesHasKv(row, request.hasKv()))
                .toList();

        int totalRows = filteredRows.size();

        int totalPages = totalRows == 0
                ? 0
                : (int) Math.ceil((double) totalRows / size);

        long offsetLong = (long) page * size;

        int fromIndex = offsetLong >= totalRows
                ? totalRows
                : (int) offsetLong;

        int toIndex = Math.min(fromIndex + size, totalRows);

        List<KvTableResponseDto.KvTableRowDto> rows =
                filteredRows.subList(fromIndex, toIndex)
                        .stream()
                        .map(row -> mapRow(row, fromIndex + 1
                                + filteredRows.subList(fromIndex, toIndex)
                                .indexOf(row)))
                        .toList();

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
                totalRows,
                VERSION,
                page,
                size,
                totalPages,
                statusOptions
        );
    }

    private boolean matchesSearch(
            KvTableRowProjection row,
            String search
    ) {
        if (search == null || search.isBlank()) {
            return true;
        }

        return contains(row.getSlZapiskaId(), search)
                || contains(row.getSlZapiskaObozn(), search)
                || contains(row.getSpecifId(), search)
                || contains(row.getSpecifObozn(), search)
                || contains(row.getKomplektovkaId(), search)
                || contains(row.getKomplektovkaDesign(), search)
                || contains(row.getParentKvDesign(), search);
    }

    private boolean matchesStatus(
            KvTableRowProjection row,
            String status
    ) {
        if (status == null) {
            return true;
        }

        return row.getStatus() != null
                && row.getStatus().equalsIgnoreCase(status);
    }

    private boolean matchesHasKv(
            KvTableRowProjection row,
            Boolean hasKv
    ) {
        if (hasKv == null) {
            return true;
        }

        return Boolean.TRUE.equals(row.getHasKv()) == hasKv;
    }

    private boolean contains(Object value, String search) {
        return value != null
                && value.toString()
                .toLowerCase(Locale.ROOT)
                .contains(search);
    }

    private KvTableResponseDto.KvTableRowDto mapRow(
            KvTableRowProjection row,
            int number
    ) {
        return new KvTableResponseDto.KvTableRowDto(
                number,
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
