/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ast;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A unified construct representing both statements and expressions in Laurel.
 * 
 * <p>By using a single datatype, we prevent duplication of constructs that can be used
 * in both contexts, such as conditionals and variable declarations.
 * 
 * <p>This sealed interface hierarchy mirrors the Lean StmtExpr inductive type.
 */
public sealed interface StmtExpr permits
        // Statement-like
        StmtExpr.IfThenElse,
        StmtExpr.Block,
        StmtExpr.LocalVariable,
        StmtExpr.While,
        StmtExpr.Exit,
        StmtExpr.Return,
        // Expression-like
        StmtExpr.LiteralInt,
        StmtExpr.LiteralBool,
        StmtExpr.Identifier,
        StmtExpr.Assign,
        StmtExpr.FieldSelect,
        StmtExpr.PureFieldUpdate,
        StmtExpr.StaticCall,
        StmtExpr.PrimitiveOp,
        // Instance-related
        StmtExpr.This,
        StmtExpr.ReferenceEquals,
        StmtExpr.AsType,
        StmtExpr.IsType,
        StmtExpr.InstanceCall,
        // Verification-specific
        StmtExpr.Forall,
        StmtExpr.Exists,
        StmtExpr.Assigned,
        StmtExpr.Old,
        StmtExpr.Fresh,
        // Proof-related
        StmtExpr.Assert,
        StmtExpr.Assume,
        StmtExpr.ProveBy,
        StmtExpr.ContractOf,
        StmtExpr.Abstract,
        StmtExpr.All,
        StmtExpr.Hole {

    // ========== Statement-like Constructs ==========

    /**
     * Conditional statement/expression.
     * @param cond the condition
     * @param thenBranch the then branch
     * @param elseBranch the optional else branch
     */
    record IfThenElse(
            StmtExpr cond,
            StmtExpr thenBranch,
            Optional<StmtExpr> elseBranch
    ) implements StmtExpr {
        public IfThenElse {
            Objects.requireNonNull(cond, "cond must not be null");
            Objects.requireNonNull(thenBranch, "thenBranch must not be null");
            Objects.requireNonNull(elseBranch, "elseBranch must not be null");
        }
    }


    /**
     * A block of statements with an optional label.
     * @param statements the list of statements in the block
     * @param label optional label for the block (used with Exit)
     */
    record Block(
            List<StmtExpr> statements,
            Optional<String> label
    ) implements StmtExpr {
        public Block {
            Objects.requireNonNull(statements, "statements must not be null");
            Objects.requireNonNull(label, "label must not be null");
            statements = List.copyOf(statements);
        }
    }

    /**
     * Local variable declaration.
     * <p>The initializer must be set if this StmtExpr is in a pure context.
     * @param name the variable name
     * @param type the variable type
     * @param initializer optional initial value
     */
    record LocalVariable(
            String name,
            HighType type,
            Optional<StmtExpr> initializer
    ) implements StmtExpr {
        public LocalVariable {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(initializer, "initializer must not be null");
        }
    }

    /**
     * While loop.
     * <p>While is only allowed in an impure context. The invariant and decreases are always pure.
     * @param cond the loop condition
     * @param invariant optional loop invariant
     * @param decreases optional decreases clause for termination
     * @param body the loop body
     */
    record While(
            StmtExpr cond,
            Optional<StmtExpr> invariant,
            Optional<StmtExpr> decreases,
            StmtExpr body
    ) implements StmtExpr {
        public While {
            Objects.requireNonNull(cond, "cond must not be null");
            Objects.requireNonNull(invariant, "invariant must not be null");
            Objects.requireNonNull(decreases, "decreases must not be null");
            Objects.requireNonNull(body, "body must not be null");
        }
    }

    /**
     * Exit from a labeled block.
     * @param target the label of the block to exit
     */
    record Exit(String target) implements StmtExpr {
        public Exit {
            Objects.requireNonNull(target, "target must not be null");
        }
    }

    /**
     * Return statement.
     * @param value optional return value
     */
    record Return(Optional<StmtExpr> value) implements StmtExpr {
        public Return {
            Objects.requireNonNull(value, "value must not be null");
        }
    }

    // ========== Expression-like Constructs ==========

    /**
     * Integer literal.
     * @param value the integer value
     */
    record LiteralInt(long value) implements StmtExpr {}

    /**
     * Boolean literal.
     * @param value the boolean value
     */
    record LiteralBool(boolean value) implements StmtExpr {}

    /**
     * Variable or name reference.
     * @param name the identifier name
     */
    record Identifier(String name) implements StmtExpr {
        public Identifier {
            Objects.requireNonNull(name, "name must not be null");
        }
    }

    /**
     * Assignment statement.
     * <p>Assign is only allowed in an impure context.
     * @param target the assignment target
     * @param value the value to assign
     */
    record Assign(StmtExpr target, StmtExpr value) implements StmtExpr {
        public Assign {
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(value, "value must not be null");
        }
    }

    /**
     * Field access expression.
     * <p>Used by itself for field reads and in combination with Assign for field writes.
     * @param target the object to access
     * @param fieldName the field name
     */
    record FieldSelect(StmtExpr target, String fieldName) implements StmtExpr {
        public FieldSelect {
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(fieldName, "fieldName must not be null");
        }
    }

    /**
     * Pure field update expression.
     * <p>This is the only way to assign values to fields of pure types.
     * @param target the object to update
     * @param fieldName the field name
     * @param newValue the new value for the field
     */
    record PureFieldUpdate(
            StmtExpr target,
            String fieldName,
            StmtExpr newValue
    ) implements StmtExpr {
        public PureFieldUpdate {
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(fieldName, "fieldName must not be null");
            Objects.requireNonNull(newValue, "newValue must not be null");
        }
    }

    /**
     * Static procedure call.
     * @param callee the procedure name
     * @param arguments the call arguments
     */
    record StaticCall(String callee, List<StmtExpr> arguments) implements StmtExpr {
        public StaticCall {
            Objects.requireNonNull(callee, "callee must not be null");
            Objects.requireNonNull(arguments, "arguments must not be null");
            arguments = List.copyOf(arguments);
        }
    }

    /**
     * Primitive operation application.
     * @param operator the operation
     * @param arguments the operands
     */
    record PrimitiveOp(Operation operator, List<StmtExpr> arguments) implements StmtExpr {
        public PrimitiveOp {
            Objects.requireNonNull(operator, "operator must not be null");
            Objects.requireNonNull(arguments, "arguments must not be null");
            arguments = List.copyOf(arguments);
        }
    }


    // ========== Instance-related Constructs ==========

    /**
     * Reference to the current instance (this).
     */
    record This() implements StmtExpr {}

    /**
     * Reference equality comparison.
     * @param lhs the left-hand side
     * @param rhs the right-hand side
     */
    record ReferenceEquals(StmtExpr lhs, StmtExpr rhs) implements StmtExpr {
        public ReferenceEquals {
            Objects.requireNonNull(lhs, "lhs must not be null");
            Objects.requireNonNull(rhs, "rhs must not be null");
        }
    }

    /**
     * Type cast expression.
     * @param target the expression to cast
     * @param targetType the target type
     */
    record AsType(StmtExpr target, HighType targetType) implements StmtExpr {
        public AsType {
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(targetType, "targetType must not be null");
        }
    }

    /**
     * Type check expression.
     * @param target the expression to check
     * @param type the type to check against
     */
    record IsType(StmtExpr target, HighType type) implements StmtExpr {
        public IsType {
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(type, "type must not be null");
        }
    }

    /**
     * Instance method call.
     * @param target the object to call the method on
     * @param callee the method name
     * @param arguments the call arguments
     */
    record InstanceCall(
            StmtExpr target,
            String callee,
            List<StmtExpr> arguments
    ) implements StmtExpr {
        public InstanceCall {
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(callee, "callee must not be null");
            Objects.requireNonNull(arguments, "arguments must not be null");
            arguments = List.copyOf(arguments);
        }
    }

    // ========== Verification-specific Constructs ==========

    /**
     * Universal quantification.
     * @param name the bound variable name
     * @param type the bound variable type
     * @param body the quantified expression
     */
    record Forall(String name, HighType type, StmtExpr body) implements StmtExpr {
        public Forall {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(body, "body must not be null");
        }
    }

    /**
     * Existential quantification.
     * @param name the bound variable name
     * @param type the bound variable type
     * @param body the quantified expression
     */
    record Exists(String name, HighType type, StmtExpr body) implements StmtExpr {
        public Exists {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(body, "body must not be null");
        }
    }

    /**
     * Check if a variable has been assigned.
     * @param name the expression to check
     */
    record Assigned(StmtExpr name) implements StmtExpr {
        public Assigned {
            Objects.requireNonNull(name, "name must not be null");
        }
    }

    /**
     * Reference to the old (pre-state) value of an expression.
     * @param value the expression
     */
    record Old(StmtExpr value) implements StmtExpr {
        public Old {
            Objects.requireNonNull(value, "value must not be null");
        }
    }

    /**
     * Check if a reference is fresh (newly allocated).
     * <p>Fresh may only target impure composite types.
     * @param value the expression to check
     */
    record Fresh(StmtExpr value) implements StmtExpr {
        public Fresh {
            Objects.requireNonNull(value, "value must not be null");
        }
    }

    // ========== Proof-related Constructs ==========

    /**
     * Assertion statement.
     * @param condition the condition to assert
     */
    record Assert(StmtExpr condition) implements StmtExpr {
        public Assert {
            Objects.requireNonNull(condition, "condition must not be null");
        }
    }

    /**
     * Assumption statement.
     * @param condition the condition to assume
     */
    record Assume(StmtExpr condition) implements StmtExpr {
        public Assume {
            Objects.requireNonNull(condition, "condition must not be null");
        }
    }

    /**
     * Proof annotation.
     * <p>ProveBy allows writing proof trees. Its semantics are the same as that of the given value,
     * but the proof is used to help prove any assertions in value.
     * @param value the expression to prove
     * @param proof the proof expression
     */
    record ProveBy(StmtExpr value, StmtExpr proof) implements StmtExpr {
        public ProveBy {
            Objects.requireNonNull(value, "value must not be null");
            Objects.requireNonNull(proof, "proof must not be null");
        }
    }

    /**
     * Extract a contract from a function.
     * @param type the type of contract to extract
     * @param function the function expression
     */
    record ContractOf(ContractType type, StmtExpr function) implements StmtExpr {
        public ContractOf {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(function, "function must not be null");
        }
    }

    /**
     * Abstract marker.
     * <p>Can be used as the root expr in a contract for reads/modifies/precondition/postcondition.
     * It can only be used for instance procedures and makes the containing type abstract.
     */
    record Abstract() implements StmtExpr {}

    /**
     * All marker.
     * <p>Refers to all objects in the heap. Can be used in a reads or modifies clause.
     */
    record All() implements StmtExpr {}

    /**
     * Hole marker.
     * <p>Has a dynamic type and is useful when programs are only partially available.
     */
    record Hole() implements StmtExpr {}


    // ========== Factory Methods ==========

    // --- Statement-like ---

    static IfThenElse ifThenElse(StmtExpr cond, StmtExpr thenBranch, StmtExpr elseBranch) {
        return new IfThenElse(cond, thenBranch, Optional.ofNullable(elseBranch));
    }

    static IfThenElse ifThen(StmtExpr cond, StmtExpr thenBranch) {
        return new IfThenElse(cond, thenBranch, Optional.empty());
    }

    static Block block(List<StmtExpr> statements) {
        return new Block(statements, Optional.empty());
    }

    static Block block(StmtExpr... statements) {
        return new Block(List.of(statements), Optional.empty());
    }

    static Block labeledBlock(String label, List<StmtExpr> statements) {
        return new Block(statements, Optional.of(label));
    }

    static Block labeledBlock(String label, StmtExpr... statements) {
        return new Block(List.of(statements), Optional.of(label));
    }

    static LocalVariable localVariable(String name, HighType type) {
        return new LocalVariable(name, type, Optional.empty());
    }

    static LocalVariable localVariable(String name, HighType type, StmtExpr initializer) {
        return new LocalVariable(name, type, Optional.of(initializer));
    }

    static While whileLoop(StmtExpr cond, StmtExpr body) {
        return new While(cond, Optional.empty(), Optional.empty(), body);
    }

    static While whileLoop(StmtExpr cond, StmtExpr invariant, StmtExpr decreases, StmtExpr body) {
        return new While(cond, Optional.ofNullable(invariant), Optional.ofNullable(decreases), body);
    }

    static Exit exit(String target) {
        return new Exit(target);
    }

    static Return returnVoid() {
        return new Return(Optional.empty());
    }

    static Return returnValue(StmtExpr value) {
        return new Return(Optional.of(value));
    }

    // --- Expression-like ---

    static LiteralInt literalInt(long value) {
        return new LiteralInt(value);
    }

    static LiteralBool literalBool(boolean value) {
        return new LiteralBool(value);
    }

    static LiteralBool literalTrue() {
        return new LiteralBool(true);
    }

    static LiteralBool literalFalse() {
        return new LiteralBool(false);
    }

    static Identifier identifier(String name) {
        return new Identifier(name);
    }

    static Assign assign(StmtExpr target, StmtExpr value) {
        return new Assign(target, value);
    }

    static FieldSelect fieldSelect(StmtExpr target, String fieldName) {
        return new FieldSelect(target, fieldName);
    }

    static PureFieldUpdate pureFieldUpdate(StmtExpr target, String fieldName, StmtExpr newValue) {
        return new PureFieldUpdate(target, fieldName, newValue);
    }

    static StaticCall staticCall(String callee, List<StmtExpr> arguments) {
        return new StaticCall(callee, arguments);
    }

    static StaticCall staticCall(String callee, StmtExpr... arguments) {
        return new StaticCall(callee, List.of(arguments));
    }

    static PrimitiveOp primitiveOp(Operation operator, List<StmtExpr> arguments) {
        return new PrimitiveOp(operator, arguments);
    }

    static PrimitiveOp primitiveOp(Operation operator, StmtExpr... arguments) {
        return new PrimitiveOp(operator, List.of(arguments));
    }

    // Convenience methods for common operations
    static PrimitiveOp add(StmtExpr left, StmtExpr right) {
        return primitiveOp(Operation.ADD, left, right);
    }

    static PrimitiveOp sub(StmtExpr left, StmtExpr right) {
        return primitiveOp(Operation.SUB, left, right);
    }

    static PrimitiveOp mul(StmtExpr left, StmtExpr right) {
        return primitiveOp(Operation.MUL, left, right);
    }

    static PrimitiveOp div(StmtExpr left, StmtExpr right) {
        return primitiveOp(Operation.DIV, left, right);
    }

    static PrimitiveOp mod(StmtExpr left, StmtExpr right) {
        return primitiveOp(Operation.MOD, left, right);
    }

    static PrimitiveOp neg(StmtExpr operand) {
        return primitiveOp(Operation.NEG, operand);
    }

    static PrimitiveOp eq(StmtExpr left, StmtExpr right) {
        return primitiveOp(Operation.EQ, left, right);
    }

    static PrimitiveOp neq(StmtExpr left, StmtExpr right) {
        return primitiveOp(Operation.NEQ, left, right);
    }

    static PrimitiveOp lt(StmtExpr left, StmtExpr right) {
        return primitiveOp(Operation.LT, left, right);
    }

    static PrimitiveOp leq(StmtExpr left, StmtExpr right) {
        return primitiveOp(Operation.LEQ, left, right);
    }

    static PrimitiveOp gt(StmtExpr left, StmtExpr right) {
        return primitiveOp(Operation.GT, left, right);
    }

    static PrimitiveOp geq(StmtExpr left, StmtExpr right) {
        return primitiveOp(Operation.GEQ, left, right);
    }

    static PrimitiveOp and(StmtExpr left, StmtExpr right) {
        return primitiveOp(Operation.AND, left, right);
    }

    static PrimitiveOp or(StmtExpr left, StmtExpr right) {
        return primitiveOp(Operation.OR, left, right);
    }

    static PrimitiveOp not(StmtExpr operand) {
        return primitiveOp(Operation.NOT, operand);
    }

    // --- Instance-related ---

    static This thisRef() {
        return new This();
    }

    static ReferenceEquals referenceEquals(StmtExpr lhs, StmtExpr rhs) {
        return new ReferenceEquals(lhs, rhs);
    }

    static AsType asType(StmtExpr target, HighType targetType) {
        return new AsType(target, targetType);
    }

    static IsType isType(StmtExpr target, HighType type) {
        return new IsType(target, type);
    }

    static InstanceCall instanceCall(StmtExpr target, String callee, List<StmtExpr> arguments) {
        return new InstanceCall(target, callee, arguments);
    }

    static InstanceCall instanceCall(StmtExpr target, String callee, StmtExpr... arguments) {
        return new InstanceCall(target, callee, List.of(arguments));
    }

    // --- Verification-specific ---

    static Forall forall(String name, HighType type, StmtExpr body) {
        return new Forall(name, type, body);
    }

    static Exists exists(String name, HighType type, StmtExpr body) {
        return new Exists(name, type, body);
    }

    static Assigned assigned(StmtExpr name) {
        return new Assigned(name);
    }

    static Old old(StmtExpr value) {
        return new Old(value);
    }

    static Fresh fresh(StmtExpr value) {
        return new Fresh(value);
    }

    // --- Proof-related ---

    static Assert assertion(StmtExpr condition) {
        return new Assert(condition);
    }

    static Assume assume(StmtExpr condition) {
        return new Assume(condition);
    }

    static ProveBy proveBy(StmtExpr value, StmtExpr proof) {
        return new ProveBy(value, proof);
    }

    static ContractOf contractOf(ContractType type, StmtExpr function) {
        return new ContractOf(type, function);
    }

    static Abstract abstractMarker() {
        return new Abstract();
    }

    static All all() {
        return new All();
    }

    static Hole hole() {
        return new Hole();
    }
}
