package com.magomez.androidapps.regalos.photos;

import com.magomez.androidapps.regalos.config.RegalosFakeJdbc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory photo store for the regalos tests: records uploads and deletes, and can fail or be unset. */
public class FakePhotoStore implements PhotoStore {

    public final Map<String, byte[]> files = new LinkedHashMap<>();
    public final List<String> uploadedNames = new ArrayList<>();
    public final List<String> deleted = new ArrayList<>();
    public boolean configured = true;
    public boolean failing;
    private int next = 1;

    /** PhotoFiles over this store, for services that clean photos up after a delete. */
    public static PhotoFiles photoFiles(RegalosFakeJdbc jdbc) {
        return new PhotoFiles(new RegalosPhotoDao(jdbc), new FakePhotoStore());
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public String upload(String name, byte[] jpeg) {
        failIfAsked();
        String id = "drive-file-" + next++;
        files.put(id, jpeg);
        uploadedNames.add(name);
        return id;
    }

    @Override
    public Optional<byte[]> download(String fileId) {
        failIfAsked();
        return Optional.ofNullable(files.get(fileId));
    }

    @Override
    public void delete(String fileId) {
        failIfAsked();
        files.remove(fileId);
        deleted.add(fileId);
    }

    private void failIfAsked() {
        if (failing) {
            throw new PhotoStoreException("Drive is down in this test");
        }
    }
}
