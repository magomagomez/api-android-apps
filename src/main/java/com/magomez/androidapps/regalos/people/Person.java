package com.magomez.androidapps.regalos.people;

/** Another user as list headers show them; daysUntilBirthday is null without a birthday, 0 on the day. */
public record Person(int id, String displayName, Birthday birthday, Integer daysUntilBirthday) {
}
