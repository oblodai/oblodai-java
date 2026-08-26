// GENERATED FILE — do not edit. Source: contract/contract.json (core 2cc44c16f516).
// Regenerate with: codegen/run.sh
package com.oblodai.contract;

/**
 * What every vocabulary this contract exports has in common: the exact string the API used, and
 * whether that string is one this snapshot knows.
 *
 * <p>The vocabularies are open. A gateway that starts sending a value this SDK has never heard
 * of must not break a deployed client, and must not have what it said thrown away either: the
 * raw string stays readable through {@link #wire()}, and {@link #isKnown()} says it is new.
 */
public interface Vocabulary {

    /** The exact string the API uses. */
    String wire();

    /** Whether this is one of the values this contract snapshot declares. */
    boolean isKnown();
}
