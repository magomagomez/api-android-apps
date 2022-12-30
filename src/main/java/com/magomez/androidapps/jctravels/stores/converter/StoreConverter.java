package com.magomez.androidapps.jctravels.stores.converter;

import com.magomez.androidapps.jctravels.stores.dto.CreateStore;
import com.magomez.androidapps.jctravels.stores.dto.CreateStoreRequest;
import com.magomez.androidapps.jctravels.stores.dto.Store;
import com.magomez.androidapps.jctravels.stores.dto.StoreDTO;
import com.magomez.androidapps.jctravels.stores.dto.StoreFilter;
import com.magomez.androidapps.jctravels.stores.dto.StoreFilterRequest;
import com.magomez.androidapps.jctravels.stores.dto.UpdateStore;
import com.magomez.androidapps.jctravels.stores.dto.UpdateStoreRequest;

import java.util.List;

public class StoreConverter {

    private StoreConverter(){}

    public static List<StoreDTO> toDtoList(List<Store> stores){
        return stores.stream()
                .map(StoreConverter::toDto)
                .toList();
    }

    public static StoreDTO toDto(Store store) {
         return new StoreDTO(
                store.id(),
                store.name(),
                store.imagePath(),
                store.done(),
                store.outlet()
        );
    }

    public static CreateStore toRecord(CreateStoreRequest request) {
        return new CreateStore(
                request.name(),
                request.outlet()
        );
    }

    public static UpdateStore toRecord(UpdateStoreRequest request) {
        return new UpdateStore(
                request.name(),
                request.done()
        );
    }

    public static StoreFilter toFilter(StoreFilterRequest request) {
        return new StoreFilter(
                request.outlet()
        );
    }
}
