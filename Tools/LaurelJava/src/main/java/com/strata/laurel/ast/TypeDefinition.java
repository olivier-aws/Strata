/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ast;

import java.util.Objects;

/**
 * Represents a type definition in a Laurel program.
 * 
 * <p>Type definitions can be either:
 * <ul>
 *   <li>{@link Composite} - a class/struct type with fields and methods</li>
 *   <li>{@link Constrained} - a refinement type with a constraint predicate</li>
 * </ul>
 */
public sealed interface TypeDefinition permits TypeDefinition.Composite, TypeDefinition.Constrained {
    
    /**
     * A composite type definition (class/struct).
     * 
     * @param type the composite type
     */
    record Composite(CompositeType type) implements TypeDefinition {
        public Composite {
            Objects.requireNonNull(type, "type must not be null");
        }
    }
    
    /**
     * A constrained (refinement) type definition.
     * 
     * @param type the constrained type
     */
    record Constrained(ConstrainedType type) implements TypeDefinition {
        public Constrained {
            Objects.requireNonNull(type, "type must not be null");
        }
    }
    
    // ========== Factory Methods ==========
    
    /**
     * Creates a composite type definition.
     * 
     * @param type the composite type
     * @return a new Composite type definition
     */
    static Composite composite(CompositeType type) {
        return new Composite(type);
    }
    
    /**
     * Creates a constrained type definition.
     * 
     * @param type the constrained type
     * @return a new Constrained type definition
     */
    static Constrained constrained(ConstrainedType type) {
        return new Constrained(type);
    }
    
    /**
     * Returns the name of the type being defined.
     * 
     * @return the type name
     */
    default String name() {
        if (this instanceof Composite c) {
            return c.type().name();
        } else if (this instanceof Constrained c) {
            return c.type().name();
        }
        throw new IllegalStateException("Unknown TypeDefinition variant");
    }
}
