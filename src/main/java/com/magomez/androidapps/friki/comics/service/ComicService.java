package com.magomez.androidapps.friki.comics.service;

import com.magomez.androidapps.friki.comics.converter.ComicConverter;
import com.magomez.androidapps.friki.comics.dao.ComicDao;
import com.magomez.androidapps.friki.comics.dto.CreateComicRequest;
import com.magomez.androidapps.friki.comics.dto.Comic;
import com.magomez.androidapps.friki.comics.dto.ComicDTO;
import com.magomez.androidapps.friki.comics.dto.ComicFilterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ComicService {

    private static final String INVALID_PARAMETERS = "Invalid Parameters";
    private final ComicDao comicDao;

    @Autowired
    public ComicService(ComicDao monumentDao){
        this.comicDao = monumentDao;
    }

    public List<ComicDTO> search(ComicFilterRequest requestFilter){
        List<Comic> monuments =  comicDao.search(requestFilter);
        return ComicConverter.toDtoList(monuments);
    }

    public ComicDTO getComic(Integer comicId){
        Comic comic =  comicDao.getComic(comicId);
        return ComicConverter.toDto(comic);
    }

    public void createComic(CreateComicRequest request){
        if(request == null || request.name() == null || request.category() == null){
            throw new ResponseStatusException(HttpStatus.CONFLICT, INVALID_PARAMETERS);
        }
        comicDao.createComic(request);
    }
}
