package com.magomez.androidapps.jcfit.auth;

import com.magomez.androidapps.jcfit.config.JcfitApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = JcfitApi.ADMIN_URL + "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "JCFit - sesión del backoffice", description = "Entrar y salir del backoffice")
public class JcfitAdminAuthController {

    private final JcfitAuthService authService;

    public JcfitAdminAuthController(JcfitAuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Log in to the backoffice: only its account; returns a Bearer token for /admin")
    @PostMapping("/login")
    public JcfitLoginResponse login(@RequestBody JcfitLoginRequest request) {
        return authService.login(request, JcfitRole.ADMIN);
    }

    @Operation(summary = "Log out of the backoffice: revokes the token of this request")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Parameter(hidden = true) @RequestAttribute(JcfitCurrentUser.ATTRIBUTE) JcfitCurrentUser user) {
        authService.logout(user);
    }
}
