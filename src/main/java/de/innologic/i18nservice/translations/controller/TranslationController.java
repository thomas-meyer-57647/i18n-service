package de.innologic.i18nservice.translations.controller;

import de.innologic.i18nservice.translations.dto.TranslationsResponse;
import de.innologic.i18nservice.translations.service.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/{projectKey}/translations")
@Tag(name = "Translations", description = "Read translations for a language")
public class TranslationController {

    private final TranslationService service;

    public TranslationController(TranslationService service) {
        this.service = service;
    }

    @GetMapping("/{languageCode}")
    @Operation(
            summary = "Runtime translations",
            description = "Fetches the effective translations for the requested language. Requires scope i18n:read by default.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Translations"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Translations retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TranslationsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Project or language not found")
    })
    public TranslationsResponse get(
            @PathVariable @Parameter(description = "Project key", example = "portal") @NotBlank String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") @NotBlank String languageCode,
            @RequestParam(name = "keys", required = false)
            @Parameter(description = "Comma-separated list of translation keys to limit the result set", example = "welcome.title,home.cta", in = ParameterIn.QUERY)
                    String keys,
            @RequestParam(name = "prefix", required = false)
            @Parameter(description = "Load keys that start with the provided prefix", example = "home.", in = ParameterIn.QUERY)
                    String prefix
    ) {
        return service.getTranslations(projectKey, languageCode, keys, prefix);
    }
}
