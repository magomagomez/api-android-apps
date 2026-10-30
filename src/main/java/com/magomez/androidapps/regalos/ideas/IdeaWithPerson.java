package com.magomez.androidapps.regalos.ideas;

import com.magomez.androidapps.regalos.people.Birthday;

/** An idea with the name and birthday of whom it is for, as my gifts groups them. */
public record IdeaWithPerson(Idea idea, String forName, Birthday forBirthday) {
}
