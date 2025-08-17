package com.dusk.module.auth.enums;
/**
 * @author kefuming
 * @date 2020-05-19 9:50
 */
public enum EditionPaymentType {
    /**
     * Payment on first tenant registration.
     */
    NewRegistration,

    /**'
     * Purchasing by an existing tenant that currently using trial version of a paid edition.
     */
    BuyNow,

    /**
     * A tenant is upgrading it's edition (either from a free edition or from a low-price paid edition).
     */
    Upgrade,

    /**
     * A tenant is extending it's current edition (without changing the edition).
     */
    Extend
}
