package com.magomez.androidapps.jcfit.notifications;

import com.magomez.androidapps.jcfit.auth.JcfitCurrentUser;
import com.magomez.androidapps.jcfit.config.JcfitApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = JcfitApi.BASE_URL + "/devices", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "JCFit - dispositivos", description = "Tokens de Firebase Cloud Messaging de la app")
public class JcfitDeviceController {

    private final JcfitDeviceService deviceService;

    public JcfitDeviceController(JcfitDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Operation(summary = "Store or refresh the FCM token of this installation")
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@Parameter(hidden = true) @RequestAttribute(JcfitCurrentUser.ATTRIBUTE) JcfitCurrentUser user,
                         @RequestBody JcfitDeviceRequest request) {
        deviceService.register(user.id(), request);
    }

    @Operation(summary = "Forget an FCM token (on logout); 204 even if it was unknown")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@RequestBody JcfitDeviceRequest request) {
        deviceService.unregister(request);
    }
}
