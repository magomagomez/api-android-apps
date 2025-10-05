package com.magomez.androidapps.friki.comics.controller;
import com.magomez.androidapps.friki.comics.dto.CreateComicRequest;
import com.magomez.androidapps.friki.comics.dto.ComicDTO;
import com.magomez.androidapps.friki.comics.dto.ComicFilterRequest;
import com.magomez.androidapps.friki.comics.service.ComicService;
import com.magomez.androidapps.friki.config.ApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = ApiConfig.BASE_URL + "/comics")
@Tag(name = "Comics", description = "Endpoints de Comics")
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class ComicController {

    private final ComicService comicsService;

    @Autowired
    public ComicController(ComicService comicsService){
        this.comicsService = comicsService;
    }

    @Operation(summary = "Seach Comics")
    @GetMapping
    public List<ComicDTO> search(@Valid ComicFilterRequest requestFilter) {

        return comicsService.search(requestFilter);
    }

    @Operation(summary = "Get comic by id")
    @GetMapping("{comicId}")
    public ComicDTO getComic(@PathVariable Integer comicId) {

        return comicsService.getComic(comicId);
    }

    @Operation(summary = "Create Comics")
    @PostMapping
    public void createComic(@RequestBody CreateComicRequest request) {
        comicsService.createComic(request);
    }

}
