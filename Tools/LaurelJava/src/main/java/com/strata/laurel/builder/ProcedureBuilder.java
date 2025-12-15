/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.builder;

import com.strata.laurel.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Fluent builder for constructing {@link Procedure} instances.
 * 
 * <p>This builder provides a convenient API for creating procedures with
 * validation of required fields. Required fields are:
 * <ul>
 *   <li>name - the procedure name</li>
 *   <li>output - the return type</li>
 *   <li>body - the procedure body</li>
 * </ul>
 * 
 * <p>Optional fields have sensible defaults:
 * <ul>
 *   <li>inputs - defaults to empty list</li>
 *   <li>precondition - defaults to {@code LiteralBool(true)}</li>
 *   <li>decreases - defaults to {@code LiteralInt(0)}</li>
 *   <li>deterministic - defaults to {@code true}</li>
 *   <li>reads - defaults to empty</li>
 *   <li>modifies - defaults to {@code All()}</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * Procedure proc = new ProcedureBuilder()
 *     .name("add")
 *     .input("x", HighType.tInt())
 *     .input("y", HighType.tInt())
 *     .output(HighType.tInt())
 *     .precondition(new StmtExpr.LiteralBool(true))
 *     .body(Body.transparent(new StmtExpr.PrimitiveOp(
 *         Operation.ADD,
 *         List.of(new StmtExpr.Identifier("x"), new StmtExpr.Identifier("y"))
 *     )))
 *     .build();
 * }</pre>
 */
public class ProcedureBuilder {
    
    private String name;
    private final List<Parameter> inputs = new ArrayList<>();
    private HighType output;
    private StmtExpr precondition;
    private StmtExpr decreases;
    private Boolean deterministic;
    private StmtExpr reads;
    private StmtExpr modifies;
    private Body body;

    
    /**
     * Creates a new ProcedureBuilder with default values.
     */
    public ProcedureBuilder() {
        // Defaults will be applied in build() if not set
    }
    
    /**
     * Sets the procedure name.
     * 
     * @param name the procedure name (required)
     * @return this builder for chaining
     * @throws NullPointerException if name is null
     */
    public ProcedureBuilder name(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        return this;
    }
    
    /**
     * Adds an input parameter.
     * 
     * @param parameter the parameter to add
     * @return this builder for chaining
     * @throws NullPointerException if parameter is null
     */
    public ProcedureBuilder input(Parameter parameter) {
        Objects.requireNonNull(parameter, "parameter must not be null");
        this.inputs.add(parameter);
        return this;
    }
    
    /**
     * Adds an input parameter with the given name and type.
     * 
     * @param name the parameter name
     * @param type the parameter type
     * @return this builder for chaining
     * @throws NullPointerException if name or type is null
     */
    public ProcedureBuilder input(String name, HighType type) {
        return input(new Parameter(name, type));
    }
    
    /**
     * Sets all input parameters, replacing any previously added.
     * 
     * @param inputs the input parameters
     * @return this builder for chaining
     * @throws NullPointerException if inputs is null
     */
    public ProcedureBuilder inputs(List<Parameter> inputs) {
        Objects.requireNonNull(inputs, "inputs must not be null");
        this.inputs.clear();
        this.inputs.addAll(inputs);
        return this;
    }
    
    /**
     * Sets the output (return) type.
     * 
     * @param output the output type (required)
     * @return this builder for chaining
     * @throws NullPointerException if output is null
     */
    public ProcedureBuilder output(HighType output) {
        this.output = Objects.requireNonNull(output, "output must not be null");
        return this;
    }
    
    /**
     * Sets the precondition.
     * 
     * @param precondition the precondition expression
     * @return this builder for chaining
     * @throws NullPointerException if precondition is null
     */
    public ProcedureBuilder precondition(StmtExpr precondition) {
        this.precondition = Objects.requireNonNull(precondition, "precondition must not be null");
        return this;
    }
    
    /**
     * Sets the decreases clause for termination proofs.
     * 
     * @param decreases the decreases expression
     * @return this builder for chaining
     * @throws NullPointerException if decreases is null
     */
    public ProcedureBuilder decreases(StmtExpr decreases) {
        this.decreases = Objects.requireNonNull(decreases, "decreases must not be null");
        return this;
    }
    
    /**
     * Sets whether the procedure is deterministic.
     * 
     * @param deterministic true if deterministic, false otherwise
     * @return this builder for chaining
     */
    public ProcedureBuilder deterministic(boolean deterministic) {
        this.deterministic = deterministic;
        return this;
    }
    
