package com.magomez.androidapps.jctravels.outlets.converter;

import com.magomez.androidapps.jctravels.outlets.dto.UpdateOutletRequest;
import com.magomez.androidapps.jctravels.outlets.dto.CreateOutlet;
import com.magomez.androidapps.jctravels.outlets.dto.CreateOutletRequest;
import com.magomez.androidapps.jctravels.outlets.dto.Outlet;
import com.magomez.androidapps.jctravels.outlets.dto.OutletDTO;
import com.magomez.androidapps.jctravels.outlets.dto.OutletFilter;
import com.magomez.androidapps.jctravels.outlets.dto.OutletFilterRequest;
import com.magomez.androidapps.jctravels.outlets.dto.UpdateOutlet;

import java.util.List;

public class OutletConverter {

    private OutletConverter(){}

    public static List<OutletDTO> toDtoList(List<Outlet> outlets){
        return outlets.stream()
                .map(OutletConverter::toDto)
                .toList();
    }

    public static OutletDTO toDto(Outlet outlet) {
        return new OutletDTO(
                outlet.id(),
                outlet.name(),
                outlet.iconPath(),
                outlet.city()
        );
    }

    public static CreateOutlet toRecord(CreateOutletRequest request) {
        return new CreateOutlet(
                request.name(),
                request.city()
        );
    }

    public static UpdateOutlet toRecord(UpdateOutletRequest request) {
        return new UpdateOutlet(
                request.name(),
                request.city()
        );
    }

    public static OutletFilter toFilter(OutletFilterRequest request){
        return new OutletFilter(
                request.city()
        );
    }
}
