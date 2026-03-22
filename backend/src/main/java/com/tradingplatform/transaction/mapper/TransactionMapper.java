package com.tradingplatform.transaction.mapper;

import com.tradingplatform.transaction.dto.TransactionDetailResponse;
import com.tradingplatform.transaction.dto.TransactionResponse;
import com.tradingplatform.transaction.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Transaction entities to DTOs.
 */
@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "listingTitle", ignore = true)
    @Mapping(target = "listingImageUrl", ignore = true)
    @Mapping(target = "buyerName", ignore = true)
    @Mapping(target = "sellerName", ignore = true)
    @Mapping(target = "userRole", ignore = true)
    TransactionResponse toResponse(Transaction transaction);

    @Mapping(target = "listingTitle", ignore = true)
    @Mapping(target = "listingDescription", ignore = true)
    @Mapping(target = "listingImageUrl", ignore = true)
    @Mapping(target = "buyerName", ignore = true)
    @Mapping(target = "buyerAvatarUrl", ignore = true)
    @Mapping(target = "sellerName", ignore = true)
    @Mapping(target = "sellerAvatarUrl", ignore = true)
    @Mapping(target = "userRole", ignore = true)
    @Mapping(target = "canCancel", ignore = true)
    @Mapping(target = "canConfirmPayment", ignore = true)
    @Mapping(target = "canConfirmFunds", ignore = true)
    @Mapping(target = "canMarkDelivered", ignore = true)
    @Mapping(target = "canConfirmReceipt", ignore = true)
    @Mapping(target = "canRate", ignore = true)
    @Mapping(target = "canDispute", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    TransactionDetailResponse toDetailResponse(Transaction transaction);
}