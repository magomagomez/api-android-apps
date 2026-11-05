package com.magomez.androidapps.regalos.ideas;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = RegalosApi.BASE_URL, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Regalos - mis ideas", description = "Ideas para regalar a alguien; solo las ve quien las apunta")
public class IdeaController {

    private final IdeaService ideaService;

    public IdeaController(IdeaService ideaService) {
        this.ideaService = ideaService;
    }

    @Operation(summary = "My ideas for that person and those shared with me for them (403 for my own id)")
    @GetMapping("/people/{userId}/ideas")
    public List<Idea> list(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                           @PathVariable int userId) {
        return ideaService.list(me.id(), userId);
    }

    @Operation(summary = "Note an idea for that person, shared with whom I choose (never them)")
    @PostMapping("/people/{userId}/ideas")
    @ResponseStatus(HttpStatus.CREATED)
    public Idea create(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                       @PathVariable int userId, @RequestBody IdeaRequest request) {
        return ideaService.create(me.id(), userId, request);
    }

    @Operation(summary = "Edit one of my ideas, bought included")
    @PutMapping("/ideas/{id}")
    public Idea update(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                       @PathVariable long id, @RequestBody IdeaRequest request) {
        return ideaService.update(me.id(), id, request);
    }

    @Operation(summary = "Give an idea shared with me: RESERVED or BOUGHT (409 if someone else gives it or its author bought it)")
    @PutMapping("/ideas/{id}/claim")
    public Idea claim(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                      @PathVariable long id, @RequestBody IdeaClaimRequest request) {
        return ideaService.claim(me.id(), id, request);
    }

    @Operation(summary = "Stop giving an idea shared with me")
    @DeleteMapping("/ideas/{id}/claim")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unclaim(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                        @PathVariable long id) {
        ideaService.unclaim(me.id(), id);
    }

    @Operation(summary = "Delete one of my ideas")
    @DeleteMapping("/ideas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                       @PathVariable long id) {
        ideaService.delete(me.id(), id);
    }
}
