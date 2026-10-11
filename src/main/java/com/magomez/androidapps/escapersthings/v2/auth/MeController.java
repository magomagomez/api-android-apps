package com.magomez.androidapps.escapersthings.v2.auth;

import com.magomez.androidapps.escapersthings.v2.config.ApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiConfig.BASE_URL)
@Tag(name = "Escapers v2 - usuarios", description = "Usuario actual y grupo")
public class MeController {

    private final AuthService authService;

    public MeController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "User of the token")
    @GetMapping("/me")
    public UserDTO me(@Parameter(hidden = true) @RequestAttribute(CurrentUser.ATTRIBUTE) CurrentUser user) {
        return user.toDto();
    }

    @Operation(summary = "Change my password (needs the current one; closes my other sessions)")
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Parameter(hidden = true) @RequestAttribute(CurrentUser.ATTRIBUTE) CurrentUser user,
                               @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user, request);
    }

    @Operation(summary = "Everyone in the group, by id")
    @GetMapping("/users")
    public List<UserDTO> users() {
        return authService.users();
    }
}
