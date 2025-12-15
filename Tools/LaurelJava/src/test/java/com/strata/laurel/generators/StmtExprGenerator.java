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
 * jqwik generator for creating random {@link StmtExpr} instances.
 * 
 * <p>Handles recursive expressions with bounded depth to prevent infinite recursion.
 */
public class StmtExprGenerator {

    private static final int DEFAULT_MAX_DEPTH = 4;
    private static final int MAX_BLOCK_SIZE = 5;
    private static final int MAX_ARGS = 4;

    /**
     * Provides an arbitrary for generating random StmtExpr instances.
     * Uses default maximum depth of 4.
     * 
     * @return an Arbitrary that generates StmtExpr instances
     */
    @Provide
    public static Arbitrary<StmtExpr> stmtExprs() {
        return stmtExprs(DEFAULT_MAX_DEPTH);
    }

    /**
     * Provides an arbitrary for generating random StmtExpr instances with specified max depth.
     * 
     * @param maxDepth the maximum recursion depth for nested expressions
     * @return an Arbitrary that generates StmtExpr instances
     */
    public static Arbitrary<StmtExpr> stmtExprs(int maxDepth) {
        return stmtExprsInternal(maxDepth);
    }

    private static Arbitrary<StmtExpr> stmtExprsInternal(int depth) {
        if (depth <= 0) {
            // At max depth, only generate leaf nodes
            return leafExpressions();
        }

        return Arbitraries.frequencyOf(
            // Leaf expressions (higher frequency)
            Tuple.of(6, leafExpressions()),
            // Simple recursive expressions
            Tuple.of(3, simpleRecursiveExpressions(depth - 1)),
            // Statement-like constructs (lower frequency)
            Tuple.of(2, statementLikeExpressions(depth - 1)),
            // Verification constructs
            Tuple.of(1, verificationExpressions(depth - 1))
        );
    }

    /**
     * Generates leaf expressions (non-recursive).
     */
    @Provide
    public static Arbitrary<StmtExpr> leafExpressions() {
        return Arbitraries.frequencyOf(
            Tuple.of(3, literalInts()),
            Tuple.of(3, literalBools()),
            Tuple.of(4, identifiers()),
            Tuple.of(1, Arbitraries.just(StmtExpr.thisRef())),
            Tuple.of(1, Arbitraries.just(StmtExpr.abstractMarker())),
            Tuple.of(1, Arbitraries.just(StmtExpr.all())),
            Tuple.of(1, Arbitraries.just(StmtExpr.hole()))
        );
    }

    /**
     * Generates integer literals.
     */
    @Provide
    public static Arbitrary<StmtExpr> literalInts() {
        return Arbitraries.longs().between(-1000, 1000).map(StmtExpr::literalInt);
    }

    /**
     * Generates boolean literals.
     */
    @Provide
    public static Arbitrary<StmtExpr> literalBools() {
        return Arbitraries.of(true, false).map(StmtExpr::literalBool);
    }

    /**
     * Generates identifier expressions.
     */
    @Provide
    public static Arbitrary<StmtExpr> identifiers() {
        return HighTypeGenerator.identifierNames().map(StmtExpr::identifier);
    }

    /**
     * Generates simple recursive expressions (binary ops, field access, etc.).
     */
    private static Arbitrary<StmtExpr> simpleRecursiveExpressions(int depth) {
        return Arbitraries.frequencyOf(
            Tuple.of(4, primitiveOps(depth)),
            Tuple.of(2, assigns(depth)),
            Tuple.of(2, fieldSelects(depth)),
            Tuple.of(1, pureFieldUpdates(depth)),
            Tuple.of(2, staticCalls(depth)),
            Tuple.of(1, instanceCalls(depth)),
            Tuple.of(1, referenceEquals(depth)),
            Tuple.of(1, asTypes(depth)),
            Tuple.of(1, isTypes(depth))
        );
    }

    /**
     * Generates statement-like expressions (if, while, block, etc.).
     */
    private static Arbitrary<StmtExpr> statementLikeExpressions(int depth) {
        return Arbitraries.frequencyOf(
            Tuple.of(3, ifThenElses(depth)),
            Tuple.of(2, blocks(depth)),
            Tuple.of(2, localVariables(depth)),
            Tuple.of(1, whileLoops(depth)),
            Tuple.of(2, returns(depth)),
            Tuple.of(1, exits())
        );
    }

    /**
     * Generates verification-related expressions.
     */
    private static Arbitrary<StmtExpr> verificationExpressions(int depth) {
        return Arbitraries.frequencyOf(
            Tuple.of(2, foralls(depth)),
            Tuple.of(2, exists(depth)),
            Tuple.of(2, asserts(depth)),
            Tuple.of(2, assumes(depth)),
            Tuple.of(1, olds(depth)),
            Tuple.of(1, freshs(depth)),
            Tuple.of(1, assigneds(depth)),
            Tuple.of(1, proveBys(depth)),
            Tuple.of(1, contractOfs(depth))
        );
    }

    // ========== Primitive Operations ==========

    private static Arbitrary<StmtExpr> primitiveOps(int depth) {
        return Combinators.combine(
            operations(),
            stmtExprsInternal(depth).list().ofMinSize(1).ofMaxSize(2)
        ).as(StmtExpr::primitiveOp);
    }

    @Provide
    public static Arbitrary<Operation> operations() {
        return Arbitraries.of(Operation.values());
    }

    // ========== Assignment and Field Access ==========

    private static Arbitrary<StmtExpr> assigns(int depth) {
        return Combinators.combine(
            stmtExprsInternal(depth),
            stmtExprsInternal(depth)
        ).as(StmtExpr::assign);
    }

