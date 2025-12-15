/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ast;

import java.util.Objects;

/**
 * Represents a constrained (refinement) type definition in Laurel.
 * 
 * <p>A constrained type is a subtype of a base type where values must satisfy
 * a constraint predicate. For example, a "PositiveInt" type could be defined
 * as integers where the value is greater than zero.
 * 
 * <p>The witness expression provides a default value that satisfies the constraint,
 * proving that the type is inhabited.
 * 
 * @param name the type name
 * @param base the base type being constrained
 * @param valueName the name used to refer to the value in the constraint
 * @param constraint the constraint predicate that values must satisfy
 * @param witness a witness value proving the type is inhabited
 */
public record ConstrainedType(
        String name,
        HighType base,
        String valueName,
        StmtExpr constraint,
        StmtExpr witness
) {
    
    /**
     * Creates a new constrained type with the given properties.
     * 
     * @throws NullPointerException if any field is null
     */
    public ConstrainedType {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(base, "base must not be null");
        Objects.requireNonNull(valueName, "valueName must not be null");
        Objects.requireNonNull(constraint, "constraint must not be null");
        Objects.requireNonNull(witness, "witness must not be null");
    }
    
    /**
     * Factory method to create a constrained type.
     * 
     * @param name the type name
     * @param base the base type
     * @param valueName the name for the value in the constraint
     * @param constraint the constraint predicate
     * @param witness the witness value
     * @return a new ConstrainedType
     */
    public static ConstrainedType of(
            String name,
            HighType base,
            String valueName,
            StmtExpr constraint,
            StmtExpr witness
    ) {
        return new ConstrainedType(name, base, valueName, constraint, witness);
    }
}
