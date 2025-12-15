/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ast;

import java.util.List;
import java.util.Objects;

/**
 * Represents the type system in Laurel including primitives and user-defined types.
 * 
 * <p>This sealed interface hierarchy mirrors the Lean HighType inductive type:
 * <ul>
 *   <li>{@link TVoid} - void type</li>
 *   <li>{@link TBool} - boolean type</li>
 *   <li>{@link TInt} - integer type</li>
 *   <li>{@link TFloat64} - 64-bit floating point type</li>
 *   <li>{@link UserDefined} - user-defined named type</li>
 *   <li>{@link Applied} - generic type application</li>
 *   <li>{@link Pure} - pure (value) type wrapper</li>
 *   <li>{@link Intersection} - intersection of multiple types</li>
 * </ul>
 */
public sealed interface HighType permits
        HighType.TVoid,
        HighType.TBool,
        HighType.TInt,
        HighType.TFloat64,
        HighType.UserDefined,
        HighType.Applied,
        HighType.Pure,
        HighType.Intersection {

    // ========== Primitive Types ==========

    /** The void type, representing no value. */
    record TVoid() implements HighType {}

    /** The boolean type. */
    record TBool() implements HighType {}

    /** The integer type (arbitrary precision). */
    record TInt() implements HighType {}

    /** The 64-bit floating point type (used for JavaScript number, Python float, Java double). */
    record TFloat64() implements HighType {}

    // ========== Compound Types ==========

    /**
     * A user-defined named type.
     * @param name the type name identifier
     */
    record UserDefined(String name) implements HighType {
        public UserDefined {
            Objects.requireNonNull(name, "name must not be null");
        }
    }


    /**
     * A generic type application (e.g., List&lt;Int&gt;).
     * @param base the base type being applied
     * @param typeArguments the type arguments
     */
    record Applied(HighType base, List<HighType> typeArguments) implements HighType {
        public Applied {
            Objects.requireNonNull(base, "base must not be null");
            Objects.requireNonNull(typeArguments, "typeArguments must not be null");
            typeArguments = List.copyOf(typeArguments);
        }
    }

    /**
     * A pure type wrapper, representing a composite type that does not support reference equality.
     * @param base the underlying type
     */
    record Pure(HighType base) implements HighType {
        public Pure {
            Objects.requireNonNull(base, "base must not be null");
        }
    }

    /**
     * An intersection type combining multiple types.
     * <p>Java has implicit intersection types, e.g., {@code <cond> ? A : B} could be typed as {@code X & Y}.
     * @param types the types in the intersection
     */
    record Intersection(List<HighType> types) implements HighType {
        public Intersection {
            Objects.requireNonNull(types, "types must not be null");
            types = List.copyOf(types);
        }
    }

    // ========== Factory Methods ==========

    /** Returns the singleton void type instance. */
    static TVoid tVoid() {
        return new TVoid();
    }

    /** Returns the singleton boolean type instance. */
    static TBool tBool() {
        return new TBool();
    }

    /** Returns the singleton integer type instance. */
    static TInt tInt() {
        return new TInt();
    }

    /** Returns the singleton float64 type instance. */
    static TFloat64 tFloat64() {
        return new TFloat64();
    }

    /**
     * Creates a user-defined type with the given name.
     * @param name the type name
     * @return a new UserDefined type
     */
    static UserDefined userDefined(String name) {
        return new UserDefined(name);
    }

    /**
     * Creates an applied (generic) type.
     * @param base the base type
     * @param typeArguments the type arguments
     * @return a new Applied type
     */
    static Applied applied(HighType base, List<HighType> typeArguments) {
        return new Applied(base, typeArguments);
    }

    /**
     * Creates an applied (generic) type with varargs.
     * @param base the base type
     * @param typeArguments the type arguments
     * @return a new Applied type
     */
    static Applied applied(HighType base, HighType... typeArguments) {
        return new Applied(base, List.of(typeArguments));
    }

    /**
     * Creates a pure type wrapper.
     * @param base the underlying type
     * @return a new Pure type
     */
    static Pure pure(HighType base) {
        return new Pure(base);
    }

    /**
     * Creates an intersection type.
     * @param types the types in the intersection
     * @return a new Intersection type
     */
    static Intersection intersection(List<HighType> types) {
        return new Intersection(types);
    }

    /**
     * Creates an intersection type with varargs.
     * @param types the types in the intersection
     * @return a new Intersection type
     */
    static Intersection intersection(HighType... types) {
        return new Intersection(List.of(types));
    }
}