    private static Arbitrary<StmtExpr> fieldSelects(int depth) {
        return Combinators.combine(
            stmtExprsInternal(depth),
            HighTypeGenerator.identifierNames()
        ).as(StmtExpr::fieldSelect);
    }

    private static Arbitrary<StmtExpr> pureFieldUpdates(int depth) {
        return Combinators.combine(
            stmtExprsInternal(depth),
            HighTypeGenerator.identifierNames(),
            stmtExprsInternal(depth)
        ).as(StmtExpr::pureFieldUpdate);
    }

    // ========== Calls ==========

    private static Arbitrary<StmtExpr> staticCalls(int depth) {
        return Combinators.combine(
            HighTypeGenerator.identifierNames(),
            stmtExprsInternal(depth).list().ofMaxSize(MAX_ARGS)
        ).as(StmtExpr::staticCall);
    }

    private static Arbitrary<StmtExpr> instanceCalls(int depth) {
        return Combinators.combine(
            stmtExprsInternal(depth),
            HighTypeGenerator.identifierNames(),
            stmtExprsInternal(depth).list().ofMaxSize(MAX_ARGS)
        ).as(StmtExpr::instanceCall);
    }

    // ========== Type Operations ==========

    private static Arbitrary<StmtExpr> referenceEquals(int depth) {
        return Combinators.combine(
            stmtExprsInternal(depth),
            stmtExprsInternal(depth)
        ).as(StmtExpr::referenceEquals);
    }

    private static Arbitrary<StmtExpr> asTypes(int depth) {
        return Combinators.combine(
            stmtExprsInternal(depth),
            HighTypeGenerator.highTypes(depth)
        ).as(StmtExpr::asType);
    }

    private static Arbitrary<StmtExpr> isTypes(int depth) {
        return Combinators.combine(
            stmtExprsInternal(depth),
            HighTypeGenerator.highTypes(depth)
        ).as(StmtExpr::isType);
    }

    // ========== Control Flow ==========

    private static Arbitrary<StmtExpr> ifThenElses(int depth) {
        return Combinators.combine(
            stmtExprsInternal(depth),
            stmtExprsInternal(depth),
            stmtExprsInternal(depth).optional()
        ).as(StmtExpr.IfThenElse::new);
    }

    private static Arbitrary<StmtExpr> blocks(int depth) {
        return Combinators.combine(
            stmtExprsInternal(depth).list().ofMaxSize(MAX_BLOCK_SIZE),
            optionalLabels()
        ).as(StmtExpr.Block::new);
    }

    private static Arbitrary<StmtExpr> localVariables(int depth) {
        return Combinators.combine(
            HighTypeGenerator.identifierNames(),
            HighTypeGenerator.highTypes(depth),
            stmtExprsInternal(depth).optional()
        ).as(StmtExpr.LocalVariable::new);
    }

    private static Arbitrary<StmtExpr> whileLoops(int depth) {
        return Combinators.combine(
            stmtExprsInternal(depth),
            stmtExprsInternal(depth).optional(),
            stmtExprsInternal(depth).optional(),
            stmtExprsInternal(depth)
        ).as(StmtExpr.While::new);
    }

    private static Arbitrary<StmtExpr> returns(int depth) {
        return stmtExprsInternal(depth).optional().map(StmtExpr.Return::new);
    }

    private static Arbitrary<StmtExpr> exits() {
        return HighTypeGenerator.identifierNames().map(StmtExpr::exit);
    }

    // ========== Verification Constructs ==========

    private static Arbitrary<StmtExpr> foralls(int depth) {
        return Combinators.combine(
            HighTypeGenerator.identifierNames(),
            HighTypeGenerator.highTypes(depth),
            stmtExprsInternal(depth)
        ).as(StmtExpr::forall);
    }

    private static Arbitrary<StmtExpr> exists(int depth) {
        return Combinators.combine(
            HighTypeGenerator.identifierNames(),
            HighTypeGenerator.highTypes(depth),
            stmtExprsInternal(depth)
        ).as(StmtExpr::exists);
    }

    private static Arbitrary<StmtExpr> asserts(int depth) {
        return stmtExprsInternal(depth).map(StmtExpr::assertion);
    }

    private static Arbitrary<StmtExpr> assumes(int depth) {
        return stmtExprsInternal(depth).map(StmtExpr::assume);
    }

    private static Arbitrary<StmtExpr> olds(int depth) {
        return stmtExprsInternal(depth).map(StmtExpr::old);
    }

    private static Arbitrary<StmtExpr> freshs(int depth) {
        return stmtExprsInternal(depth).map(StmtExpr::fresh);
    }

    private static Arbitrary<StmtExpr> assigneds(int depth) {
        return stmtExprsInternal(depth).map(StmtExpr::assigned);
    }

    private static Arbitrary<StmtExpr> proveBys(int depth) {
        return Combinators.combine(
            stmtExprsInternal(depth),
            stmtExprsInternal(depth)
        ).as(StmtExpr::proveBy);
    }

    private static Arbitrary<StmtExpr> contractOfs(int depth) {
        return Combinators.combine(
            contractTypes(),
            stmtExprsInternal(depth)
        ).as(StmtExpr::contractOf);
    }

    @Provide
    public static Arbitrary<ContractType> contractTypes() {
        return Arbitraries.of(ContractType.values());
    }

    // ========== Helpers ==========

    private static Arbitrary<Optional<String>> optionalLabels() {
        return Arbitraries.frequencyOf(
            Tuple.of(3, Arbitraries.just(Optional.<String>empty())),
            Tuple.of(1, HighTypeGenerator.identifierNames().map(Optional::of))
        );
    }
}
