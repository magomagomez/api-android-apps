package com.magomez.androidapps.regalos.claims;

import com.magomez.androidapps.regalos.ideas.Idea;
import com.magomez.androidapps.regalos.people.Person;

import java.util.List;

/**
 * My gifts for one person (person set, with my ideas for them) or for one shared list (list
 * set, no ideas).
 */
public record ClaimGroup(Person person, SharedListRef list, List<MyClaim> claims, List<Idea> ideas) {
}
