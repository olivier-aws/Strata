/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.generators;

import com.strata.laurel.ast.HighType;
import net.jqwik.api.*;

import java.util.List;

/**
 * jqwik generator for creating random {@link HighType} instances.
 * 
 * <p>Handles recursive types with bounded depth to prevent infinite recursion.
 */
public class HighTypeGenerator {

    private static final int DEFAULT_MAX_DEPTH = 3;
    private static final int MAX_TYPE_ARGS = 3;
    private static final int MAX_INTERSECTION_TYPES = 3;

    /**
     * Provides an arbitrary for generating random HighType instances.
     * Uses default maximum depth of 3.
     * 
     * @return an Arbitrary that generates HighType instances
     */
    @Provide
    public static Arbitrary<HighType> highTypes() {
        return highTypes(DEFAULT_MAX_DEPTH);
    }

    /**
     * Provides an arbitrary for generating random HighType instances with specified max depth.
     * 
     * @param maxDepth the maximum recursion depth for nested types
     * @return an Arbitrary that generates HighType instances
     */
    public static Arbitrary<HighType> highTypes(int maxDepth) {
        return highTypesInternal(maxDepth);
    }

    private static Arbitrary<HighType> highTypesInternal(int depth) {
        if (depth <= 0) {
            // At max depth, only generate non-recursive types
            return primitiveOrUserDefinedTypes();
        }

        return Arbitraries.frequencyOf(
            // Primitive types (higher frequency)
            Tuple.of(5, primitiveTypes()),
            // User-defined types
            Tuple.of(3, userDefinedTypes()),
            // Recursive types (lower frequency to control size)
            Tuple.of(2, appliedTypes(depth - 1)),
            Tuple.of(2, pureTypes(depth - 1)),
            Tuple.of(1, intersectionTypes(depth - 1))
        );
    }

    /**
     * Generates only primitive types (TVoid, TBool, TInt, TFloat64).
     */
    @Provide
    public static Arbitrary<HighType> primitiveTypes() {
        return Arbitraries.of(
            HighType.tVoid(),
            HighType.tBool(),
            HighType.tInt(),
            HighType.tFloat64()
        );
    }

    /**
     * Generates user-defined types with random names.
     */
    @Provide
    public static Arbitrary<HighType> userDefinedTypes() {
        return typeNames().map(HighType::userDefined);
    }

    /**
     * Generates primitive or user-defined types (non-recursive).
     */
    public static Arbitrary<HighType> primitiveOrUserDefinedTypes() {
        return Arbitraries.frequencyOf(
            Tuple.of(4, primitiveTypes()),
            Tuple.of(2, userDefinedTypes())
        );
    }

    /**
     * Generates Applied types with a base type and type arguments.
     */
    private static Arbitrary<HighType> appliedTypes(int depth) {
        return Combinators.combine(
            highTypesInternal(depth),
            highTypesInternal(depth).list().ofMinSize(1).ofMaxSize(MAX_TYPE_ARGS)
        ).as(HighType::applied);
    }

    /**
     * Generates Pure type wrappers.
     */
    private static Arbitrary<HighType> pureTypes(int depth) {
        return highTypesInternal(depth).map(HighType::pure);
    }

    /**
     * Generates Intersection types.
     */
    private static Arbitrary<HighType> intersectionTypes(int depth) {
        return highTypesInternal(depth)
            .list()
            .ofMinSize(2)
            .ofMaxSize(MAX_INTERSECTION_TYPES)
            .map(HighType::intersection);
    }

    /**
     * Generates valid type names (identifiers).
     */
    @Provide
    public static Arbitrary<String> typeNames() {
        return Arbitraries.frequencyOf(
            // Common type names
            Tuple.of(3, Arbitraries.of(
                "Object", "String", "List", "Map", "Set", "Array",
                "MyClass", "Point", "Person", "Node", "Tree"
            )),
            // Generated names
            Tuple.of(1, Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(1)
                .ofMaxLength(1)
                .flatMap(first -> Arbitraries.strings()
                    .withCharRange('a', 'z')
                    .ofMinLength(0)
                    .ofMaxLength(10)
                    .map(rest -> first + rest)))
        );
    }

    /**
     * Generates valid identifier names (for variables, fields, etc.).
     */
    @Provide
    public static Arbitrary<String> identifierNames() {
        return Arbitraries.frequencyOf(
            // Common identifier names
            Tuple.of(3, Arbitraries.of(
                "x", "y", "z", "i", "j", "n", "m",
                "value", "result", "temp", "count", "index",
                "left", "right", "head", "tail", "data"
            )),
            // Generated names
            Tuple.of(1, Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(12))
        );
    }
}
