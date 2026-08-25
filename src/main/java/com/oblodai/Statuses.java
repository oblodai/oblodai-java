package com.oblodai;

import com.oblodai.contract.PaymentStatus;
import com.oblodai.contract.PayoutStatus;
import java.util.List;

/**
 * The two lifecycles, as predicates.
 *
 * <p>Invoice: {@code select → created → confirm_check → paid | paid_over | wrong_amount | expired |
 * cancelled}. Payout: {@code pending → approved → awaiting_cosign → broadcasting → sent → confirmed |
 * failed | cancelled}.
 *
 * <p>Prefer webhooks for state changes and poll {@code info} only as a fallback.
 */
public final class Statuses {

    /** Invoice statuses after which nothing else can happen. */
    public static final List<PaymentStatus> FINAL_PAYMENT_STATUSES =
            List.of(
                    PaymentStatus.PAID,
                    PaymentStatus.PAID_OVER,
                    PaymentStatus.WRONG_AMOUNT,
                    PaymentStatus.EXPIRED,
                    PaymentStatus.CANCELLED);

    /** Payout statuses after which nothing else can happen. */
    public static final List<PayoutStatus> FINAL_PAYOUT_STATUSES =
            List.of(PayoutStatus.CONFIRMED, PayoutStatus.FAILED, PayoutStatus.CANCELLED);

    private Statuses() {}

    /**
     * @param status an invoice status
     * @return whether the invoice has reached a state it cannot leave
     */
    public static boolean isPaymentFinal(PaymentStatus status) {
        return FINAL_PAYMENT_STATUSES.contains(status);
    }

    /**
     * The merchant has the money. {@code wrong_amount} is NOT paid: it is underpaid and waits for a
     * decision — see {@link #isPaymentUnderpaid(PaymentStatus)}.
     *
     * @param status an invoice status
     * @return whether the invoice was paid in full or over
     */
    public static boolean isPaymentPaid(PaymentStatus status) {
        return status == PaymentStatus.PAID || status == PaymentStatus.PAID_OVER;
    }

    /**
     * The invoice is waiting for a merchant decision: accept the shortfall or refund it, with
     * {@code refunds().resolve(...)}.
     *
     * @param status an invoice status
     * @return whether the invoice is underpaid
     */
    public static boolean isPaymentUnderpaid(PaymentStatus status) {
        return status == PaymentStatus.WRONG_AMOUNT;
    }

    /**
     * @param status a payout status
     * @return whether the payout has reached a state it cannot leave
     */
    public static boolean isPayoutFinal(PayoutStatus status) {
        return FINAL_PAYOUT_STATUSES.contains(status);
    }

    /**
     * @param status a payout status
     * @return whether the payout reached the chain and is irreversible
     */
    public static boolean isPayoutSucceeded(PayoutStatus status) {
        return status == PayoutStatus.CONFIRMED;
    }
}
