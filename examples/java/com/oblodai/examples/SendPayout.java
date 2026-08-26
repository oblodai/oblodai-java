package com.oblodai.examples;

import com.oblodai.Oblodai;
import com.oblodai.RequestOptions;
import com.oblodai.contract.Network;
import com.oblodai.contract.requests.PayoutRequest;
import com.oblodai.contract.requests.PayoutValidateRequest;
import com.oblodai.errors.OblodaiException;
import com.oblodai.models.Payout;
import com.oblodai.models.PayoutValidation;

/**
 * Send money out: dry-run the payout, then create it under your own idempotency key so a lost
 * response can never become a second payout.
 *
 * <pre>
 * OBLODAI_PUBLIC_ID=… OBLODAI_SECRET=… \
 *   mvn -q exec:java -Dexec.classpathScope=test \
 *       -Dexec.mainClass=com.oblodai.examples.SendPayout
 * </pre>
 */
public final class SendPayout {

    private SendPayout() {}

    /**
     * @param args optional: address, amount, currency, network
     */
    public static void main(String[] args) {
        String address = args.length > 0 ? args[0] : "TQrY8bkbpXKPt2LZbU8jqfnpFbUSF15sbx";
        String amount = args.length > 1 ? args[1] : "10";
        String currency = args.length > 2 ? args[2] : "USDT";
        String network = args.length > 3 ? args[3] : Network.TRON.wire();

        Oblodai oblodai = Oblodai.builder().build();

        // Your own reference. Reusing it makes the whole operation replayable across restarts: the
        // gateway deduplicates by order_id, and the SDK reuses your key on every retry.
        String orderId = "payout-42";

        try {
            PayoutValidation check =
                    oblodai
                            .payouts()
                            .validate(
                                    new PayoutValidateRequest()
                                            .amount(amount)
                                            .currency(currency)
                                            .network(network)
                                            .address(address));
            System.out.println("valid       " + check.valid());
            System.out.println("commission  " + check.commission() + " " + check.currency());
            System.out.println("debited     " + check.payerAmount());
            if (check.maturityNote() != null && !check.maturityNote().isEmpty()) {
                System.out.println("note        " + check.maturityNote());
            }

            Payout payout =
                    oblodai
                            .payouts()
                            .create(
                                    new PayoutRequest()
                                            .amount(amount)
                                            .currency(currency)
                                            .network(network)
                                            .address(address)
                                            .orderId(orderId),
                                    RequestOptions.of().idempotencyKey(orderId));

            System.out.println("payout      " + payout.uuid());
            System.out.println("status      " + payout.status());
            System.out.println("fee bearer  " + payout.feeBearer());
        } catch (OblodaiException e) {
            switch (e.code()) {
                case "payout.insufficient_funds", "payout.funds_maturing" ->
                        // Retryable: the balance may still arrive. Wait for retryAfter, then repeat
                        // the call with the SAME idempotency key.
                        System.err.println(
                                "not enough funds yet; retry in "
                                        + (e.retryAfter() == null ? 60 : e.retryAfter())
                                        + "s");
                case "payout.bad_address" ->
                        System.err.println("that address is not valid on this network");
                default -> System.err.println("refused: " + e.code() + " (" + e.getMessage() + ")");
            }
        }
    }
}
