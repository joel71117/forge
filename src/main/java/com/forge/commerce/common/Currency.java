package com.forge.commerce.common;

/**
 * A small currency vocabulary for the domain model.
 *
 * <p>We deliberately keep it explicit instead of using raw strings because a domain rule
 * like "USD + EUR" should fail fast at the type boundary, not hide in a runtime bug.</p>
 */
public enum Currency {
    USD,
    EUR,
    GBP
}
