package com.magomez.androidapps.regalos.photos;

import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;
import com.magomez.androidapps.regalos.lists.ListDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static com.magomez.androidapps.regalos.config.RegalosTestRows.assertRegalosError;
import static com.magomez.androidapps.regalos.config.RegalosTestRows.row;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who may set a photo (R11) and who may see it (R10): to anyone else the wish or idea does not
 * exist, and nothing reaches Drive for them.
 */
class RegalosPhotoServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");
    private static final int CRIS = 1;
    private static final int JAVI = 2;
    private static final int ALBA = 3;
    private static final long WISH = 42;
    private static final long LIST = 7;
    private static final long IDEA = 9;

    private static final String OWN = "SELECT photo_file_id FROM regalos_items WHERE id = ? AND owner_user_id = ?";
    private static final String LIST_ITEM = "SELECT photo_file_id FROM regalos_items WHERE id = ? AND list_id = ?";
    private static final String IDEA_SLOT = "SELECT photo_file_id FROM regalos_ideas WHERE id = ? AND author_user_id = ?";
    private static final String SET_OWN = "UPDATE regalos_items SET photo_file_id = ?, photo_updated_at = ? WHERE id = ? AND owner_user_id = ?";
    private static final String SET_LIST = "UPDATE regalos_items SET photo_file_id = ?, photo_updated_at = ? WHERE id = ? AND list_id = ?";
    private static final String SET_IDEA = "UPDATE regalos_ideas SET photo_file_id";
    private static final String EDITED = "UPDATE regalos_claims SET item_edited_at";
    private static final String VIEW = "LEFT JOIN regalos_claims c ON c.item_id = i.id WHERE i.id = ?";
    private static final String IS_MEMBER = "FROM regalos_list_members WHERE list_id = ? AND user_id = ?";

    private RegalosFakeJdbc jdbc;
    private FakePhotoStore store;
    private RegalosPhotoService service;

    @BeforeEach
    void setUp() {
        jdbc = new RegalosFakeJdbc();
        store = new FakePhotoStore();
        RegalosPhotoDao dao = new RegalosPhotoDao(jdbc);
        service = new RegalosPhotoService(dao, store, new PhotoFiles(dao, store), new ListDao(jdbc),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void theOwnerSetsThePhotoOfTheirWishAndTheOldOneGoes() throws IOException {
        store.files.put("old-photo-id", new byte[]{1});
        jdbc.rows(OWN, row("photo_file_id", "old-photo-id"));

        PhotoRef ref = service.setOwnItemPhoto(CRIS, WISH, PhotoImagesTest.jpeg(200, 100));

        assertThat(ref.photoVersion()).isEqualTo(NOW);
        assertThat(jdbc.callsWith(OWN).get(0).args()).containsExactly(WISH, CRIS);
        assertThat(jdbc.callsWith(SET_OWN).get(0).args())
                .containsExactly("drive-file-1", OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC), WISH, CRIS);
        assertThat(store.uploadedNames).singleElement().asString().matches("regalos-item-42-[0-9a-f]{4}\\.jpg");
        assertThat(store.deleted).containsExactly("old-photo-id");
        assertThat(jdbc.callsWith(EDITED).get(0).args()).containsExactly(WISH);
    }

    @Test
    void nobodyElseCanSetThePhotoOfMyWish() throws IOException {
        // The owner_user_id filter finds nothing for Javi.
        assertRegalosError(() -> service.setOwnItemPhoto(JAVI, WISH, PhotoImagesTest.jpeg(20, 20)),
                HttpStatus.NOT_FOUND, "not_found");

        assertThat(jdbc.callsWith(OWN).get(0).args()).containsExactly(WISH, JAVI);
        assertThat(store.uploadedNames).isEmpty();
        assertThat(jdbc.callsWith(SET_OWN)).isEmpty();
    }

    @Test
    void aWishDeletedWhileUploadingDropsTheNewPhoto() throws IOException {
        jdbc.rows(OWN, row("photo_file_id", null)).updateCount(SET_OWN, 0);

        assertRegalosError(() -> service.setOwnItemPhoto(CRIS, WISH, PhotoImagesTest.jpeg(20, 20)),
                HttpStatus.NOT_FOUND, "not_found");

        assertThat(store.deleted).containsExactly("drive-file-1");
        assertThat(store.files).isEmpty();
    }

    @Test
    void whatIsNotAPhotoNeverReachesDrive() {
        jdbc.rows(OWN, row("photo_file_id", null));

        assertRegalosError(() -> service.setOwnItemPhoto(CRIS, WISH, new byte[]{1, 2, 3}),
                HttpStatus.BAD_REQUEST, "bad_request");

        assertThat(store.uploadedNames).isEmpty();
    }

    @Test
    void withoutDriveConfiguredPhotosAnswer503AndTouchNothing() {
        store.configured = false;

        assertRegalosError(() -> service.setOwnItemPhoto(CRIS, WISH, new byte[]{1}),
                HttpStatus.SERVICE_UNAVAILABLE, "photos_unavailable");
        assertRegalosError(() -> service.itemPhoto(CRIS, WISH), HttpStatus.SERVICE_UNAVAILABLE, "photos_unavailable");
        assertThat(jdbc.calls).isEmpty();
    }

    @Test
    void anyMemberOfAListSetsThePhotoOfItsWishesAndOutsidersFindNoList() throws IOException {
        jdbc.scalar(IS_MEMBER, 1).rows(LIST_ITEM, row("photo_file_id", null));

        service.setListItemPhoto(ALBA, LIST, WISH, PhotoImagesTest.jpeg(20, 20));

        assertThat(jdbc.callsWith(SET_LIST).get(0).args()).containsExactly("drive-file-1",
                OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC), WISH, LIST);

        jdbc.scalar(IS_MEMBER, 0);
        assertRegalosError(() -> service.setListItemPhoto(JAVI, LIST, WISH, PhotoImagesTest.jpeg(20, 20)),
                HttpStatus.NOT_FOUND, "not_found");
        assertThat(store.uploadedNames).hasSize(1);
    }

    @Test
    void theAuthorSetsThePhotoOfAnIdeaAndNobodyElseFindsIt() throws IOException {
        jdbc.rows(IDEA_SLOT, row("photo_file_id", null));

        service.setIdeaPhoto(CRIS, IDEA, PhotoImagesTest.jpeg(20, 20));

        assertThat(jdbc.callsWith(IDEA_SLOT).get(0).args()).containsExactly(IDEA, CRIS);
        assertThat(jdbc.callsWith(SET_IDEA).get(0).args()).endsWith(IDEA, CRIS);
        assertThat(store.uploadedNames).singleElement().asString().startsWith("regalos-idea-9-");
        assertThat(jdbc.callsWith(EDITED)).isEmpty();
    }

    @Test
    void someoneElsesIdeaPhotoDoesNotExist() {
        // The author filter finds nothing for Javi, whose idea it is not (R7).
        assertRegalosError(() -> service.ideaPhoto(JAVI, IDEA), HttpStatus.NOT_FOUND, "not_found");

        assertThat(jdbc.callsWith(IDEA_SLOT).get(0).args()).containsExactly(IDEA, JAVI);
    }

    @Test
    void myIdeaPhotoComesFromDrive() {
        store.files.put("idea-photo-id", new byte[]{7});
        jdbc.rows(IDEA_SLOT, row("photo_file_id", "idea-photo-id"));

        assertThat(service.ideaPhoto(CRIS, IDEA)).containsExactly(7);
    }

    @Test
    void theOwnerAndEveryoneElseSeeThePhotoOfAnActiveWishOfALetter() {
        store.files.put("wish-photo-id", new byte[]{5});
        jdbc.rows(VIEW, letterWish(CRIS, "ACTIVE", null, null));

        assertThat(service.itemPhoto(CRIS, WISH)).containsExactly(5);
        assertThat(service.itemPhoto(JAVI, WISH)).containsExactly(5);
    }

    @Test
    void aReceivedWishShowsItsPhotoOnlyToItsOwnerAndItsGiver() {
        store.files.put("wish-photo-id", new byte[]{5});
        jdbc.rows(VIEW, letterWish(CRIS, "RECEIVED", JAVI, JAVI));

        assertThat(service.itemPhoto(CRIS, WISH)).containsExactly(5);
        assertThat(service.itemPhoto(JAVI, WISH)).containsExactly(5);
        assertRegalosError(() -> service.itemPhoto(ALBA, WISH), HttpStatus.NOT_FOUND, "not_found");
    }

    @Test
    void theOwnerNoLongerSeesAWishTheyDeleted() {
        jdbc.rows(VIEW, letterWish(CRIS, "DELETED", JAVI, JAVI));

        assertRegalosError(() -> service.itemPhoto(CRIS, WISH), HttpStatus.NOT_FOUND, "not_found");
    }

    @Test
    void onlyMembersSeeThePhotoOfAListWish() {
        store.files.put("wish-photo-id", new byte[]{5});
        jdbc.rows(VIEW, row("owner_user_id", null, "list_id", LIST, "status", "ACTIVE",
                "photo_file_id", "wish-photo-id", "marked_by_user_id", null, "giver_user_id", null));

        jdbc.scalar(IS_MEMBER, 0);
        assertRegalosError(() -> service.itemPhoto(JAVI, WISH), HttpStatus.NOT_FOUND, "not_found");

        jdbc.scalar(IS_MEMBER, 1);
        assertThat(service.itemPhoto(ALBA, WISH)).containsExactly(5);
    }

    @Test
    void aWishWithoutPhotoOrWhosePhotoIsGoneAnswers404() {
        jdbc.rows(VIEW, row("owner_user_id", CRIS, "list_id", null, "status", "ACTIVE",
                "photo_file_id", null, "marked_by_user_id", null, "giver_user_id", null));
        assertRegalosError(() -> service.itemPhoto(JAVI, WISH), HttpStatus.NOT_FOUND, "not_found");

        jdbc.rows(VIEW, letterWish(CRIS, "ACTIVE", null, null));
        assertRegalosError(() -> service.itemPhoto(JAVI, WISH), HttpStatus.NOT_FOUND, "not_found");
    }

    @Test
    void removingAPhotoClearsTheRowAndDeletesTheFile() {
        jdbc.rows(OWN, row("photo_file_id", "old-photo-id"));

        service.deleteOwnItemPhoto(CRIS, WISH);

        assertThat(jdbc.callsWith(SET_OWN).get(0).args()).containsExactly(null, null, WISH, CRIS);
        assertThat(store.deleted).containsExactly("old-photo-id");
        assertThat(jdbc.callsWith(EDITED)).hasSize(1);
    }

    @Test
    void removingAPhotoThatIsNotThereChangesNothing() {
        jdbc.rows(OWN, row("photo_file_id", null));

        service.deleteOwnItemPhoto(CRIS, WISH);

        assertThat(jdbc.callsWith(SET_OWN)).isEmpty();
        assertThat(jdbc.callsWith(EDITED)).isEmpty();
    }

    @Test
    void anOldPhotoDriveWillNotDeleteDoesNotUndoTheNewOne() throws IOException {
        jdbc.rows(OWN, row("photo_file_id", "old-photo-id"));
        byte[] photo = PhotoImagesTest.jpeg(20, 20);
        PhotoFiles files = new PhotoFiles(new RegalosPhotoDao(jdbc), new FakePhotoStore() {
            @Override
            public void delete(String fileId) {
                throw new PhotoStoreException("Drive is down in this test");
            }
        });
        RegalosPhotoService withFailingDeletes = new RegalosPhotoService(new RegalosPhotoDao(jdbc), store, files, new ListDao(jdbc),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(withFailingDeletes.setOwnItemPhoto(CRIS, WISH, photo).photoVersion()).isEqualTo(NOW);
        assertThat(jdbc.callsWith(SET_OWN)).hasSize(1);
    }

    private static Map<String, Object> letterWish(int ownerId, String status, Integer markedBy, Integer giver) {
        return row("owner_user_id", ownerId, "list_id", null, "status", status, "photo_file_id", "wish-photo-id",
                "marked_by_user_id", markedBy, "giver_user_id", giver);
    }
}
