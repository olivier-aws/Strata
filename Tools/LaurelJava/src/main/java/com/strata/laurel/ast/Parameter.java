/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ast;

import java.util.Objects;

/**
 * Represents a parameter in a Laurel procedure.
 * 
 * <p>Parameters have a name and a type, and are used to define the inputs
 * and outputs of procedures.
 * 
 * @param name the parameter name
 * @param type the parameter type
 */
public record Parameter(String name, HighType type) {
    
    /**
     * Creates a new parameter with the given name and type.
     * 
     * @throws NullPointerException if name or type is null
     */
    public Parameter {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }
    
    /**
     * Factory method to create a parameter.
     * 
     * @param name the parameter name
     * @param type the parameter type
     * @return a new Parameter instance
     */
    public static Parameter of(String name, HighType type) {
        return new Parameter(name, type);
    }
}
