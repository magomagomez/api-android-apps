package com.magomez.androidapps.regalos.items;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = RegalosApi.BASE_URL + "/me/items", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Regalos - mi carta", description = "Mis deseos, sin nada de quién los regala")
public class OwnItemController {

    private final ItemService itemService;

    public OwnItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @Operation(summary = "My wishes, ACTIVE by default or RECEIVED, in the order I wrote them")
    @GetMapping
    public List<OwnItem> list(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                              @RequestParam(required = false) ItemStatus status) {
        return itemService.list(me.id(), status);
    }

    @Operation(summary = "Add a wish to my letter")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OwnItem create(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                          @RequestBody ItemRequest request) {
        return itemService.create(me.id(), request);
    }

    @Operation(summary = "Edit an ACTIVE wish of mine")
    @PutMapping("{id}")
    public OwnItem update(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                          @PathVariable long id, @RequestBody ItemRequest request) {
        return itemService.update(me.id(), id, request);
    }

    @Operation(summary = "Mark a wish as received: it leaves the others' view of my letter")
    @PostMapping("{id}/received")
    public OwnItem received(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                            @PathVariable long id) {
        return itemService.received(me.id(), id);
    }

    @Operation(summary = "Undo received: the wish is pending again")
    @DeleteMapping("{id}/received")
    public OwnItem notReceived(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                               @PathVariable long id) {
        return itemService.notReceived(me.id(), id);
    }

    @Operation(summary = "Delete a wish of mine (the same 204 whether someone gives it or not)")
    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                       @PathVariable long id) {
        itemService.delete(me.id(), id);
    }
}