    /**
     * Sets the reads clause.
     * 
     * @param reads the reads expression
     * @return this builder for chaining
     * @throws NullPointerException if reads is null
     */
    public ProcedureBuilder reads(StmtExpr reads) {
        this.reads = Objects.requireNonNull(reads, "reads must not be null");
        return this;
    }
    
    /**
     * Sets the modifies clause.
     * 
     * @param modifies the modifies expression
     * @return this builder for chaining
     * @throws NullPointerException if modifies is null
     */
    public ProcedureBuilder modifies(StmtExpr modifies) {
        this.modifies = Objects.requireNonNull(modifies, "modifies must not be null");
        return this;
    }
    
    /**
     * Sets the procedure body.
     * 
     * @param body the procedure body (required)
     * @return this builder for chaining
     * @throws NullPointerException if body is null
     */
    public ProcedureBuilder body(Body body) {
        this.body = Objects.requireNonNull(body, "body must not be null");
        return this;
    }
    
    /**
     * Sets a transparent body with the given implementation.
     * 
     * @param implementation the implementation expression
     * @return this builder for chaining
     * @throws NullPointerException if implementation is null
     */
    public ProcedureBuilder transparentBody(StmtExpr implementation) {
        return body(Body.transparent(implementation));
    }
    
    /**
     * Sets an opaque body with the given postcondition.
     * 
     * @param postcondition the postcondition expression
     * @return this builder for chaining
     * @throws NullPointerException if postcondition is null
     */
    public ProcedureBuilder opaqueBody(StmtExpr postcondition) {
        return body(Body.opaque(postcondition));
    }
    
    /**
     * Sets an opaque body with postcondition and implementation.
     * 
     * @param postcondition the postcondition expression
     * @param implementation the hidden implementation
     * @return this builder for chaining
     * @throws NullPointerException if postcondition or implementation is null
     */
    public ProcedureBuilder opaqueBody(StmtExpr postcondition, StmtExpr implementation) {
        return body(Body.opaque(postcondition, implementation));
    }
    
    /**
     * Sets an abstract body with the given postcondition.
     * 
     * @param postcondition the postcondition expression
     * @return this builder for chaining
     * @throws NullPointerException if postcondition is null
     */
    public ProcedureBuilder abstractBody(StmtExpr postcondition) {
        return body(Body.abstractBody(postcondition));
    }
    
    /**
     * Builds the Procedure instance.
     * 
     * @return a new Procedure with the configured values
     * @throws IllegalStateException if required fields (name, output, body) are not set
     */
    public Procedure build() {
        if (name == null) {
            throw new IllegalStateException("name is required");
        }
        if (output == null) {
            throw new IllegalStateException("output is required");
        }
        if (body == null) {
            throw new IllegalStateException("body is required");
        }
        
        // Apply defaults for optional fields
        StmtExpr effectivePrecondition = precondition != null 
                ? precondition 
                : new StmtExpr.LiteralBool(true);
        
        StmtExpr effectiveDecreases = decreases != null 
                ? decreases 
                : new StmtExpr.LiteralInt(0);
        
        boolean effectiveDeterministic = deterministic != null 
                ? deterministic 
                : true;
        
        Optional<StmtExpr> effectiveReads = reads != null 
                ? Optional.of(reads) 
                : Optional.empty();
        
        StmtExpr effectiveModifies = modifies != null 
                ? modifies 
                : new StmtExpr.All();
        
        return new Procedure(
                name,
                List.copyOf(inputs),
                output,
                effectivePrecondition,
                effectiveDecreases,
                effectiveDeterministic,
                effectiveReads,
                effectiveModifies,
                body
        );
    }
    
    /**
     * Creates a new builder initialized from an existing Procedure.
     * 
     * @param procedure the procedure to copy from
     * @return a new builder with values from the procedure
     * @throws NullPointerException if procedure is null
     */
    public static ProcedureBuilder from(Procedure procedure) {
        Objects.requireNonNull(procedure, "procedure must not be null");
        ProcedureBuilder builder = new ProcedureBuilder()
                .name(procedure.name())
                .inputs(procedure.inputs())
                .output(procedure.output())
                .precondition(procedure.precondition())
                .decreases(procedure.decreases())
                .deterministic(procedure.deterministic())
                .modifies(procedure.modifies())
                .body(procedure.body());
        
        procedure.reads().ifPresent(builder::reads);
        
        return builder;
    }
}
