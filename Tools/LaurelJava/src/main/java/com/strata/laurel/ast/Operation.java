/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ast;

/**
 * Primitive operations supported in Laurel expressions.
 * 
 * <p>These operations work on different types:
 * <ul>
 *   <li>Boolean operations: {@link #EQ}, {@link #NEQ}, {@link #AND}, {@link #OR}, {@link #NOT}</li>
 *   <li>Arithmetic operations: {@link #NEG}, {@link #ADD}, {@link #SUB}, {@link #MUL}, {@link #DIV}, {@link #MOD}</li>
 *   <li>Comparison operations: {@link #LT}, {@link #LEQ}, {@link #GT}, {@link #GEQ}</li>
 * </ul>
 * 
 * <p>Note: Equality on composite types uses reference equality for impure types,
 * and structural equality for pure ones.
 */
public enum Operation {
    // ========== Boolean Operations ==========
    
    /** Equality comparison. */
    EQ,
    
    /** Inequality comparison. */
    NEQ,
    
    /** Logical AND. */
    AND,
    
    /** Logical OR. */
    OR,
    
    /** Logical NOT (unary). */
    NOT,
    
    // ========== Arithmetic Operations ==========
    
    /** Arithmetic negation (unary). */
    NEG,
    
    /** Addition. */
    ADD,
    
    /** Subtraction. */
    SUB,
    
    /** Multiplication. */
    MUL,
    
    /** Division. */
    DIV,
    
    /** Modulo (remainder). */
    MOD,
    
    // ========== Comparison Operations ==========
    
    /** Less than. */
    LT,
    
    /** Less than or equal. */
    LEQ,
    
    /** Greater than. */
    GT,
    
    /** Greater than or equal. */
    GEQ
}
