package com.magomez.androidapps.jcwedding.attendant.service;

import com.magomez.androidapps.jcwedding.attendant.converter.AttendantConverter;
import com.magomez.androidapps.jcwedding.attendant.dto.Companion;
import com.magomez.androidapps.jcwedding.attendant.dto.RequestAttendantDTO;
import com.magomez.androidapps.jcwedding.attendant.dto.SearchAttendantDTO;
import com.magomez.androidapps.jcwedding.attendant.repository.AttendantRepository;
import com.magomez.androidapps.jcwedding.attendant.dto.Attendant;
import com.magomez.androidapps.jcwedding.attendant.dto.AttendantDTO;
import com.magomez.androidapps.jcwedding.mail.MailDTO;
import com.magomez.androidapps.jcwedding.mail.MailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AttendantService {

    private final AttendantRepository attendantRepository;
    private final MailSenderService mailSenderService;

    @Autowired
    public AttendantService(AttendantRepository attendantRepository, MailSenderService mailSenderService){
        this.attendantRepository = attendantRepository;
        this.mailSenderService = mailSenderService;
    }

    public List<AttendantDTO> searchAttendants(SearchAttendantDTO attendantSearch){
        List<Attendant> attendants = attendantRepository.searchAttendants(attendantSearch);
        if(attendants.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendant not found");
        }
        for(Attendant attendant : attendants){
            List<Companion> companions = attendantRepository.searchCompanions(attendant.getId());
            attendant.setCompanion(companions);
        }
        return AttendantConverter.toDto(attendants);
    }

    public void updateAttendants(RequestAttendantDTO requestAttendantDTO){
        attendantRepository.updateAttendants(requestAttendantDTO);
        confirmationMail(requestAttendantDTO);
    }

    private void confirmationMail(RequestAttendantDTO requestAttendantDTO) {
        List<Attendant> attendants = attendantRepository.getAttendantsbyId(requestAttendantDTO);
        String msg = "Han confirmat els següensts convidats : " ;
        for(Attendant attendant : attendants){
            msg = msg + " " + attendant.getName() + " " + attendant.getSurname() +"!";
        }
        MailDTO mailDTO = new MailDTO();
        mailDTO.setConfirmMessage(msg);
        mailDTO.setType(3);
        mailSenderService.sendMail(mailDTO);
    }

}
