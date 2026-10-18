package com.magomez.androidapps.jcfit.measurements;

import com.magomez.androidapps.jcfit.config.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MeasurementService {

    private static final Logger log = LoggerFactory.getLogger(MeasurementService.class);

    static final String NOT_FOUND = "La medida no existe";
    private static final String MEASUREMENT_EXISTS = "measurement_exists";
    private static final String MEASUREMENT_EXISTS_MESSAGE = "Ya hay un registro ese día";
    private static final Comparator<MeasurementPhoto> BY_POSE = Comparator.comparing(MeasurementPhoto::pose);

    private final MeasurementDao measurementDao;
    private final Clock clock;

    public MeasurementService(MeasurementDao measurementDao, @Qualifier("jcfitClock") Clock clock) {
        this.measurementDao = measurementDao;
        this.clock = clock;
    }

    /** Every measurement newest first, with every photo read in one query. */
    public MeasurementHistory history() {
        List<MeasurementRow> rows = measurementDao.findAll();
        Map<Integer, List<MeasurementPhoto>> photos = measurementDao.findAllPhotos().stream()
                .collect(Collectors.groupingBy(MeasurementPhotoRow::measurementId,
                        Collectors.mapping(MeasurementPhotoRow::photo, Collectors.toList())));
        List<Measurement> items = rows.stream()
                .map(row -> row.withPhotos(sorted(photos.getOrDefault(row.id(), List.of()))))
                .toList();
        if (items.isEmpty()) {
            return new MeasurementHistory(null, null, items);
        }
        return new MeasurementHistory(items.get(items.size() - 1), items.get(0), items);
    }

    public Measurement get(int id) {
        MeasurementRow row = measurementDao.findById(id).orElseThrow(() -> ApiException.notFound(NOT_FOUND));
        List<MeasurementPhoto> photos = measurementDao.findPhotos(id).stream().map(MeasurementPhotoRow::photo).toList();
        return row.withPhotos(sorted(photos));
    }

    public Measurement create(MeasurementRequest request) {
        MeasurementRequest clean = MeasurementValidator.measurement(request, today());
        int id;
        try {
            id = measurementDao.insert(clean);
        } catch (DuplicateKeyException e) {
            throw dayTaken(clean.measuredOn());
        }
        return get(id);
    }

    public Measurement update(int id, MeasurementRequest request) {
        MeasurementRequest clean = MeasurementValidator.measurement(request, today());
        boolean updated;
        try {
            updated = measurementDao.update(id, clean);
        } catch (DuplicateKeyException e) {
            throw dayTaken(clean.measuredOn());
        }
        if (!updated) {
            throw ApiException.notFound(NOT_FOUND);
        }
        return get(id);
    }

    public void delete(int id) {
        if (!measurementDao.delete(id)) {
            throw ApiException.notFound(NOT_FOUND);
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static List<MeasurementPhoto> sorted(List<MeasurementPhoto> photos) {
        return photos.stream().sorted(BY_POSE).toList();
    }

    private static ApiException dayTaken(LocalDate day) {
        log.info("jcfit: there is already a measurement on {}", day);
        return new ApiException(HttpStatus.CONFLICT, MEASUREMENT_EXISTS, MEASUREMENT_EXISTS_MESSAGE);
    }
}
