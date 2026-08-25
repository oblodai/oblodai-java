package com.oblodai.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * What {@code /v1/payment/resolve} did with an underpayment, discriminated by the
 * {@code resolution} field: {@code accepted} keeps what landed as full settlement,
 * {@code refunded} sends it back.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "resolution",
        visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ResolutionAccepted.class, name = "accepted"),
    @JsonSubTypes.Type(value = ResolutionRefunded.class, name = "refunded")
})
public sealed interface Resolution permits ResolutionAccepted, ResolutionRefunded {

    /** The discriminator: {@code accepted} or {@code refunded}. */
    String resolution();
}
