package com.magomez.androidapps.regalos.auth;

import com.magomez.androidapps.regalos.config.RegalosApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = RegalosApi.BASE_URL + "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Regalos - sesión", description = "Crear cuenta, entrar y salir")
public class RegalosAuthController {

    private final RegalosAuthService authService;

    public RegalosAuthController(RegalosAuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Create an account with the group's invite code; returns a Bearer token")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest request, HttpServletRequest http) {
        return authService.register(request, ClientIp.of(http));
    }

    @Operation(summary = "Log in; returns a Bearer token")
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, ClientIp.of(http));
    }

    @Operation(summary = "Log out: revokes the token of this request")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser user) {
        authService.logout(user);
    }
}
