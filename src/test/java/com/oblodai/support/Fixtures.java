package com.oblodai.support;

/** Result objects the gateway answers with, complete enough for the generated models to parse. */
public final class Fixtures {

    private Fixtures() {}

    /**
     * @param uuid the invoice id
     * @return a {@code PaymentView} JSON object
     */
    public static String payment(String uuid) {
        return payment(uuid, "created", "25");
    }

    /**
     * @param uuid the invoice id
     * @param status its status
     * @param amount its amount, as the API writes it
     * @return a {@code PaymentView} JSON object
     */
    public static String payment(String uuid, String status, String amount) {
        return "{\"uuid\":\"" + uuid + "\",\"order_id\":\"o-" + uuid + "\",\"status\":\"" + status + "\","
                + "\"amount\":\"" + amount + "\",\"currency\":\"USDT\",\"merchant_amount\":\"24.5\","
                + "\"payer_amount\":\"" + amount + "\",\"payer_currency\":\"USDT\",\"network\":\"tron\","
                + "\"address\":\"TXYZ\",\"address_muxed\":\"\",\"address_qr_code\":\"\","
                + "\"address_xaddress\":\"\",\"memo\":\"\",\"destination_tag\":\"\","
                + "\"amount_paid\":\"0\",\"amount_remaining\":\"" + amount + "\",\"commission\":\"0.5\","
                + "\"fee_percent\":\"2\",\"exchange_rate\":\"1\",\"method_adjustment\":\"0\","
                + "\"method_adjustment_bps\":0,\"network_surcharge\":\"0\",\"network_surcharge_bps\":0,"
                + "\"confirmations\":0,\"required_confirmations\":20,\"is_final\":false,\"is_multi\":false,"
                + "\"is_test\":true,\"additional_data\":\"\",\"document_url\":\"\",\"payer_address\":\"\","
                + "\"payer_address_is_refundable\":false,\"payer_email\":\"\",\"tx_list\":[],\"txid\":\"\","
                + "\"url\":\"https://pay.example/" + uuid + "\",\"url_return\":\"\",\"url_success\":\"\","
                + "\"created_at\":\"2026-09-24T10:00:00Z\",\"updated_at\":\"2026-09-24T10:00:00Z\","
                + "\"expired_at\":\"2026-09-24T11:00:00Z\",\"rate_expires_at\":\"2026-09-24T10:15:00Z\","
                + "\"paid_at\":null}";
    }

    /**
     * @param uuid the payout id
     * @return a {@code PayoutView} JSON object
     */
    public static String payout(String uuid) {
        return "{\"uuid\":\"" + uuid + "\",\"status\":\"pending\",\"source\":\"api\",\"amount\":\"10\","
                + "\"commission\":\"1\",\"payer_amount\":\"11\",\"currency\":\"USDT\",\"network\":\"tron\","
                + "\"address\":\"T\",\"memo\":\"\",\"txid\":\"\",\"document_url\":\"\",\"is_final\":false,"
                + "\"is_refund\":false,\"approval_required\":false,\"fee_bearer\":\"merchant\",\"created_at\":\"2026-09-24T10:00:00Z\","
                + "\"updated_at\":\"2026-09-24T10:00:00Z\"}";
    }

    /**
     * @param items a JSON array of items
     * @param offset the page's offset
     * @param more whether more pages follow
     * @return an {@code {items, paginate}} result
     */
    public static String page(String items, long offset, boolean more) {
        return "{\"items\":" + items + ",\"paginate\":{\"total\":100,\"per_page\":2,\"offset\":" + offset
                + ",\"has_pages\":" + more + "}}";
    }

    /**
     * @param uuids invoice ids
     * @return a JSON array of {@link #payment(String)} objects
     */
    public static String payments(String... uuids) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < uuids.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(payment(uuids[i]));
        }
        return out.append(']').toString();
    }

    /** @return a {@code BatchSubmitResponse} */
    public static String batchSubmitted() {
        return "{\"batch_id\":\"b1\",\"count\":2,\"kind\":\"payout\",\"status\":\"pending\"}";
    }

    /**
     * @param status the batch status
     * @return a {@code BatchInfoResponse}
     */
    public static String batchInfo(String status) {
        return "{\"batch_id\":\"b1\",\"kind\":\"payout\",\"status\":\"" + status + "\",\"total\":2,"
                + "\"succeeded\":2,\"failed\":0,\"items\":[],\"on_error\":\"continue\",\"created_at\":\"2026-09-24T10:00:00Z\","
                + "\"updated_at\":\"2026-09-24T10:00:00Z\"}";
    }

    /** @return a {@code DocumentJobAccepted} */
    public static String documentAccepted() {
        return "{\"job_id\":\"j1\",\"kind\":\"statement\",\"status\":\"queued\",\"format\":\"pdf\","
                + "\"lang\":\"en\",\"period\":{\"from\":\"2026-09-01\",\"to\":\"2026-09-30\"},\"created_at\":\"2026-09-24T10:00:00Z\","
                + "\"updated_at\":\"2026-09-24T10:00:00Z\"}";
    }

    /**
     * @param status the job status
     * @return a {@code DocumentJobView}
     */
    public static String documentJob(String status) {
        return "{\"job_id\":\"j1\",\"kind\":\"statement\",\"status\":\"" + status + "\",\"format\":\"pdf\","
                + "\"lang\":\"en\",\"period\":{\"from\":\"2026-09-01\",\"to\":\"2026-09-30\"},\"created_at\":\"2026-09-24T10:00:00Z\","
                + "\"updated_at\":\"2026-09-24T10:00:00Z\"}";
    }

    /**
     * @param uuid the invoice id
     * @param sequence the event sequence
     * @return a verified-looking {@code type=payment} webhook body
     */
    public static String paymentWebhook(String uuid, long sequence) {
        return "{\"type\":\"payment\",\"uuid\":\"" + uuid + "\",\"order_id\":\"o-" + uuid + "\","
                + "\"status\":\"paid\",\"amount\":\"25\",\"currency\":\"USDT\",\"network\":\"tron\","
                + "\"payer_amount\":\"25\",\"payer_currency\":\"USDT\",\"payment_amount\":\"25\","
                + "\"payer_address\":\"T\",\"payer_address_is_refundable\":true,\"additional_data\":\"\","
                + "\"txid\":\"tx\",\"is_final\":true,\"sequence\":" + sequence + ","
                + "\"event_at\":\"2026-09-24T10:00:00Z\"}";
    }

    /**
     * @param valid whether the payout would be accepted
     * @return a {@code PayoutValidateResult}
     */
    public static String payoutValidation(boolean valid) {
        return "{\"valid\":" + valid + ",\"amount\":\"10.50\",\"commission\":\"1\","
                + "\"payer_amount\":\"11.50\",\"currency\":\"USDT\",\"network\":\"tron\","
                + "\"address\":\"T\",\"fee_bearer\":\"merchant\",\"maturity_note\":\"\"}";
    }
}
