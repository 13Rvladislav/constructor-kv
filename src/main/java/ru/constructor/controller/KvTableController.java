package ru.constructor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.constructor.dto.request.KvTableRequestDto;
import ru.constructor.dto.response.KvTableResponseDto;
import ru.constructor.service.KvTableService;

@RestController
@RequestMapping("/api/kvtable")
@RequiredArgsConstructor
@Tag(
        name = "Таблица КВ",
        description = "Сводная таблица комплектовочных ведомостей"
)
public class KvTableController {

    private final KvTableService kvTableService;

    @GetMapping
    @Operation(
            summary = "Получить таблицу КВ",
            description = "Получение сводной таблицы по теме, изделию и группе"
    )
    public KvTableResponseDto getKvTable(
            @Valid @ModelAttribute KvTableRequestDto request
    ) {
        return kvTableService.getKvTable(request);
    }
}