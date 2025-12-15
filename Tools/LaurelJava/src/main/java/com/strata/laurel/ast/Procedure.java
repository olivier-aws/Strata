/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ast;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a procedure (function/method) in Laurel.
 * 
 * <p>A procedure is the main verification unit in Laurel, consisting of:
 * <ul>
 *   <li>A name identifying the procedure</li>
 *   <li>Input parameters</li>
 *   <li>An output type</li>
 *   <li>Contracts: precondition, reads clause, modifies clause</li>
 *   <li>A decreases clause for termination proofs</li>
 *   <li>A determinism flag</li>
 *   <li>A body (transparent, opaque, or abstract)</li>
 * </ul>
 * 
 * @param name the procedure name
 * @param inputs the input parameters
 * @param output the output type
 * @param precondition the precondition that must hold before execution
 * @param decreases the decreases clause for termination proofs
 * @param deterministic whether the procedure is deterministic
 * @param reads optional reads clause specifying what may be read
 * @param modifies the modifies clause specifying what may be modified
 * @param body the procedure body
 */
public record Procedure(
        String name,
        List<Parameter> inputs,
        HighType output,
        StmtExpr precondition,
        StmtExpr decreases,
        boolean deterministic,
        Optional<StmtExpr> reads,
        StmtExpr modifies,
        Body body
) {
    
    /**
     * Creates a new procedure with the given properties.
     * 
     * @throws NullPointerException if any required field is null
     */
    public Procedure {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(inputs, "inputs must not be null");
        Objects.requireNonNull(output, "output must not be null");
        Objects.requireNonNull(precondition, "precondition must not be null");
        Objects.requireNonNull(decreases, "decreases must not be null");
        Objects.requireNonNull(reads, "reads must not be null");
        Objects.requireNonNull(modifies, "modifies must not be null");
        Objects.requireNonNull(body, "body must not be null");
        inputs = List.copyOf(inputs);
    }
}
