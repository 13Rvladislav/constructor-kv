
package ru.constructor.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ru.constructor.dto.request.KomplektovkaFormRequestDto;
import ru.constructor.dto.response.KomplektovkaFormResponseDto;
import ru.constructor.service.KomplektovkaFormService;

/**
 * REST-контроллер комплектовок.
 *
 * Отвечает за HTTP API формирования КВ.
 *
 * Бизнес-логика находится в KomplektovkaFormService,
 * поэтому контроллер не выполняет запросы к репозиториям
 * и не содержит правил формирования структуры.
 */
@RestController
@RequestMapping("/api/kv")
@RequiredArgsConstructor
public class KomplektovkaController {

    private final KomplektovkaFormService komplektovkaFormService;

    /**
     * Сформировать комплектовку.
     *
     * POST /api/kv/form
     *
     * Пример запроса:
     *
     * {
     *   "slZapiskaId": 11,
     *   "specifId": 3,
     *   "formedBy": 1
     * }
     *
     * @param request параметры формирования
     * @return информация о созданной КВ
     */
    @PostMapping("/form")
    public ResponseEntity<KomplektovkaFormResponseDto> formKomplektovka(
            @Valid @RequestBody KomplektovkaFormRequestDto request
    ) {
        KomplektovkaFormResponseDto response =
                komplektovkaFormService.formKomplektovka(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
