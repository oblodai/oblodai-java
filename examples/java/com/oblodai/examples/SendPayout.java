package com.oblodai.examples;

import com.oblodai.Oblodai;
import com.oblodai.RequestOptions;
import com.oblodai.errors.OblodaiException;
import com.oblodai.generated.models.PayoutItem;
import com.oblodai.generated.models.PayoutRequest;
import com.oblodai.generated.models.PayoutValidateRequest;
import com.oblodai.generated.models.PayoutValidateResult;
import java.math.BigDecimal;

/**
 * Send a payout: dry-run it, then send it under your own idempotency key so a retry after a crash
 * can never pay twice.
 *
 * <pre>
 * OBLODAI_PUBLIC_ID=… OBLODAI_SECRET=… mvn -q exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=com.oblodai.examples.SendPayout
 * </pre>
 */
public final class SendPayout {

    private SendPayout() {}

    /**
     * @param args ignored
     */
    public static void main(String[] args) {
        try (Oblodai oblodai = Oblodai.builder().build()) {
            System.out.println(run(oblodai));
        }
    }

    /**
     * @param oblodai the client
     * @return what happened, one line
     */
    public static String run(Oblodai oblodai) {
        BigDecimal amount = new BigDecimal("10.50");
        PayoutValidateResult check =
                oblodai.payouts()
                        .validate(
                                PayoutValidateRequest.builder()
                                        .amount(amount)
                                        .currency("USDT")
                                        .network("tron")
                                        .address("TXYZ")
                                        .build());
        if (!Boolean.TRUE.equals(check.valid())) {
            return "payout would be refused: " + check.maturityNote();
        }
        try {
            PayoutItem payout =
                    oblodai.payouts()
                            .create(
                                    PayoutRequest.builder()
                                            .amount(amount)
                                            .currency("USDT")
                                            .network("tron")
                                            .address("TXYZ")
                                            .orderId("withdrawal-77")
                                            .build(),
                                    // The same key on every retry of this business operation.
                                    RequestOptions.of().idempotencyKey("withdrawal-77"));
            return "payout " + payout.uuid() + " is " + payout.status() + ", fee " + check.commission();
        } catch (OblodaiException e) {
            // "[payout.insufficient_funds] … (request_id=…)": quote the request id to support.
            return "payout failed: " + e.getMessage() + (e.retryable() ? " - try again later" : "");
        }
    }
}
