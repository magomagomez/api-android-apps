package com.magomez.androidapps.escapersthings.userstats.service;

import com.magomez.androidapps.escapersthings.escaperooms.dao.EscapeRoomsDao;
import com.magomez.androidapps.escapersthings.escaperooms.dto.EscapeRoom;
import com.magomez.androidapps.escapersthings.escaperooms.dto.UsersEnum;
import com.magomez.androidapps.escapersthings.userstats.converter.UserStatsConverter;
import com.magomez.androidapps.escapersthings.userstats.dto.UserGradesDTO;
import com.magomez.androidapps.escapersthings.mail.MailService;
import com.magomez.androidapps.escapersthings.users.dao.UsersDao;
import com.magomez.androidapps.escapersthings.users.dto.UserLogin;
import com.magomez.androidapps.escapersthings.userstats.dao.UserStatsDao;
import com.magomez.androidapps.escapersthings.userstats.dto.Stats;
import com.magomez.androidapps.escapersthings.userstats.dto.StatsGrades;
import com.magomez.androidapps.escapersthings.userstats.dto.UserStatsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserStatsService {

    private static final String HORROR_SORT = "H";
    private static final String INMERSION_SORT = "I";
    private static final String GAME_MASTER_SORT = "G";
    private static final String TOP_SORT = "A";
    private static final String WORST_SORT = "W";

    private final UserStatsDao userStatsDao;
    private final EscapeRoomsDao escapeRoomsDao;
    private final UsersDao usersDao;
    private final MailService mailService;

    @Autowired
    public UserStatsService(final UserStatsDao userStatsDao, final EscapeRoomsDao escapeRoomsDao,
                            final UsersDao usersDao, MailService mailService){

        this.userStatsDao = userStatsDao;
        this.escapeRoomsDao = escapeRoomsDao;
        this.usersDao = usersDao;
        this.mailService = mailService;
    }

    public List<UserStatsDTO> getStats(Integer userId){
        List<Stats> userStats = userStatsDao.getStats(userId);
        return UserStatsConverter.toDtoList(userStats);
    }

    public UserGradesDTO getUserGrades(Integer userId, String order){
        List<StatsGrades> all = userStatsDao.getStatsGrades(userId, order);
        List<StatsGrades> horror = userStatsDao.getStatsGrades(userId, HORROR_SORT);
        List<StatsGrades> inmersion = userStatsDao.getStatsGrades(userId, INMERSION_SORT);
        List<StatsGrades> gm = userStatsDao.getStatsGrades(userId, GAME_MASTER_SORT);
        List<StatsGrades> top = userStatsDao.getStatsGrades(userId, TOP_SORT);
        List<StatsGrades> worst = userStatsDao.getStatsGrades(userId, WORST_SORT);

        return new UserGradesDTO(
                UserStatsConverter.gradesToDto(all),
                UserStatsConverter.gradesToDto(horror),
                UserStatsConverter.gradesToDto(top),
                UserStatsConverter.gradesToDto(inmersion),
                UserStatsConverter.gradesToDto(gm),
                UserStatsConverter.gradesToDto(worst)
        );
    }

    public void resendPendingEscapeRooms(Integer userId){
        List<EscapeRoom> escapeRooms = escapeRoomsDao.getPendingEscapeRooms(userId);
        UserLogin user = usersDao.getUserById(userId);
        for (EscapeRoom escape : escapeRooms){
            sendMail( UsersEnum.DEFAULT.getValue(user.nombre()), escape);
        }
    }

    private void sendMail(UsersEnum user, EscapeRoom escapeRoom){
        mailService.sendMail(user.getMail(),escapeRoom.name(),escapeRoom.id(),
                escapeRoom.imageUrl(), user.getUserName());
    }

}
