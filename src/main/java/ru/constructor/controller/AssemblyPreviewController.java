package ru.constructor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.constructor.dto.request.AssemblyPreviewRequestDto;
import ru.constructor.dto.response.AssemblyHierarchyResponseDto;
import ru.constructor.service.AssemblyPreviewService;

import java.util.List;

@RestController
@RequestMapping("/api/kv/preview")
@RequiredArgsConstructor
@Tag(
        name = "Предпросмотр КВ",
        description = "Получение иерархии сборок перед формированием КВ"
)
public class AssemblyPreviewController {

    private final AssemblyPreviewService assemblyPreviewService;

    @GetMapping("/assembly")
    @Operation(
            summary = "Получить иерархию сборки",
            description = """
                    Возвращает дерево сборки с содержимым,
                    сериями и результатами проверки серийности.
                    """
    )
    public List<AssemblyHierarchyResponseDto> previewAssembly(
            @Valid
            @ModelAttribute
            @Parameter(description = "Параметры предпросмотра сборки")
            AssemblyPreviewRequestDto request
    ) {
        return assemblyPreviewService.getAssemblyPreview(
                request.specifId()
        );
    }
}