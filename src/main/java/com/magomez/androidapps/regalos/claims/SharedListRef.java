package com.magomez.androidapps.regalos.claims;

import com.magomez.androidapps.regalos.people.Birthday;

/** The shared list a group of my gifts belongs to. */
public record SharedListRef(long id, String name, Birthday birthday, Integer daysUntilBirthday) {
}
