package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/transfer/to-user}: business balance to another user's personal balance.
 *
 * @param uuid the transfer's identifier
 * @param currency the asset moved
 * @param amount the amount moved, a decimal string at the asset's own scale
 * @param toUserId the user whose personal balance was credited
 * @param documentUrl signed link to the transfer's PDF document
 */
public record TransferToUser(
        @JsonProperty("uuid") String uuid,
        @JsonProperty("currency") String currency,
        @JsonProperty("amount") String amount,
        @JsonProperty("to_user_id") String toUserId,
        @JsonProperty("document_url") String documentUrl) {}
