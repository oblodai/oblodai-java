package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * {@code /v1/referral/info} — the merchant's referral code and what it has earned.
 *
 * @param code the merchant's referral code
 * @param link the ready-made referral link carrying that code
 * @param tierBps referral tiers, in basis points, first tier first
 * @param referredCount how many merchants have signed up through the code, all time
 * @param earningsByAsset all-time earnings keyed by asset, each a decimal string at that asset's
 *     own scale
 * @param week the same two figures restricted to the current week
 */
public record ReferralInfo(
        @JsonProperty("code") String code,
        @JsonProperty("link") String link,
        @JsonProperty("tier_bps") List<Integer> tierBps,
        @JsonProperty("referred_count") Integer referredCount,
        @JsonProperty("earnings_by_asset") Map<String, String> earningsByAsset,
        @JsonProperty("week") Week week) {

    /**
     * The current week's slice of the referral figures.
     *
     * @param referredCount how many merchants signed up through the code this week
     * @param earningsByAsset this week's earnings keyed by asset, each a decimal string
     */
    public record Week(
            @JsonProperty("referred_count") Integer referredCount,
            @JsonProperty("earnings_by_asset") Map<String, String> earningsByAsset) {}
}
