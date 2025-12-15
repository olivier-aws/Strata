/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ast;

import java.util.List;
import java.util.Objects;

/**
 * Represents a composite (class/struct) type definition in Laurel.
 * 
 * <p>A composite type consists of:
 * <ul>
 *   <li>A name identifying the type</li>
 *   <li>A list of types it extends (for inheritance)</li>
 *   <li>Fields (data members)</li>
 *   <li>Instance procedures (methods)</li>
 * </ul>
 * 
 * @param name the type name
 * @param extending list of type names this type extends
 * @param fields the fields of this type
 * @param instanceProcedures the instance methods of this type
 */
public record CompositeType(
        String name,
        List<String> extending,
        List<Field> fields,
        List<Procedure> instanceProcedures
) {
    
    /**
     * Creates a new composite type with the given properties.
     * 
     * @throws NullPointerException if any field is null
     */
    public CompositeType {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(extending, "extending must not be null");
        Objects.requireNonNull(fields, "fields must not be null");
        Objects.requireNonNull(instanceProcedures, "instanceProcedures must not be null");
        extending = List.copyOf(extending);
        fields = List.copyOf(fields);
        instanceProcedures = List.copyOf(instanceProcedures);
    }
    
    /**
     * Factory method to create a composite type with no inheritance.
     * 
     * @param name the type name
     * @param fields the fields
     * @param instanceProcedures the instance methods
     * @return a new CompositeType
     */
    public static CompositeType of(String name, List<Field> fields, List<Procedure> instanceProcedures) {
        return new CompositeType(name, List.of(), fields, instanceProcedures);
    }
    
    /**
     * Factory method to create a simple composite type with only fields.
     * 
     * @param name the type name
     * @param fields the fields
     * @return a new CompositeType with no methods
     */
    public static CompositeType withFields(String name, List<Field> fields) {
        return new CompositeType(name, List.of(), fields, List.of());
    }
    
    /**
     * Factory method to create a simple composite type with only fields (varargs).
     * 
     * @param name the type name
     * @param fields the fields
     * @return a new CompositeType with no methods
     */
    public static CompositeType withFields(String name, Field... fields) {
        return new CompositeType(name, List.of(), List.of(fields), List.of());
    }
}
