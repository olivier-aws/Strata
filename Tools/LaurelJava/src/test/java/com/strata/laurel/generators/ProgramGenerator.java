/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.generators;

import com.strata.laurel.ast.*;
import net.jqwik.api.*;

import java.util.List;
import java.util.Optional;

/**
 * jqwik generator for creating random {@link Program} instances.
 * 
 * <p>Composes type, expression, and procedure generators to create complete programs.
 */
public class ProgramGenerator {

    private static final int DEFAULT_MAX_DEPTH = 3;
    private static final int MAX_PROCEDURES = 5;
    private static final int MAX_FIELDS = 5;
    private static final int MAX_TYPES = 3;
    private static final int MAX_PARAMS = 4;
    private static final int MAX_INSTANCE_PROCS = 3;
    private static final int MAX_EXTENDING = 2;

    /**
     * Provides an arbitrary for generating random Program instances.
     * 
     * @return an Arbitrary that generates Program instances
     */
    @Provide
    public static Arbitrary<Program> programs() {
        return programs(DEFAULT_MAX_DEPTH);
    }

    /**
     * Provides an arbitrary for generating random Program instances with specified max depth.
     * 
     * @param maxDepth the maximum recursion depth for nested structures
     * @return an Arbitrary that generates Program instances
     */
    public static Arbitrary<Program> programs(int maxDepth) {
        return Combinators.combine(
            procedures(maxDepth).list().ofMaxSize(MAX_PROCEDURES),
            fields(maxDepth).list().ofMaxSize(MAX_FIELDS),
            typeDefinitions(maxDepth).list().ofMaxSize(MAX_TYPES)
        ).as(Program::new);
    }

    // ========== Procedure Generation ==========

    /**
     * Generates random Procedure instances.
     */
    @Provide
    public static Arbitrary<Procedure> procedures() {
        return procedures(DEFAULT_MAX_DEPTH);
    }

    /**
     * Generates random Procedure instances with specified max depth.
     */
    public static Arbitrary<Procedure> procedures(int maxDepth) {
        // jqwik Combinators.combine supports up to 8 arguments, so we need to split
        // First combine: name, inputs, output, precondition, decreases
        Arbitrary<ProcedurePartA> partA = Combinators.combine(
            HighTypeGenerator.identifierNames(),
            parameters(maxDepth).list().ofMaxSize(MAX_PARAMS),
            HighTypeGenerator.highTypes(maxDepth),
            StmtExprGenerator.stmtExprs(maxDepth),  // precondition
            StmtExprGenerator.stmtExprs(maxDepth)   // decreases
        ).as(ProcedurePartA::new);

        // Second combine: deterministic, reads, modifies, body
        Arbitrary<ProcedurePartB> partB = Combinators.combine(
            Arbitraries.of(true, false),            // deterministic
            StmtExprGenerator.stmtExprs(maxDepth).optional(),  // reads
            StmtExprGenerator.stmtExprs(maxDepth),  // modifies
            bodies(maxDepth)
        ).as(ProcedurePartB::new);

        // Combine both parts
        return Combinators.combine(partA, partB).as((a, b) ->
            new Procedure(a.name, a.inputs, a.output, a.precondition, a.decreases,
                         b.deterministic, b.reads, b.modifies, b.body));
    }

    // Helper records for splitting Procedure generation
    private record ProcedurePartA(
        String name,
        List<Parameter> inputs,
        HighType output,
        StmtExpr precondition,
        StmtExpr decreases
    ) {}

    private record ProcedurePartB(
        boolean deterministic,
        Optional<StmtExpr> reads,
        StmtExpr modifies,
        Body body
    ) {}

    /**
     * Generates random Parameter instances.
     */
    @Provide
    public static Arbitrary<Parameter> parameters() {
        return parameters(DEFAULT_MAX_DEPTH);
    }

    /**
     * Generates random Parameter instances with specified max depth.
     */
    public static Arbitrary<Parameter> parameters(int maxDepth) {
        return Combinators.combine(
            HighTypeGenerator.identifierNames(),
            HighTypeGenerator.highTypes(maxDepth)
        ).as(Parameter::new);
    }

    /**
     * Generates random Body instances.
     */
    @Provide
    public static Arbitrary<Body> bodies() {
        return bodies(DEFAULT_MAX_DEPTH);
    }

    /**
     * Generates random Body instances with specified max depth.
     */
    public static Arbitrary<Body> bodies(int maxDepth) {
        return Arbitraries.frequencyOf(
            Tuple.of(3, transparentBodies(maxDepth)),
            Tuple.of(2, opaqueBodies(maxDepth)),
            Tuple.of(1, abstractBodies(maxDepth))
        );
    }

