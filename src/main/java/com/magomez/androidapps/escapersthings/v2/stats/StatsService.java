package com.magomez.androidapps.escapersthings.v2.stats;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatsService {

    private final StatsDao statsDao;

    public StatsService(StatsDao statsDao) {
        this.statsDao = statsDao;
    }

    /** total and byCategory count the rooms I played; the averages and tops use the grades. */
    public Stats stats(int userId) {
        List<CategoryCount> byCategory = statsDao.playedByCategory(userId);
        List<MyGrade> grades = statsDao.myGrades(userId);
        int total = byCategory.stream().mapToInt(CategoryCount::count).sum();
        return new Stats(total, StatsCalculator.myAverage(grades), StatsCalculator.round(statsDao.groupAverage()),
                byCategory, StatsCalculator.tops(grades));
    }
}
