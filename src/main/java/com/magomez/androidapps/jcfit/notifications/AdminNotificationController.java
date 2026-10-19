package com.magomez.androidapps.jcfit.notifications;

import com.magomez.androidapps.jcfit.config.JcfitApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = JcfitApi.ADMIN_URL + "/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "JCFit - backoffice avisos", description = "Notificaciones push a la app")
public class AdminNotificationController {

    private final AdminNotificationService notificationService;

    public AdminNotificationController(AdminNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Send a notice to every device of the app account (503 without Firebase, 502 if none got it)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminNotification send(@RequestBody AdminNotificationRequest request) {
        return notificationService.send(request);
    }

    @Operation(summary = "Notices sent, newest first (limit 1-100, default 20)")
    @GetMapping
    public List<AdminNotification> list(@RequestParam(required = false) Integer limit) {
        return notificationService.list(limit);
    }
}