    private static Arbitrary<Body> transparentBodies(int maxDepth) {
        return StmtExprGenerator.stmtExprs(maxDepth).map(Body::transparent);
    }

    private static Arbitrary<Body> opaqueBodies(int maxDepth) {
        return Combinators.combine(
            StmtExprGenerator.stmtExprs(maxDepth),
            StmtExprGenerator.stmtExprs(maxDepth).optional()
        ).as(Body.Opaque::new);
    }

    private static Arbitrary<Body> abstractBodies(int maxDepth) {
        return StmtExprGenerator.stmtExprs(maxDepth).map(Body::abstractBody);
    }

    // ========== Field Generation ==========

    /**
     * Generates random Field instances.
     */
    @Provide
    public static Arbitrary<Field> fields() {
        return fields(DEFAULT_MAX_DEPTH);
    }

    /**
     * Generates random Field instances with specified max depth.
     */
    public static Arbitrary<Field> fields(int maxDepth) {
        return Combinators.combine(
            HighTypeGenerator.identifierNames(),
            Arbitraries.of(true, false),
            HighTypeGenerator.highTypes(maxDepth)
        ).as(Field::new);
    }

    // ========== Type Definition Generation ==========

    /**
     * Generates random TypeDefinition instances.
     */
    @Provide
    public static Arbitrary<TypeDefinition> typeDefinitions() {
        return typeDefinitions(DEFAULT_MAX_DEPTH);
    }

    /**
     * Generates random TypeDefinition instances with specified max depth.
     */
    public static Arbitrary<TypeDefinition> typeDefinitions(int maxDepth) {
        return Arbitraries.frequencyOf(
            Tuple.of(2, compositeTypeDefinitions(maxDepth)),
            Tuple.of(1, constrainedTypeDefinitions(maxDepth))
        );
    }

    private static Arbitrary<TypeDefinition> compositeTypeDefinitions(int maxDepth) {
        return compositeTypes(maxDepth).map(TypeDefinition::composite);
    }

    private static Arbitrary<TypeDefinition> constrainedTypeDefinitions(int maxDepth) {
        return constrainedTypes(maxDepth).map(TypeDefinition::constrained);
    }

    /**
     * Generates random CompositeType instances.
     */
    @Provide
    public static Arbitrary<CompositeType> compositeTypes() {
        return compositeTypes(DEFAULT_MAX_DEPTH);
    }

    /**
     * Generates random CompositeType instances with specified max depth.
     */
    public static Arbitrary<CompositeType> compositeTypes(int maxDepth) {
        return Combinators.combine(
            HighTypeGenerator.typeNames(),
            HighTypeGenerator.typeNames().list().ofMaxSize(MAX_EXTENDING),
            fields(maxDepth).list().ofMaxSize(MAX_FIELDS),
            procedures(maxDepth).list().ofMaxSize(MAX_INSTANCE_PROCS)
        ).as(CompositeType::new);
    }

    /**
     * Generates random ConstrainedType instances.
     */
    @Provide
    public static Arbitrary<ConstrainedType> constrainedTypes() {
        return constrainedTypes(DEFAULT_MAX_DEPTH);
    }

    /**
     * Generates random ConstrainedType instances with specified max depth.
     */
    public static Arbitrary<ConstrainedType> constrainedTypes(int maxDepth) {
        return Combinators.combine(
            HighTypeGenerator.typeNames(),
            HighTypeGenerator.highTypes(maxDepth),
            HighTypeGenerator.identifierNames(),
            StmtExprGenerator.stmtExprs(maxDepth),  // constraint
            StmtExprGenerator.stmtExprs(maxDepth)   // witness
        ).as(ConstrainedType::new);
    }

    // ========== Convenience Methods for Smaller Programs ==========

    /**
     * Generates small programs with limited size for faster testing.
     */
    @Provide
    public static Arbitrary<Program> smallPrograms() {
        return Combinators.combine(
            procedures(2).list().ofMaxSize(2),
            fields(2).list().ofMaxSize(2),
            typeDefinitions(2).list().ofMaxSize(1)
        ).as(Program::new);
    }

    /**
     * Generates programs with only procedures (no fields or types).
     */
    @Provide
    public static Arbitrary<Program> procedureOnlyPrograms() {
        return procedures(DEFAULT_MAX_DEPTH)
            .list()
            .ofMinSize(1)
            .ofMaxSize(MAX_PROCEDURES)
            .map(procs -> new Program(procs, List.of(), List.of()));
    }

    /**
     * Generates empty programs.
     */
    @Provide
    public static Arbitrary<Program> emptyPrograms() {
        return Arbitraries.just(Program.empty());
    }
}
