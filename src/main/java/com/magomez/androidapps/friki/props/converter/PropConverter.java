package com.magomez.androidapps.friki.props.converter;

import com.magomez.androidapps.friki.props.dto.Prop;
import com.magomez.androidapps.friki.props.dto.PropDTO;

import java.util.List;

public class PropConverter {

    private PropConverter(){}

    public static List<PropDTO> toDtoList(List<Prop> monuments){
        return monuments.stream()
                .map(PropConverter::toDto)
                .toList();
    }

    public static PropDTO toDto(Prop prop) {
        return new PropDTO(
                prop.id(),
                prop.name(),
                prop.isWishList(),
                prop.categories()
        );
    }

}
