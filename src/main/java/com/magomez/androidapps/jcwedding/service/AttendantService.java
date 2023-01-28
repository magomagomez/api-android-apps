package com.magomez.androidapps.jcwedding.service;

import com.magomez.androidapps.jcwedding.converter.AttendantConverter;
import com.magomez.androidapps.jcwedding.dto.Companion;
import com.magomez.androidapps.jcwedding.dto.RequestAttendantDTO;
import com.magomez.androidapps.jcwedding.dto.SearchAttendantDTO;
import com.magomez.androidapps.jcwedding.repository.AttendantRepository;
import com.magomez.androidapps.jcwedding.dto.Attendant;
import com.magomez.androidapps.jcwedding.dto.AttendantDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttendantService {

    private final AttendantRepository attendantRepository;

    @Autowired
    public AttendantService(AttendantRepository attendantRepository){
        this.attendantRepository = attendantRepository;
    }

    public List<AttendantDTO> searchAttendants(SearchAttendantDTO attendantSearch){
        List<Attendant> attendants = attendantRepository.searchAttendants(attendantSearch);
        for(Attendant attendant : attendants){
            List<Companion> companions = attendantRepository.searchCompanions(attendant.getId());
            attendant.setCompanion(companions);
        }
        return AttendantConverter.toDto(attendants);
    }

    public void updateAttendants(RequestAttendantDTO requestAttendantDTO){
        attendantRepository.updateAttendants(requestAttendantDTO);
    }

}
