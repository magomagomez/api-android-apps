package com.magomez.androidapps.friki.comics.controller;
import com.magomez.androidapps.friki.comics.dto.CreateComicRequest;
import com.magomez.androidapps.friki.comics.dto.ComicDTO;
import com.magomez.androidapps.friki.comics.dto.ComicFilterRequest;
import com.magomez.androidapps.friki.comics.service.ComicService;
import com.magomez.androidapps.friki.config.ApiConfig;
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
@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST,RequestMethod.PUT})
public class ComicsController {

    private final ComicService comicsService;

    @Autowired
    public ComicsController(ComicService comicsService){
        this.comicsService = comicsService;
    }

    @GetMapping
    public List<ComicDTO> search(@Valid ComicFilterRequest requestFilter) {

        return comicsService.search(requestFilter);
    }

    @GetMapping("{comicId}")
    public ComicDTO getComic(@PathVariable Integer comicId) {

        return comicsService.getComic(comicId);
    }

    @PostMapping
    public void createComic(@RequestBody CreateComicRequest request) {
        comicsService.createComic(request);
    }

}
