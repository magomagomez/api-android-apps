package com.magomez.androidapps.regalos.people;

import com.magomez.androidapps.regalos.auth.RegalosCurrentUser;
import com.magomez.androidapps.regalos.config.RegalosApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = RegalosApi.BASE_URL, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Regalos - personas", description = "Mi perfil y los demás, por cumpleaños")
public class PeopleController {

    private final PeopleService peopleService;

    public PeopleController(PeopleService peopleService) {
        this.peopleService = peopleService;
    }

    @Operation(summary = "My profile")
    @GetMapping("/me")
    public Me me(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me) {
        return peopleService.me(me.id());
    }

    @Operation(summary = "Change my display name or birthday (null removes it)")
    @PutMapping("/me")
    public Me updateMe(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                       @RequestBody ProfileRequest request) {
        return peopleService.updateMe(me.id(), request);
    }

    @Operation(summary = "Everybody else, nearest birthday first, with their wishes left to give")
    @GetMapping("/people")
    public List<PersonSummary> people(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me) {
        return peopleService.people(me.id());
    }
}
