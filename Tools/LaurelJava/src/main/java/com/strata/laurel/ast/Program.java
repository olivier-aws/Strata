/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ast;

import java.util.List;
import java.util.Objects;

/**
 * Represents a complete Laurel program.
 * 
 * <p>A program is the top-level construct in Laurel, containing:
 * <ul>
 *   <li>Static procedures (functions not associated with a type)</li>
 *   <li>Static fields (global variables)</li>
 *   <li>Type definitions (composite and constrained types)</li>
 * </ul>
 * 
 * @param staticProcedures the static procedures in the program
 * @param staticFields the static (global) fields in the program
 * @param types the type definitions in the program
 */
public record Program(
        List<Procedure> staticProcedures,
        List<Field> staticFields,
        List<TypeDefinition> types
) {
    
    /**
     * Creates a new program with the given components.
     * 
     * @throws NullPointerException if any field is null
     */
    public Program {
        Objects.requireNonNull(staticProcedures, "staticProcedures must not be null");
        Objects.requireNonNull(staticFields, "staticFields must not be null");
        Objects.requireNonNull(types, "types must not be null");
        staticProcedures = List.copyOf(staticProcedures);
        staticFields = List.copyOf(staticFields);
        types = List.copyOf(types);
    }
    
    // ========== Factory Methods ==========
    
    /**
     * Creates an empty program with no procedures, fields, or types.
     * 
     * @return a new empty Program
     */
    public static Program empty() {
        return new Program(List.of(), List.of(), List.of());
    }
    
    /**
     * Creates a program with only static procedures.
     * 
     * @param procedures the static procedures
     * @return a new Program with only procedures
     */
    public static Program withProcedures(List<Procedure> procedures) {
        return new Program(procedures, List.of(), List.of());
    }
    
    /**
     * Creates a program with only static procedures (varargs).
     * 
     * @param procedures the static procedures
     * @return a new Program with only procedures
     */
    public static Program withProcedures(Procedure... procedures) {
        return new Program(List.of(procedures), List.of(), List.of());
    }
    
    /**
     * Creates a program with only type definitions.
     * 
     * @param types the type definitions
     * @return a new Program with only types
     */
    public static Program withTypes(List<TypeDefinition> types) {
        return new Program(List.of(), List.of(), types);
    }
    
    /**
     * Creates a program with only type definitions (varargs).
     * 
     * @param types the type definitions
     * @return a new Program with only types
     */
    public static Program withTypes(TypeDefinition... types) {
        return new Program(List.of(), List.of(), List.of(types));
    }
    
    /**
     * Creates a program with procedures and types.
     * 
     * @param procedures the static procedures
     * @param types the type definitions
     * @return a new Program with procedures and types
     */
    public static Program of(List<Procedure> procedures, List<TypeDefinition> types) {
        return new Program(procedures, List.of(), types);
    }
}
