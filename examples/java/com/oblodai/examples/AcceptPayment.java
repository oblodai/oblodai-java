package com.oblodai.examples;

import com.oblodai.Money;
import com.oblodai.Oblodai;
import com.oblodai.Statuses;
import com.oblodai.contract.Network;
import com.oblodai.contract.requests.PaymentRequest;
import com.oblodai.errors.OblodaiException;
import com.oblodai.models.Payment;

/**
 * Accept a crypto payment: create an invoice, show the payer where to send the money, and read the
 * invoice back.
 *
 * <p>Run it against the sandbox with a {@code test_} key:
 *
 * <pre>
 * OBLODAI_PUBLIC_ID=… OBLODAI_SECRET=… \
 *   mvn -q exec:java -Dexec.classpathScope=test \
 *       -Dexec.mainClass=com.oblodai.examples.AcceptPayment
 * </pre>
 */
public final class AcceptPayment {

    private AcceptPayment() {}

    /**
     * @param args unused
     */
    public static void main(String[] args) {
        // publicId/secret/baseUrl come from OBLODAI_PUBLIC_ID, OBLODAI_SECRET and OBLODAI_BASE_URL.
        Oblodai oblodai = Oblodai.builder().build();

        try {
            Payment invoice =
                    oblodai
                            .payments()
                            .create(
                                    new PaymentRequest()
                                            // Amounts are decimal strings. Never a double: USDT has six
                                            // decimals, BTC eight, ETH eighteen.
                                            .amount("25")
                                            // What you price in: a fiat (USD, EUR, …) or a crypto asset.
                                            .currency("USDT")
                                            // Omit the network to let the payer choose one on the pay page.
                                            .network(Network.TRON)
                                            // Your reference. The invoice is idempotent per order_id.
                                            .orderId("order-" + System.currentTimeMillis())
                                            .urlCallback("https://shop.example/oblodai/webhook"));

            System.out.println("invoice  " + invoice.uuid());
            System.out.println("status   " + invoice.status());
            System.out.println("pay page " + invoice.url());
            System.out.println("address  " + invoice.address());
            System.out.println("due      " + invoice.payerAmount() + " " + invoice.payerCurrency());

            // Prefer webhooks for state changes; polling is the fallback.
            Payment current = oblodai.payments().info(invoice.uuid());
            if (Statuses.isPaymentPaid(current.status())) {
                System.out.println("paid, merchant nets " + current.merchantAmount());
            } else if (Statuses.isPaymentUnderpaid(current.status())) {
                System.out.println(
                        "underpaid by "
                                + Money.subtract(current.payerAmount(), current.amountPaid())
                                + " — settle it with refunds().resolve(...)");
            } else {
                System.out.println("waiting for the payer");
            }
        } catch (OblodaiException e) {
            // code is the machine-readable reason; requestId is what support asks for.
            System.err.println("refused: " + e.code() + " (" + e.getMessage() + ")");
            System.err.println("request id: " + e.requestId());
            if (e.retryable()) System.err.println("the gateway says this may succeed later");
        }
    }
}
