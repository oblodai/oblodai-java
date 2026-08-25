package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code /v1/transfer/to-personal}: business balance to the owner's personal balance.
 *
 * @param uuid the transfer's identifier
 * @param currency the asset moved
 * @param amount the amount moved, a decimal string at the asset's own scale
 * @param direction always {@code to_personal} for this transfer
 * @param personalBalance the personal balance after the transfer, a decimal string
 * @param documentUrl signed link to the transfer's PDF document
 */
public record TransferToPersonal(
        @JsonProperty("uuid") String uuid,
        @JsonProperty("currency") String currency,
        @JsonProperty("amount") String amount,
        @JsonProperty("direction") String direction,
        @JsonProperty("personal_balance") String personalBalance,
        @JsonProperty("document_url") String documentUrl) {}
