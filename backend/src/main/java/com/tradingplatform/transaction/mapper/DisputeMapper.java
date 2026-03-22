package com.tradingplatform.transaction.mapper;

import com.tradingplatform.transaction.dto.DisputeResponse;
import com.tradingplatform.transaction.entity.Dispute;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for Dispute entity to DTO conversion.
 */
@Mapper(componentModel = "spring")
public interface DisputeMapper {

    /**
     * Converts a Dispute entity to DisputeResponse DTO.
     * openerName is set separately in the service.
     *
     * @param dispute the dispute entity
     * @return the response DTO
     */
    @Mapping(target = "openerName", ignore = true)
    DisputeResponse toResponse(Dispute dispute);
}