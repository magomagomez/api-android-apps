package com.magomez.androidapps.regalos.lists;

import com.magomez.androidapps.regalos.auth.RegalosCurrentUser;
import com.magomez.androidapps.regalos.claims.OthersItem;
import com.magomez.androidapps.regalos.config.RegalosApi;
import com.magomez.androidapps.regalos.items.ItemRequest;
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
@RequestMapping(value = RegalosApi.BASE_URL + "/lists", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Regalos - listas compartidas", description = "Listas para alguien que no usa la app")
public class ListController {

    private final ListService listService;

    public ListController(ListService listService) {
        this.listService = listService;
    }

    @Operation(summary = "The lists I am invited to, nearest birthday first")
    @GetMapping
    public List<ListSummary> lists(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me) {
        return listService.lists(me.id());
    }

    @Operation(summary = "Create a list for someone outside the app and invite people to it")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListDetail create(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                             @RequestBody ListRequest request) {
        return listService.create(me.id(), request);
    }

    @Operation(summary = "A list, its members and its wishes with who gives each (404 if not invited)")
    @GetMapping("{id}")
    public ListDetail detail(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                             @PathVariable long id) {
        return listService.detail(me.id(), id);
    }

    @Operation(summary = "Rename a list, change its birthday or its members (only its creator)")
    @PutMapping("{id}")
    public ListDetail update(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                             @PathVariable long id, @RequestBody ListRequest request) {
        return listService.update(me.id(), id, request);
    }

    @Operation(summary = "Delete a list with its wishes (only its creator)")
    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                       @PathVariable long id) {
        listService.delete(me.id(), id);
    }

    @Operation(summary = "Leave a list (its creator cannot)")
    @DeleteMapping("{id}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                      @PathVariable long id) {
        listService.leave(me.id(), id);
    }

    @Operation(summary = "Add a wish to a list (any member)")
    @PostMapping("{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public OthersItem addItem(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                              @PathVariable long id, @RequestBody ItemRequest request) {
        return listService.addItem(me.id(), id, request);
    }

    @Operation(summary = "Edit a wish of a list (any member)")
    @PutMapping("{id}/items/{itemId}")
    public OthersItem updateItem(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                                 @PathVariable long id, @PathVariable long itemId, @RequestBody ItemRequest request) {
        return listService.updateItem(me.id(), id, itemId, request);
    }

    @Operation(summary = "Remove a wish from a list (any member)")
    @DeleteMapping("{id}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@Parameter(hidden = true) @RequestAttribute(RegalosCurrentUser.ATTRIBUTE) RegalosCurrentUser me,
                           @PathVariable long id, @PathVariable long itemId) {
        listService.deleteItem(me.id(), id, itemId);
    }
}
