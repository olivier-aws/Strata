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
 * Fluent builder for constructing {@link CompositeType} instances.
 * 
 * <p>This builder provides a convenient API for creating composite types with
 * validation of required fields. Required fields are:
 * <ul>
 *   <li>name - the type name</li>
 * </ul>
 * 
 * <p>Optional fields have sensible defaults:
 * <ul>
 *   <li>extending - defaults to empty list</li>
 *   <li>fields - defaults to empty list</li>
 *   <li>instanceProcedures - defaults to empty list</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * CompositeType point = new CompositeTypeBuilder()
 *     .name("Point")
 *     .field(Field.mutable("x", HighType.tInt()))
 *     .field(Field.mutable("y", HighType.tInt()))
 *     .procedure(distanceProc)
 *     .build();
 * }</pre>
 */
public class CompositeTypeBuilder {
    
    private String name;
    private final List<String> extending = new ArrayList<>();
    private final List<Field> fields = new ArrayList<>();
    private final List<Procedure> instanceProcedures = new ArrayList<>();
    
    /**
     * Creates a new CompositeTypeBuilder with default values.
     */
    public CompositeTypeBuilder() {
        // Defaults will be applied in build() if not set
    }
    
    /**
     * Sets the type name.
     * 
     * @param name the type name (required)
     * @return this builder for chaining
     * @throws NullPointerException if name is null
     */
    public CompositeTypeBuilder name(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        return this;
    }

    
    /**
     * Adds a type that this type extends.
     * 
     * @param typeName the name of the type to extend
     * @return this builder for chaining
     * @throws NullPointerException if typeName is null
     */
    public CompositeTypeBuilder extending(String typeName) {
        Objects.requireNonNull(typeName, "typeName must not be null");
        this.extending.add(typeName);
        return this;
    }
    
    /**
     * Sets all types that this type extends, replacing any previously added.
     * 
     * @param extending the type names to extend
     * @return this builder for chaining
     * @throws NullPointerException if extending is null
     */
    public CompositeTypeBuilder extending(List<String> extending) {
        Objects.requireNonNull(extending, "extending must not be null");
        this.extending.clear();
        this.extending.addAll(extending);
        return this;
    }
    
    /**
     * Adds a field to the type.
     * 
     * @param field the field to add
     * @return this builder for chaining
     * @throws NullPointerException if field is null
     */
    public CompositeTypeBuilder field(Field field) {
        Objects.requireNonNull(field, "field must not be null");
        this.fields.add(field);
        return this;
    }
    
    /**
     * Adds a field with the given name, mutability, and type.
     * 
     * @param name the field name
     * @param isMutable whether the field is mutable
     * @param type the field type
     * @return this builder for chaining
     * @throws NullPointerException if name or type is null
     */
    public CompositeTypeBuilder field(String name, boolean isMutable, HighType type) {
        return field(new Field(name, isMutable, type));
    }
    
    /**
     * Adds a mutable field with the given name and type.
     * 
     * @param name the field name
     * @param type the field type
     * @return this builder for chaining
     * @throws NullPointerException if name or type is null
     */
    public CompositeTypeBuilder mutableField(String name, HighType type) {
        return field(Field.mutable(name, type));
    }
    
    /**
     * Adds an immutable field with the given name and type.
     * 
     * @param name the field name
     * @param type the field type
     * @return this builder for chaining
     * @throws NullPointerException if name or type is null
     */
    public CompositeTypeBuilder immutableField(String name, HighType type) {
        return field(Field.immutable(name, type));
    }
    
    /**
     * Sets all fields, replacing any previously added.
     * 
     * @param fields the fields
     * @return this builder for chaining
     * @throws NullPointerException if fields is null
     */
    public CompositeTypeBuilder fields(List<Field> fields) {
        Objects.requireNonNull(fields, "fields must not be null");
        this.fields.clear();
        this.fields.addAll(fields);
        return this;
    }
    
    /**
     * Adds an instance procedure (method) to the type.
     * 
     * @param procedure the procedure to add
     * @return this builder for chaining
     * @throws NullPointerException if procedure is null
     */
    public CompositeTypeBuilder procedure(Procedure procedure) {
        Objects.requireNonNull(procedure, "procedure must not be null");
        this.instanceProcedures.add(procedure);
        return this;
    }
    
    /**
     * Sets all instance procedures, replacing any previously added.
     * 
     * @param procedures the procedures
     * @return this builder for chaining
     * @throws NullPointerException if procedures is null
     */
    public CompositeTypeBuilder procedures(List<Procedure> procedures) {
        Objects.requireNonNull(procedures, "procedures must not be null");
        this.instanceProcedures.clear();
        this.instanceProcedures.addAll(procedures);
        return this;
    }
    
    /**
     * Builds the CompositeType instance.
     * 
     * @return a new CompositeType with the configured values
     * @throws IllegalStateException if required fields (name) are not set
     */
    public CompositeType build() {
        if (name == null) {
            throw new IllegalStateException("name is required");
        }
        
        return new CompositeType(
                name,
                List.copyOf(extending),
                List.copyOf(fields),
                List.copyOf(instanceProcedures)
        );
    }
    
    /**
     * Creates a new builder initialized from an existing CompositeType.
     * 
     * @param type the composite type to copy from
     * @return a new builder with values from the type
     * @throws NullPointerException if type is null
     */
    public static CompositeTypeBuilder from(CompositeType type) {
        Objects.requireNonNull(type, "type must not be null");
        return new CompositeTypeBuilder()
                .name(type.name())
                .extending(type.extending())
                .fields(type.fields())
                .procedures(type.instanceProcedures());
    }
}
