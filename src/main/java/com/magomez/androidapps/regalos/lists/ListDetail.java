package com.magomez.androidapps.regalos.lists;

import com.magomez.androidapps.regalos.claims.OthersItem;
import com.magomez.androidapps.regalos.people.Person;

import java.util.List;

/** GET /lists/{id}: the list, who is invited and its wishes with who gives each. */
public record ListDetail(ListSummary list, List<Person> members, List<OthersItem> items) {
}
