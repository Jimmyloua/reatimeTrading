package com.tradingplatform.transaction.mapper;

import com.tradingplatform.transaction.dto.RatingResponse;
import com.tradingplatform.transaction.entity.Rating;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RatingMapper {

    @Mapping(target = "raterName", ignore = true)
    RatingResponse toResponse(Rating rating);
}