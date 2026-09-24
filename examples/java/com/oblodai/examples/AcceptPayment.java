package com.oblodai.examples;

import com.oblodai.Oblodai;
import com.oblodai.Statuses;
import com.oblodai.generated.models.LookupRequest;
import com.oblodai.generated.models.PaymentInfoResult;
import com.oblodai.generated.models.PaymentRequest;
import com.oblodai.generated.models.PaymentView;

/**
 * Accept a payment: create an invoice, send the payer to its page, read it back and interpret the
 * status.
 *
 * <pre>
 * OBLODAI_PUBLIC_ID=… OBLODAI_SECRET=… mvn -q exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=com.oblodai.examples.AcceptPayment
 * </pre>
 */
public final class AcceptPayment {

    private AcceptPayment() {}

    /**
     * @param args ignored
     */
    public static void main(String[] args) {
        try (Oblodai oblodai = Oblodai.builder().build()) { // OBLODAI_PUBLIC_ID / OBLODAI_SECRET
            System.out.println(run(oblodai));
        }
    }

    /**
     * @param oblodai the client
     * @return what happened, one line
     */
    public static String run(Oblodai oblodai) {
        PaymentView invoice =
                oblodai.payments()
                        .create(
                                PaymentRequest.builder()
                                        .amount("25") // a decimal string or a BigDecimal, never a double
                                        .currency("USDT")
                                        .orderId("order-1001") // your reference; idempotent per order
                                        .urlCallback("https://shop.example/oblodai/webhook")
                                        .build());
        System.out.println("send the payer to " + invoice.url());

        // Webhooks are the source of truth; polling is for reconciliation.
        PaymentInfoResult now =
                oblodai.payments().getInfo(LookupRequest.builder().uuid(invoice.uuid()).build());
        if (Statuses.isPaymentPaid(now.status())) {
            return "invoice " + now.orderId() + " is paid: " + now.amountPaid() + " " + now.payerCurrency();
        }
        if (Statuses.isPaymentFinal(now.status())) {
            return "invoice " + now.orderId() + " ended as " + now.status();
        }
        return "invoice " + now.orderId() + " is " + now.status() + ", waiting for the payer";
    }
}
