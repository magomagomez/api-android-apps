package com.magomez.androidapps.legacy;

import com.magomez.androidapps.jcwedding.attendant.dao.AttendantDao;
import com.magomez.androidapps.jcwedding.attendant.dto.RequestAttendantDTO;
import com.magomez.androidapps.jcwedding.attendant.repository.AttendantRepository;
import com.magomez.androidapps.jcwedding.attendant.service.AttendantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttendantServiceTest {

    private RecordingJdbcTemplate jdbc;
    private AttendantService service;

    @BeforeEach
    void setUp() {
        jdbc = new RecordingJdbcTemplate();
        // No mail is ever sent: every request here is rejected before confirmationMail.
        service = new AttendantService(new AttendantRepository(new AttendantDao(jdbc)), null);
    }

    @Test
    void emptyIdListIsRejectedBeforeTouchingTheDatabase() {
        RequestAttendantDTO request = new RequestAttendantDTO();
        request.setId(List.of());

        assertBadRequest(request);
    }

    @Test
    void missingIdListIsRejectedBeforeTouchingTheDatabase() {
        assertBadRequest(new RequestAttendantDTO());
    }

    @Test
    void idsAreBoundAsOnePlaceholderEach() {
        RequestAttendantDTO request = new RequestAttendantDTO();
        request.setId(List.of(4, 8, 15));

        new AttendantDao(jdbc).updateAttendants(request);

        assertThat(jdbc.normalizedSql()).endsWith("where id in (?,?,?)");
        assertThat(jdbc.lastArgs).containsExactly(4, 8, 15);
    }

    private void assertBadRequest(RequestAttendantDTO request) {
        assertThatThrownBy(() -> service.updateAttendants(request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(jdbc.lastSql).isNull();
    }
}
