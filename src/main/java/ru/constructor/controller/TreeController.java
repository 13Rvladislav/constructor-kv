
package ru.constructor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.constructor.dto.response.FullTreeResponseDto;
import ru.constructor.dto.response.TemaInfoResponseDto;
import ru.constructor.service.TreeService;

@RestController
@RequestMapping("/api/tree")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "PDM — дерево навигации",
        description = "Просмотр тем, изделий и групп"
)
public class TreeController {

    private final TreeService treeService;

    @GetMapping("/full")
    @Operation(
            summary = "Получить полное дерево PDM",
            description = "Возвращает иерархию Тема → Изделие → Группа"
    )
    public FullTreeResponseDto getFullTree() {
        return treeService.buildFullTree();
    }

    @GetMapping("/tema/{temaId}")
    @Operation(
            summary = "Получить дерево темы",
            description = "Возвращает изделия и группы выбранной темы"
    )
    public FullTreeResponseDto getTreeByTemaId(
            @Parameter(description = "Идентификатор темы", example = "4")
            @PathVariable
            @Positive
            Integer temaId
    ) {
        return treeService.buildTreeByTemaId(temaId);
    }

    @GetMapping("/tema/{temaId}/info")
    @Operation(
            summary = "Получить информацию о теме",
            description = "Возвращает информацию о теме без вложенных изделий"
    )
    public TemaInfoResponseDto getTemaInfo(
            @Parameter(description = "Идентификатор темы", example = "4")
            @PathVariable
            @Positive
            Integer temaId
    ) {
        return treeService.getTemaInfo(temaId);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Поиск тем",
            description = "Поиск тем по части наименования без учёта регистра"
    )
    public TemaInfoResponseDto searchTemas(
            @Parameter(description = "Строка поиска", example = "А-100")
            @RequestParam
            String query
    ) {
        return treeService.searchTemas(query);
    }
}
