package com.magomez.androidapps.regalos.lists;

import com.magomez.androidapps.regalos.people.Birthday;

import java.util.List;

/** Body of POST and PUT /lists. Whoever creates the list is always a member, listed or not. */
public record ListRequest(String name, Birthday birthday, List<Integer> memberIds) {
}
