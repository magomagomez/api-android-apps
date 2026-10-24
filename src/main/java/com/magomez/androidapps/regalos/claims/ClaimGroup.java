package com.magomez.androidapps.regalos.claims;

import com.magomez.androidapps.regalos.people.Person;

import java.util.List;

/** My gifts for one person. */
public record ClaimGroup(Person person, List<MyClaim> claims) {
}
