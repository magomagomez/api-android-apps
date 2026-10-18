package com.magomez.androidapps.jcfit.training;

import com.magomez.androidapps.jcfit.config.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** "Empezar", "Guardar serie" and "Terminar": the session of a day and its sets. */
@Service
public class WorkoutSessionService {

    private static final Logger log = LoggerFactory.getLogger(WorkoutSessionService.class);

    private final TrainingProgramDao programDao;
    private final WorkoutSessionDao sessionDao;
    private final SetLogDao setLogDao;
    private final Clock clock;

    public WorkoutSessionService(TrainingProgramDao programDao, WorkoutSessionDao sessionDao, SetLogDao setLogDao,
                                 @Qualifier("jcfitClock") Clock clock) {
        this.programDao = programDao;
        this.sessionDao = sessionDao;
        this.setLogDao = setLogDao;
        this.clock = clock;
    }

    /** Idempotent: a day and date already started gives back the same session, not created. */
    @Transactional
    public StartedSession start(StartSessionRequest request) {
        if (request == null || request.programDayId() == null || request.date() == null) {
            throw ApiException.badRequest("Faltan el entreno o la fecha");
        }
        TrainingRows.ActiveDay activeDay = programDao.findActiveDay(request.programDayId())
                .orElseThrow(TrainingErrors::dayNotFound);
        TrainingDates.requireDayOfWeek(activeDay.day().dayOfWeek(), request.date());
        TrainingDates.weekNumber(activeDay.programStart(), request.date());
        if (request.date().isAfter(TrainingDates.today(clock))) {
            throw TrainingErrors.futureDate();
        }
        return sessionDao.insertIfAbsent(activeDay.day().id(), activeDay.day().name(), request.date(), now())
                .map(created -> {
                    log.info("jcfit: session {} started for day {} on {}", created.id(), created.programDayId(),
                            created.date());
                    return new StartedSession(reply(created), true);
                })
                .orElseGet(() -> new StartedSession(reply(sessionDao.find(activeDay.day().id(), request.date())
                        .orElseThrow(TrainingErrors::sessionNotFound)), false));
    }

    /** Insert or update by session, exercise and set number; a finished session can still be edited. */
    @Transactional
    public LoggedSet saveSet(int sessionId, SaveSetRequest request) {
        SetValidator.validate(request);
        TrainingRows.Session session = sessionDao.findById(sessionId).orElseThrow(TrainingErrors::sessionNotFound);
        if (session.programDayId() == null || !programDao.isInDay(session.programDayId(), request.exerciseId())) {
            throw TrainingErrors.exerciseNotInDay();
        }
        boolean completed = SetValidator.completed(request);
        setLogDao.upsert(sessionId, request, completed, now());
        return new LoggedSet(request.setNumber(), request.weightKg(), request.reps(), completed);
    }

    @Transactional
    public void deleteSet(int sessionId, int exerciseId, int setNumber) {
        sessionDao.findById(sessionId).orElseThrow(TrainingErrors::sessionNotFound);
        setLogDao.delete(sessionId, exerciseId, setNumber);
    }

    /** Idempotent: finishing again keeps the first finish time. */
    public SessionReply finish(int sessionId) {
        TrainingRows.Session session = sessionDao.finish(sessionId, now()).orElseThrow(TrainingErrors::sessionNotFound);
        log.info("jcfit: session {} finished", sessionId);
        return reply(session);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private static SessionReply reply(TrainingRows.Session session) {
        return new SessionReply(session.id(), session.startedAt(), session.finishedAt());
    }
}
