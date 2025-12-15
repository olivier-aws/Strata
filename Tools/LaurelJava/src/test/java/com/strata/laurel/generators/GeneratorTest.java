/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.generators;

import com.strata.laurel.ast.*;
import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the jqwik generators to verify they produce valid AST instances.
 */
class GeneratorTest {

    @Property(tries = 100)
    void highTypeGeneratorProducesNonNullTypes(@ForAll("highTypes") HighType type) {
        assertNotNull(type, "Generated HighType should not be null");
    }

    @Property(tries = 100)
    void stmtExprGeneratorProducesNonNullExpressions(@ForAll("stmtExprs") StmtExpr expr) {
        assertNotNull(expr, "Generated StmtExpr should not be null");
    }

    @Property(tries = 50)
    void programGeneratorProducesNonNullPrograms(@ForAll("programs") Program program) {
        assertNotNull(program, "Generated Program should not be null");
        assertNotNull(program.staticProcedures(), "staticProcedures should not be null");
        assertNotNull(program.staticFields(), "staticFields should not be null");
        assertNotNull(program.types(), "types should not be null");
    }

    @Property(tries = 50)
    void procedureGeneratorProducesValidProcedures(@ForAll("procedures") Procedure proc) {
        assertNotNull(proc, "Generated Procedure should not be null");
        assertNotNull(proc.name(), "Procedure name should not be null");
        assertNotNull(proc.inputs(), "Procedure inputs should not be null");
        assertNotNull(proc.output(), "Procedure output should not be null");
        assertNotNull(proc.body(), "Procedure body should not be null");
    }

    @Property(tries = 100)
    void primitiveTypesAreGenerated(@ForAll("primitiveTypes") HighType type) {
        assertTrue(
            type instanceof HighType.TVoid ||
            type instanceof HighType.TBool ||
            type instanceof HighType.TInt ||
            type instanceof HighType.TFloat64,
            "Should be a primitive type"
        );
    }

    @Property(tries = 100)
    void leafExpressionsAreGenerated(@ForAll("leafExpressions") StmtExpr expr) {
        assertTrue(
            expr instanceof StmtExpr.LiteralInt ||
            expr instanceof StmtExpr.LiteralBool ||
            expr instanceof StmtExpr.Identifier ||
            expr instanceof StmtExpr.This ||
            expr instanceof StmtExpr.Abstract ||
            expr instanceof StmtExpr.All ||
            expr instanceof StmtExpr.Hole,
            "Should be a leaf expression"
        );
    }

    @Property(tries = 50)
    void compositeTypesHaveValidStructure(@ForAll("compositeTypes") CompositeType type) {
        assertNotNull(type.name(), "CompositeType name should not be null");
        assertNotNull(type.extending(), "extending list should not be null");
        assertNotNull(type.fields(), "fields list should not be null");
        assertNotNull(type.instanceProcedures(), "instanceProcedures list should not be null");
    }

    @Property(tries = 50)
    void constrainedTypesHaveValidStructure(@ForAll("constrainedTypes") ConstrainedType type) {
        assertNotNull(type.name(), "ConstrainedType name should not be null");
        assertNotNull(type.base(), "base type should not be null");
        assertNotNull(type.valueName(), "valueName should not be null");
        assertNotNull(type.constraint(), "constraint should not be null");
        assertNotNull(type.witness(), "witness should not be null");
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<HighType> highTypes() {
        return HighTypeGenerator.highTypes();
    }

    @Provide
    Arbitrary<HighType> primitiveTypes() {
        return HighTypeGenerator.primitiveTypes();
    }

    @Provide
    Arbitrary<StmtExpr> stmtExprs() {
        return StmtExprGenerator.stmtExprs();
    }

    @Provide
    Arbitrary<StmtExpr> leafExpressions() {
        return StmtExprGenerator.leafExpressions();
    }

    @Provide
    Arbitrary<Program> programs() {
        return ProgramGenerator.programs();
    }

    @Provide
    Arbitrary<Procedure> procedures() {
        return ProgramGenerator.procedures();
    }

    @Provide
    Arbitrary<CompositeType> compositeTypes() {
        return ProgramGenerator.compositeTypes();
    }

    @Provide
    Arbitrary<ConstrainedType> constrainedTypes() {
        return ProgramGenerator.constrainedTypes();
    }
}
