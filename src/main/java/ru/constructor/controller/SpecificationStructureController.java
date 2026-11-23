
package ru.constructor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.constructor.dto.response.SpecificationStructureResponseDto;
import ru.constructor.service.SpecificationStructureService;

import java.util.List;

/**
 * API просмотра полной структуры спецификации.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(
        name = "Структура спецификации",
        description = "Получение полной структуры спецификации вместе с сериями"
)
public class SpecificationStructureController {

    private final SpecificationStructureService specificationStructureService;

    /**
     * Получить структуру спецификации по REST-пути с ID.
     *
     * GET /api/specifications/{specifId}/structure
     */
    @GetMapping("/specifications/{specifId}/structure")
    @Operation(
            summary = "Получить полную структуру спецификации"
    )
    public ResponseEntity<List<SpecificationStructureResponseDto>>
    getSpecificationStructure(
            @Parameter(description = "Идентификатор спецификации")
            @PathVariable Integer specifId
    ) {

        return ResponseEntity.ok(
                specificationStructureService.getSpecificationStructure(
                        specifId
                )
        );
    }

    /**
     * Совместимый маршрут для существующего frontend-вызова.
     *
     * GET /api/kv/specification-structure?specifId=3
     */
    @GetMapping("/kv/specification-structure")
    @Operation(
            summary = "Получить структуру спецификации",
            description = "Совместимый endpoint для frontend"
    )
    public ResponseEntity<List<SpecificationStructureResponseDto>>
    getSpecificationStructureByQuery(
            @Parameter(
                    description = "Идентификатор спецификации",
                    example = "3"
            )
            @RequestParam Integer specifId
    ) {

        return ResponseEntity.ok(
                specificationStructureService.getSpecificationStructure(
                        specifId
                )
        );
    }
}
