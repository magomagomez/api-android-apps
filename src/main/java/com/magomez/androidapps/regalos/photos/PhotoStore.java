package com.magomez.androidapps.regalos.photos;

import java.util.Optional;

/** Where the photos of wishes and ideas live (Google Drive in production, a fake in tests). */
public interface PhotoStore {

    /** False while its credentials are missing: photo routes then answer 503 and the rest works. */
    boolean isConfigured();

    /** Stores a JPEG under a name and returns its file id; throws {@link PhotoStoreException}. */
    String upload(String name, byte[] jpeg);

    /** The bytes of a stored photo, empty if it is gone; throws {@link PhotoStoreException}. */
    Optional<byte[]> download(String fileId);

    /** Removes a photo; a photo already gone counts as removed. Throws {@link PhotoStoreException}. */
    void delete(String fileId);
}
