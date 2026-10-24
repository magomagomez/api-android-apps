package com.magomez.androidapps.regalos.claims;

import com.magomez.androidapps.regalos.people.Person;

import java.util.List;

/** GET /people/{userId}/items: their ACTIVE wishes with the claim of each. */
public record PersonItems(Person person, List<OthersItem> items) {
}
