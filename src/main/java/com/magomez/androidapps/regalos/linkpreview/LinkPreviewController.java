package com.magomez.androidapps.regalos.linkpreview;

import com.magomez.androidapps.regalos.config.RegalosApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = RegalosApi.BASE_URL + "/link-preview", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Regalos - autorrelleno", description = "Lee el enlace de una tienda y propone los datos del deseo")
public class LinkPreviewController {

    private final LinkPreviewService linkPreviewService;

    public LinkPreviewController(LinkPreviewService linkPreviewService) {
        this.linkPreviewService = linkPreviewService;
    }

    @Operation(summary = "Name, photo, price and shop proposed by a shop link (empty fields when the page says nothing)")
    @PostMapping
    public LinkPreview preview(@RequestBody LinkPreviewRequest request) {
        return linkPreviewService.preview(request);
    }
}
