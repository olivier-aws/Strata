/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.builder;

import com.strata.laurel.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for constructing {@link Program} instances.
 * 
 * <p>This builder provides a convenient API for creating programs with
 * validation. All fields are optional and default to empty lists.
 * 
 * <p>Example usage:
 * <pre>{@code
 * Program program = new ProgramBuilder()
 *     .procedure(mainProc)
 *     .procedure(helperProc)
 *     .staticField(Field.mutable("counter", HighType.tInt()))
 *     .type(TypeDefinition.composite(pointType))
 *     .build();
 * }</pre>
 */
public class ProgramBuilder {
    
    private final List<Procedure> staticProcedures = new ArrayList<>();
    private final List<Field> staticFields = new ArrayList<>();
    private final List<TypeDefinition> types = new ArrayList<>();
    
    /**
     * Creates a new ProgramBuilder with default values.
     */
    public ProgramBuilder() {
        // All fields default to empty lists
    }
    
    /**
     * Adds a static procedure to the program.
     * 
     * @param procedure the procedure to add
     * @return this builder for chaining
     * @throws NullPointerException if procedure is null
     */
    public ProgramBuilder procedure(Procedure procedure) {
        Objects.requireNonNull(procedure, "procedure must not be null");
        this.staticProcedures.add(procedure);
        return this;
    }

    
    /**
     * Sets all static procedures, replacing any previously added.
     * 
     * @param procedures the procedures
     * @return this builder for chaining
     * @throws NullPointerException if procedures is null
     */
    public ProgramBuilder procedures(List<Procedure> procedures) {
        Objects.requireNonNull(procedures, "procedures must not be null");
        this.staticProcedures.clear();
        this.staticProcedures.addAll(procedures);
        return this;
    }
    
    /**
     * Adds a static field to the program.
     * 
     * @param field the field to add
     * @return this builder for chaining
     * @throws NullPointerException if field is null
     */
    public ProgramBuilder staticField(Field field) {
        Objects.requireNonNull(field, "field must not be null");
        this.staticFields.add(field);
        return this;
    }
    
    /**
     * Adds a static field with the given name, mutability, and type.
     * 
     * @param name the field name
     * @param isMutable whether the field is mutable
     * @param type the field type
     * @return this builder for chaining
     * @throws NullPointerException if name or type is null
     */
    public ProgramBuilder staticField(String name, boolean isMutable, HighType type) {
        return staticField(new Field(name, isMutable, type));
    }
    
    /**
     * Adds a mutable static field with the given name and type.
     * 
     * @param name the field name
     * @param type the field type
     * @return this builder for chaining
     * @throws NullPointerException if name or type is null
     */
    public ProgramBuilder mutableStaticField(String name, HighType type) {
        return staticField(Field.mutable(name, type));
    }
    
    /**
     * Adds an immutable static field with the given name and type.
     * 
     * @param name the field name
     * @param type the field type
     * @return this builder for chaining
     * @throws NullPointerException if name or type is null
     */
    public ProgramBuilder immutableStaticField(String name, HighType type) {
        return staticField(Field.immutable(name, type));
    }
    
    /**
     * Sets all static fields, replacing any previously added.
     * 
     * @param fields the fields
     * @return this builder for chaining
     * @throws NullPointerException if fields is null
     */
    public ProgramBuilder staticFields(List<Field> fields) {
        Objects.requireNonNull(fields, "fields must not be null");
        this.staticFields.clear();
        this.staticFields.addAll(fields);
        return this;
    }
    
    /**
     * Adds a type definition to the program.
     * 
     * @param type the type definition to add
     * @return this builder for chaining
     * @throws NullPointerException if type is null
     */
    public ProgramBuilder type(TypeDefinition type) {
        Objects.requireNonNull(type, "type must not be null");
        this.types.add(type);
        return this;
    }
    
    /**
     * Adds a composite type definition to the program.
     * 
     * @param compositeType the composite type to add
     * @return this builder for chaining
     * @throws NullPointerException if compositeType is null
     */
    public ProgramBuilder compositeType(CompositeType compositeType) {
        return type(TypeDefinition.composite(compositeType));
    }
    
    /**
     * Adds a constrained type definition to the program.
     * 
     * @param constrainedType the constrained type to add
     * @return this builder for chaining
     * @throws NullPointerException if constrainedType is null
     */
    public ProgramBuilder constrainedType(ConstrainedType constrainedType) {
        return type(TypeDefinition.constrained(constrainedType));
    }
    
    /**
     * Sets all type definitions, replacing any previously added.
     * 
     * @param types the type definitions
     * @return this builder for chaining
     * @throws NullPointerException if types is null
     */
    public ProgramBuilder types(List<TypeDefinition> types) {
        Objects.requireNonNull(types, "types must not be null");
        this.types.clear();
        this.types.addAll(types);
        return this;
    }
    
    /**
     * Builds the Program instance.
     * 
     * @return a new Program with the configured values
     */
    public Program build() {
        return new Program(
                List.copyOf(staticProcedures),
                List.copyOf(staticFields),
                List.copyOf(types)
        );
    }
    
    /**
     * Creates a new builder initialized from an existing Program.
     * 
     * @param program the program to copy from
     * @return a new builder with values from the program
     * @throws NullPointerException if program is null
     */
    public static ProgramBuilder from(Program program) {
        Objects.requireNonNull(program, "program must not be null");
        return new ProgramBuilder()
                .procedures(program.staticProcedures())
                .staticFields(program.staticFields())
                .types(program.types());
    }
}
