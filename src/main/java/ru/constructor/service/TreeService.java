
package ru.constructor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ru.constructor.dto.response.FullTreeResponseDto;
import ru.constructor.dto.response.TemaInfoResponseDto;
import ru.constructor.entity.IzdelieEntity;
import ru.constructor.entity.TemaEntity;
import ru.constructor.repository.GroupeRepository;
import ru.constructor.repository.IzdelieRepository;
import ru.constructor.repository.TemaRepository;
import ru.constructor.repository.projection.GroupeTreeProjection;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TreeService {

    private static final String VERSION = "1.0";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final TemaRepository temaRepository;
    private final IzdelieRepository izdelieRepository;
    private final GroupeRepository groupeRepository;

    /**
     * Построение полного дерева:
     * Тема -> Изделия -> Группы.
     */
    public FullTreeResponseDto buildFullTree() {
        log.info("Building full PDM navigation tree");

        List<TemaEntity> temas = temaRepository.findAllByOrderByIdAsc();

        if (temas.isEmpty()) {
            return FullTreeResponseDto.empty();
        }

        List<IzdelieEntity> izdelia = izdelieRepository.findAllByOrderByIdAsc();

        List<GroupeTreeProjection> groups =
                groupeRepository.findAllGroupsForTree();

        Map<Integer, List<FullTreeResponseDto.GroupeNode>> groupsByProduct =
                buildGroupsMap(groups);

        Map<Integer, List<IzdelieEntity>> productsByTema =
                new HashMap<>();

        for (IzdelieEntity izdelie : izdelia) {
            productsByTema
                    .computeIfAbsent(
                            izdelie.getIdTema(),
                            ignored -> new ArrayList<>()
                    )
                    .add(izdelie);
        }

        List<FullTreeResponseDto.TemaNode> temaNodes =
                new ArrayList<>();

        long totalNodes = 0;

        for (TemaEntity tema : temas) {
            List<IzdelieEntity> temaProducts =
                    productsByTema.getOrDefault(
                            tema.getId(),
                            List.of()
                    );

            List<FullTreeResponseDto.IzdelieNode> izdelieNodes =
                    new ArrayList<>();

            for (IzdelieEntity izdelie : temaProducts) {
                List<FullTreeResponseDto.GroupeNode> productGroups =
                        groupsByProduct.getOrDefault(
                                izdelie.getId(),
                                List.of()
                        );

                izdelieNodes.add(
                        new FullTreeResponseDto.IzdelieNode(
                                izdelie.getId(),
                                defaultValue(izdelie.getNaimIzd(), "Без названия"),
                                defaultValue(izdelie.getName(), ""),
                                formatDate(izdelie),
                                productGroups
                        )
                );

                totalNodes += 1 + productGroups.size();
            }

            temaNodes.add(
                    new FullTreeResponseDto.TemaNode(
                            tema.getId(),
                            defaultValue(tema.getNaim(), "Без названия"),
                            tema.getIndeksKd(),
                            izdelieNodes
                    )
            );

            totalNodes++;
        }

        log.info(
                "PDM tree built. Themes: {}, total nodes: {}",
                temaNodes.size(),
                totalNodes
        );

        return new FullTreeResponseDto(
                temaNodes,
                totalNodes,
                VERSION
        );
    }

    /**
     * Построение дерева для одной темы.
     */
    public FullTreeResponseDto buildTreeByTemaId(Integer temaId) {
        if (temaId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Идентификатор темы обязателен"
            );
        }

        TemaEntity tema = temaRepository.findById(temaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Тема с ID " + temaId + " не найдена"
                ));

        List<IzdelieEntity> izdelia =
                izdelieRepository.findByIdTemaOrderByIdAsc(temaId);

        List<GroupeTreeProjection> allGroups =
                groupeRepository.findAllGroupsForTree();

        Map<Integer, List<FullTreeResponseDto.GroupeNode>> groupsByProduct =
                buildGroupsMap(allGroups);

        List<FullTreeResponseDto.IzdelieNode> izdelieNodes =
                new ArrayList<>();

        long totalNodes = 1;

        for (IzdelieEntity izdelie : izdelia) {
            List<FullTreeResponseDto.GroupeNode> groups =
                    groupsByProduct.getOrDefault(
                            izdelie.getId(),
                            List.of()
                    );

            izdelieNodes.add(
                    new FullTreeResponseDto.IzdelieNode(
                            izdelie.getId(),
                            defaultValue(izdelie.getNaimIzd(), "Без названия"),
                            defaultValue(izdelie.getName(), ""),
                            formatDate(izdelie),
                            groups
                    )
            );

            totalNodes += 1 + groups.size();
        }

        FullTreeResponseDto.TemaNode temaNode =
                new FullTreeResponseDto.TemaNode(
                        tema.getId(),
                        defaultValue(tema.getNaim(), "Без названия"),
                        tema.getIndeksKd(),
                        izdelieNodes
                );

        return new FullTreeResponseDto(
                List.of(temaNode),
                totalNodes,
                VERSION
        );
    }

    /**
     * Получение информации о теме без изделий.
     */
    public TemaInfoResponseDto getTemaInfo(Integer temaId) {
        TemaEntity tema = temaRepository.findById(temaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Тема с ID " + temaId + " не найдена"
                ));

        TemaInfoResponseDto.TemaInfoNode node =
                new TemaInfoResponseDto.TemaInfoNode(
                        tema.getId(),
                        tema.getNaim(),
                        tema.getIndeksKd()
                );

        return new TemaInfoResponseDto(
                List.of(node),
                1,
                VERSION
        );
    }

    /**
     * Поиск тем по названию.
     */
    public TemaInfoResponseDto searchTemas(String query) {
        if (query == null || query.isBlank()) {
            return TemaInfoResponseDto.empty();
        }

        List<TemaInfoResponseDto.TemaInfoNode> nodes =
                temaRepository
                        .findByNaimContainingIgnoreCaseOrderByIdAsc(query.trim())
                        .stream()
                        .map(tema -> new TemaInfoResponseDto.TemaInfoNode(
                                tema.getId(),
                                tema.getNaim(),
                                tema.getIndeksKd()
                        ))
                        .toList();

        return new TemaInfoResponseDto(
                nodes,
                nodes.size(),
                VERSION
        );
    }

    private Map<Integer, List<FullTreeResponseDto.GroupeNode>> buildGroupsMap(
            List<GroupeTreeProjection> groups
    ) {
        Map<Integer, List<FullTreeResponseDto.GroupeNode>> result =
                new LinkedHashMap<>();

        for (GroupeTreeProjection group : groups) {
            result.computeIfAbsent(
                    group.getIzdelieId(),
                    ignored -> new ArrayList<>()
            ).add(
                    new FullTreeResponseDto.GroupeNode(
                            group.getGroupeId(),
                            defaultValue(group.getGroupeName(), "Без названия")
                    )
            );
        }

        return result;
    }

    private String formatDate(IzdelieEntity izdelie) {
        if (izdelie.getDateAdd() == null) {
            return null;
        }

        return izdelie.getDateAdd().format(DATE_FORMATTER);
    }

    private String defaultValue(String value, String fallback) {
        return value != null ? value : fallback;
    }
}
