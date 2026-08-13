package com.forge.commerce.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money is a value object: the amount and currency together describe the business value.
 *
 * <p>We avoid double/float because they do not represent decimal values precisely and can
 * create subtle billing bugs. BigDecimal keeps monetary rules explicit.</p>
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {
    private static final int SCALE = 2;

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Money amount cannot be null.");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Negative money is not allowed in this domain.");
        }
        amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public Money add(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        ensureSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(long multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    public int compareTo(Money other) {
        ensureSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    private void ensureSameCurrency(Money other) {
        if (other == null || this.currency != other.currency) {
            throw new IllegalArgumentException("Currency mismatch: cannot combine " + this.currency + " with " + other);
        }
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
