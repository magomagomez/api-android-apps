package com.magomez.androidapps.regalos.claims;

import com.magomez.androidapps.regalos.auth.RegalosCurrentUser;
import com.magomez.androidapps.regalos.config.RegalosApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = RegalosApi.BASE_URL, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Regalos - regalar", description = "Cartas de los demás, marcas y mis regalos")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @Operation(summary = "Someone else's ACTIVE wishes with who gives each (403 for my own id)")
    @GetMapping("/people/{userId}/items")
    public PersonItems letter(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                              @PathVariable int userId) {
        return claimService.listOf(me.id(), userId);
    }

    @Operation(summary = "Claim a wish, or change my claim (409 if someone else has it)")
    @PutMapping("/items/{id}/claim")
    public OthersItem claim(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                            @PathVariable long id, @RequestBody ClaimRequest request) {
        return claimService.claim(me.id(), id, request);
    }

    @Operation(summary = "Release my claim on a wish")
    @DeleteMapping("/items/{id}/claim")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unclaim(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                        @PathVariable long id) {
        claimService.unclaim(me.id(), id);
    }

    @Operation(summary = "My gifts grouped by person, with what I spent and what is left to buy")
    @GetMapping("/me/claims")
    public MyClaims myClaims(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me) {
        return claimService.myClaims(me.id());
    }
}
