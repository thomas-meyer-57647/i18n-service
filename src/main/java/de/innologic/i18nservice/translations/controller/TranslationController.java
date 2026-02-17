package de.innologic.i18nservice.translations.controller;

import de.innologic.i18nservice.translations.dto.TranslationsResponse;
import de.innologic.i18nservice.translations.service.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/{projectKey}/translations")
public class TranslationController {

    private final TranslationService service;

    public TranslationController(TranslationService service) {
        this.service = service;
    }

    /**
     * GET /api/v1/{projectKey}/translations/{languageCode}
     * Optional:
     *  - ?keys=A,B,C
     *  - ?prefix=HOME.
     *  - (ohne Parameter) => komplettes Bundle
     */
    @GetMapping("/{languageCode}")
    @Operation(
            summary = "Runtime translations",
            description = "Requires scope i18n:read by default. Can be public when app.runtime.public=true.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public TranslationsResponse get(
            @PathVariable @NotBlank String projectKey,
            @PathVariable @NotBlank String languageCode,
            @RequestParam(name = "keys", required = false) String keys,
            @RequestParam(name = "prefix", required = false) String prefix
    ) {
        return service.getTranslations(projectKey, languageCode, keys, prefix);
    }
}
