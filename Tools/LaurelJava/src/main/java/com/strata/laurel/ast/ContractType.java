/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ast;

/**
 * Types of contracts that can be extracted from a function using {@link StmtExpr.ContractOf}.
 * 
 * <p>Contracts specify the behavior and constraints of procedures:
 * <ul>
 *   <li>{@link #READS} - specifies what the procedure may read</li>
 *   <li>{@link #MODIFIES} - specifies what the procedure may modify</li>
 *   <li>{@link #PRECONDITION} - specifies what must be true before the procedure executes</li>
 *   <li>{@link #POSTCONDITION} - specifies what will be true after the procedure executes</li>
 * </ul>
 */
public enum ContractType {
    /** The reads clause, specifying what the procedure may read. */
    READS,
    
    /** The modifies clause, specifying what the procedure may modify. */
    MODIFIES,
    
    /** The precondition, specifying what must be true before execution. */
    PRECONDITION,
    
    /** The postcondition, specifying what will be true after execution. */
    POSTCONDITION
}
