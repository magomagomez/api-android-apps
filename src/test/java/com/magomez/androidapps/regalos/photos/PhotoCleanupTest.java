package com.magomez.androidapps.regalos.photos;

import com.magomez.androidapps.regalos.claims.ClaimDao;
import com.magomez.androidapps.regalos.claims.ClaimService;
import com.magomez.androidapps.regalos.config.RegalosConfig;
import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;
import com.magomez.androidapps.regalos.ideas.IdeaDao;
import com.magomez.androidapps.regalos.ideas.IdeaService;
import com.magomez.androidapps.regalos.items.ItemDao;
import com.magomez.androidapps.regalos.items.ItemService;
import com.magomez.androidapps.regalos.lists.ListDao;
import com.magomez.androidapps.regalos.lists.ListService;
import com.magomez.androidapps.regalos.people.PeopleService;
import com.magomez.androidapps.regalos.people.RegalosUserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/** A wish, idea or list that really goes takes its photos out of Drive; one kept for its giver (R5) does not. */
class PhotoCleanupTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-31T10:00:00Z"), RegalosConfig.SPAIN);
    private static final String ITEM_FILE = "SELECT photo_file_id FROM regalos_items WHERE id = ?";
    private static final String LIST_FILES = "WHERE list_id = ? AND photo_file_id IS NOT NULL";
    private static final String IDEA_SLOT = "SELECT photo_file_id FROM regalos_ideas WHERE id = ? AND author_user_id = ?";
    private static final String SOFT_DELETE = "UPDATE regalos_items SET status = 'DELETED'";
    private static final String IS_MEMBER = "FROM regalos_list_members WHERE list_id = ? AND user_id = ?";
    private static final String SUMMARIES = "FROM regalos_lists l JOIN regalos_list_members m";
    private static final String ITEM_BY_ID = "WHERE i.id = ?";
    private static final String PURGE = "WHERE id = ? AND status = 'DELETED'";
    private static final int CRIS = 1;
    private static final int JAVI = 2;
    private static final long WISH = 42;
    private static final long LEO = 10;

    private RegalosFakeJdbc jdbc;
    private FakePhotoStore store;
    private PhotoFiles photoFiles;

    @BeforeEach
    void setUp() {
        jdbc = new RegalosFakeJdbc();
        store = new FakePhotoStore();
        photoFiles = new PhotoFiles(new RegalosPhotoDao(jdbc), store);
    }

    @Test
    void aWishThatReallyGoesTakesItsPhotoWithIt() {
        jdbc.rows(ITEM_FILE, row("photo_file_id", "wish-photo-id")).updateCount(SOFT_DELETE, 0);

        new ItemService(new ItemDao(jdbc), photoFiles).delete(CRIS, WISH);

        assertThat(store.deleted).containsExactly("wish-photo-id");
    }

    @Test
    void aClaimedWishKeepsItsPhotoForItsGiver() {
        jdbc.rows(ITEM_FILE, row("photo_file_id", "wish-photo-id")).updateCount(SOFT_DELETE, 1);

        new ItemService(new ItemDao(jdbc), photoFiles).delete(CRIS, WISH);

        assertThat(store.deleted).isEmpty();
    }

    @Test
    void anIdeaTakesItsPhotoWithIt() {
        jdbc.rows(IDEA_SLOT, row("photo_file_id", "idea-photo-id"));

        new IdeaService(new IdeaDao(jdbc), people(), photoFiles).delete(CRIS, 9);

        assertThat(jdbc.callsWith(IDEA_SLOT).get(0).args()).containsExactly(9L, CRIS);
        assertThat(store.deleted).containsExactly("idea-photo-id");
    }

    @Test
    void aDeletedListTakesThePhotosOfAllItsWishes() {
        jdbc.scalar(IS_MEMBER, 1)
                .rows(SUMMARIES, listOfCris())
                .rows(LIST_FILES, row("photo_file_id", "photo-a"), row("photo_file_id", "photo-b"));

        listService().delete(CRIS, LEO);

        assertThat(store.deleted).containsExactly("photo-a", "photo-b");
    }

    @Test
    void aListWishThatReallyGoesTakesItsPhotoAndAClaimedOneKeepsIt() {
        jdbc.scalar(IS_MEMBER, 1).rows(SUMMARIES, listOfCris())
                .rows(ITEM_FILE, row("photo_file_id", "wish-photo-id")).updateCount(SOFT_DELETE, 1);
        listService().deleteItem(CRIS, LEO, WISH);
        assertThat(store.deleted).isEmpty();

        jdbc.updateCount(SOFT_DELETE, 0);
        listService().deleteItem(CRIS, LEO, WISH);
        assertThat(store.deleted).containsExactly("wish-photo-id");
    }

    @Test
    void releasingAWishItsOwnerAlreadyDeletedTakesItsPhoto() {
        jdbc.rows(ITEM_BY_ID, deletedWishClaimedByJavi())
                .rows(ITEM_FILE, row("photo_file_id", "wish-photo-id"))
                .updateCount(PURGE, 1);

        claimService().unclaim(JAVI, WISH);

        assertThat(store.deleted).containsExactly("wish-photo-id");
    }

    private PeopleService people() {
        return new PeopleService(new RegalosUserDao(jdbc), CLOCK);
    }

    private ClaimService claimService() {
        return new ClaimService(new ClaimDao(jdbc), people(), new ListDao(jdbc), new IdeaDao(jdbc), photoFiles);
    }

    private ListService listService() {
        return new ListService(new ListDao(jdbc), claimService(), people(), photoFiles);
    }

    private static Map<String, Object> listOfCris() {
        return row("id", LEO, "name", "Leo", "birthday_day", 9, "birthday_month", 11, "created_by_user_id", CRIS,
                "created_by_name", "Cris", "member_count", 2L, "wish_count", 1L, "free_count", 1L, "mine_count", 0L);
    }

    private static Map<String, Object> deletedWishClaimedByJavi() {
        Map<String, Object> wish = new HashMap<>(row("id", WISH, "owner_user_id", CRIS, "owner_name", "Cris",
                "owner_birthday_day", null, "owner_birthday_month", null, "list_id", null, "list_name", null,
                "list_birthday_day", null, "list_birthday_month", null, "added_by_name", null));
        wish.putAll(row("title", "Jarrón", "url", null, "store", null, "price_cents", 3500, "image_url", null,
                "photo_updated_at", null, "notes", null, "wanted_a_lot", false, "status", "DELETED",
                "claim_state", "RESERVED", "marked_by_user_id", JAVI, "giver_user_id", JAVI, "giver_name", "Javi",
                "item_edited_at", null));
        return wish;
    }
}
