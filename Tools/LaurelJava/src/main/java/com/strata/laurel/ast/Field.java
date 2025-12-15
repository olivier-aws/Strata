/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ast;

import java.util.Objects;

/**
 * Represents a field in a Laurel composite type or program.
 * 
 * <p>Fields have a name, mutability flag, and type. Mutable fields can be
 * modified after initialization, while immutable fields cannot.
 * 
 * @param name the field name
 * @param isMutable whether the field can be modified after initialization
 * @param type the field type
 */
public record Field(String name, boolean isMutable, HighType type) {
    
    /**
     * Creates a new field with the given name, mutability, and type.
     * 
     * @throws NullPointerException if name or type is null
     */
    public Field {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }
    
    /**
     * Factory method to create a field.
     * 
     * @param name the field name
     * @param isMutable whether the field is mutable
     * @param type the field type
     * @return a new Field instance
     */
    public static Field of(String name, boolean isMutable, HighType type) {
        return new Field(name, isMutable, type);
    }
    
    /**
     * Factory method to create a mutable field.
     * 
     * @param name the field name
     * @param type the field type
     * @return a new mutable Field instance
     */
    public static Field mutable(String name, HighType type) {
        return new Field(name, true, type);
    }
    
    /**
     * Factory method to create an immutable field.
     * 
     * @param name the field name
     * @param type the field type
     * @return a new immutable Field instance
     */
    public static Field immutable(String name, HighType type) {
        return new Field(name, false, type);
    }
}
