package com.oblodai;

import com.oblodai.generated.models.PaymentStatus;
import com.oblodai.generated.models.PayoutStatus;
import java.util.List;

/**
 * The two lifecycles, as predicates. Which statuses are final and which of those are a success is
 * the contract's ({@code x-status-classes}, generated into {@code PaymentStatus.isFinal()} /
 * {@code isSuccess()} and the same on {@code PayoutStatus}); these helpers read it.
 *
 * <p>Prefer webhooks for state changes and poll {@code info} only as a fallback.
 */
public final class Statuses {

    /** Invoice statuses after which nothing else can happen: the contract's {@code x-status-classes}. */
    public static final List<PaymentStatus> FINAL_PAYMENT_STATUSES = PaymentStatus.finalValues();

    /** Payout statuses after which nothing else can happen: the contract's {@code x-status-classes}. */
    public static final List<PayoutStatus> FINAL_PAYOUT_STATUSES = PayoutStatus.finalValues();

    private Statuses() {}

    /**
     * @param status an invoice status
     * @return whether the invoice has reached a state it cannot leave
     */
    public static boolean isPaymentFinal(PaymentStatus status) {
        return status != null && status.isFinal();
    }

    /**
     * The merchant has the money. {@code wrong_amount} is NOT paid: it is underpaid and waits for a
     * decision — see {@link #isPaymentUnderpaid(PaymentStatus)}.
     *
     * @param status an invoice status
     * @return whether the invoice was paid in full or over
     */
    public static boolean isPaymentPaid(PaymentStatus status) {
        return status != null && status.isSuccess();
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
        return status != null && status.isFinal();
    }

    /**
     * @param status a payout status
     * @return whether the payout reached the chain and is irreversible
     */
    public static boolean isPayoutSucceeded(PayoutStatus status) {
        return status != null && status.isSuccess();
    }
}
