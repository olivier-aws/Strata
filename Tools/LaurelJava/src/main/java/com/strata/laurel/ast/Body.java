/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ast;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the body of a Laurel procedure.
 * 
 * <p>A procedure body can be one of three variants:
 * <ul>
 *   <li>{@link Transparent} - a fully visible implementation</li>
 *   <li>{@link Opaque} - a hidden implementation with a postcondition</li>
 *   <li>{@link Abstract} - no implementation, only a postcondition specification</li>
 * </ul>
 */
public sealed interface Body permits Body.Transparent, Body.Opaque, Body.Abstract {
    
    /**
     * A transparent body with a fully visible implementation.
     * 
     * <p>The implementation is available for verification and can be inlined.
     * 
     * @param body the implementation expression
     */
    record Transparent(StmtExpr body) implements Body {
        public Transparent {
            Objects.requireNonNull(body, "body must not be null");
        }
    }
    
    /**
     * An opaque body with a hidden implementation.
     * 
     * <p>The implementation is not visible to callers; only the postcondition
     * is used for verification. The optional implementation can be used for
     * internal verification of the procedure itself.
     * 
     * @param postcondition the postcondition that holds after execution
     * @param implementation optional hidden implementation for internal verification
     */
    record Opaque(StmtExpr postcondition, Optional<StmtExpr> implementation) implements Body {
        public Opaque {
            Objects.requireNonNull(postcondition, "postcondition must not be null");
            Objects.requireNonNull(implementation, "implementation must not be null");
        }
    }
    
    /**
     * An abstract body with no implementation.
     * 
     * <p>Only the postcondition is specified. This is used for abstract methods
     * that must be implemented by subtypes.
     * 
     * @param postcondition the postcondition that implementations must satisfy
     */
    record Abstract(StmtExpr postcondition) implements Body {
        public Abstract {
            Objects.requireNonNull(postcondition, "postcondition must not be null");
        }
    }
    
    // ========== Factory Methods ==========
    
    /**
     * Creates a transparent body with the given implementation.
     * 
     * @param body the implementation expression
     * @return a new Transparent body
     */
    static Transparent transparent(StmtExpr body) {
        return new Transparent(body);
    }
    
    /**
     * Creates an opaque body with a postcondition and optional implementation.
     * 
     * @param postcondition the postcondition
     * @param implementation optional implementation for internal verification
     * @return a new Opaque body
     */
    static Opaque opaque(StmtExpr postcondition, StmtExpr implementation) {
        return new Opaque(postcondition, Optional.ofNullable(implementation));
    }
    
    /**
     * Creates an opaque body with only a postcondition.
     * 
     * @param postcondition the postcondition
     * @return a new Opaque body without implementation
     */
    static Opaque opaque(StmtExpr postcondition) {
        return new Opaque(postcondition, Optional.empty());
    }
    
    /**
     * Creates an abstract body with the given postcondition.
     * 
     * @param postcondition the postcondition that implementations must satisfy
     * @return a new Abstract body
     */
    static Abstract abstractBody(StmtExpr postcondition) {
        return new Abstract(postcondition);
    }
}
