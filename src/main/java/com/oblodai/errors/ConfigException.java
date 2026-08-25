package com.oblodai.errors;

/**
 * Raised before any request is sent: bad options, missing credentials, an unusable argument. Nothing
 * reached the network, so nothing happened on the gateway.
 */
public class ConfigException extends OblodaiException {

    private static final long serialVersionUID = 1L;

    /** A route needs a key pair the client was not given. */
    public static final String MISSING_CREDENTIALS = "sdk.missing_credentials";

    /** The client options do not make sense (bad base URL, half a key pair). */
    public static final String BAD_CONFIG = "sdk.bad_config";

    /** The caller's idempotency key is not a header-safe value. */
    public static final String BAD_IDEMPOTENCY_KEY = "sdk.bad_idempotency_key";

    /** The route does not deduplicate by {@code Idempotency-Key}, so a key must not be sent. */
    public static final String IDEMPOTENCY_UNSUPPORTED = "sdk.idempotency_unsupported";

    /** A path parameter would have rewritten the URL. */
    public static final String BAD_PATH_PARAM = "sdk.bad_path_param";

    /**
     * @param code one of the {@code sdk.*} constants on this class
     * @param message what is wrong and how to fix it
     * @param field the option or argument at fault, when there is one
     */
    public ConfigException(String code, String message, String field) {
        super(code, message, 0, false, null, null, field, false, null, null);
    }
}
