/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.pretty;

import com.strata.laurel.ast.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pretty printer for Laurel AST nodes.
 * 
 * <p>Produces human-readable string representations of Laurel types, expressions,
 * procedures, and programs with appropriate indentation and formatting.
 * 
 * <p>Example usage:
 * <pre>{@code
 * PrettyPrinter pp = new PrettyPrinter();
 * String output = pp.print(program);
 * System.out.println(output);
 * }</pre>
 */
public class PrettyPrinter {
    
    private static final String DEFAULT_INDENT = "  ";
    
    private final String indentString;
    
    /**
     * Creates a pretty printer with the default indentation (2 spaces).
     */
    public PrettyPrinter() {
        this(DEFAULT_INDENT);
    }
    
    /**
     * Creates a pretty printer with a custom indentation string.
     * 
     * @param indentString the string to use for each level of indentation
     */
    public PrettyPrinter(String indentString) {
        this.indentString = indentString;
    }
    
    // ========== HighType Printing ==========
    
    /**
     * Pretty-prints a HighType to a string.
     * 
     * @param type the type to print
     * @return a human-readable string representation
     */
    public String print(HighType type) {
        return printType(type);
    }
    
    private String printType(HighType type) {
        return switch (type) {
            case HighType.TVoid t -> "void";
            case HighType.TBool t -> "bool";
            case HighType.TInt t -> "int";
            case HighType.TFloat64 t -> "float64";
            case HighType.UserDefined t -> t.name();
            case HighType.Applied t -> printAppliedType(t);
            case HighType.Pure t -> "pure " + printType(t.base());
            case HighType.Intersection t -> printIntersectionType(t);
        };
    }
    
    private String printAppliedType(HighType.Applied type) {
        String base = printType(type.base());
        if (type.typeArguments().isEmpty()) {
            return base;
        }
        String args = type.typeArguments().stream()
                .map(this::printType)
                .collect(Collectors.joining(", "));
        return base + "<" + args + ">";
    }
    
    private String printIntersectionType(HighType.Intersection type) {
        if (type.types().isEmpty()) {
            return "⊤"; // Top type for empty intersection
        }
        return type.types().stream()
                .map(this::printType)
                .collect(Collectors.joining(" & "));
    }

    
    // ========== StmtExpr Printing ==========
    
    /**
     * Pretty-prints a StmtExpr to a string.
     * 
     * @param expr the expression/statement to print
     * @return a human-readable string representation
     */
    public String print(StmtExpr expr) {
        return printExpr(expr, 0);
    }
    
    private String printExpr(StmtExpr expr, int indent) {
        return switch (expr) {
            // Statement-like
            case StmtExpr.IfThenElse e -> printIfThenElse(e, indent);
            case StmtExpr.Block e -> printBlock(e, indent);
            case StmtExpr.LocalVariable e -> printLocalVariable(e, indent);
            case StmtExpr.While e -> printWhile(e, indent);
            case StmtExpr.Exit e -> "exit " + e.target();
            case StmtExpr.Return e -> printReturn(e);
            
            // Expression-like
            case StmtExpr.LiteralInt e -> String.valueOf(e.value());
            case StmtExpr.LiteralBool e -> String.valueOf(e.value());
            case StmtExpr.Identifier e -> e.name();
            case StmtExpr.Assign e -> printAssign(e, indent);
            case StmtExpr.FieldSelect e -> printFieldSelect(e);
            case StmtExpr.PureFieldUpdate e -> printPureFieldUpdate(e, indent);
            case StmtExpr.StaticCall e -> printStaticCall(e, indent);
            case StmtExpr.PrimitiveOp e -> printPrimitiveOp(e, indent);
            
            // Instance-related
            case StmtExpr.This e -> "this";
            case StmtExpr.ReferenceEquals e -> printReferenceEquals(e, indent);
            case StmtExpr.AsType e -> printAsType(e, indent);
            case StmtExpr.IsType e -> printIsType(e, indent);
            case StmtExpr.InstanceCall e -> printInstanceCall(e, indent);
            
            // Verification-specific
            case StmtExpr.Forall e -> printForall(e, indent);
            case StmtExpr.Exists e -> printExists(e, indent);
            case StmtExpr.Assigned e -> "assigned(" + printExpr(e.name(), indent) + ")";
            case StmtExpr.Old e -> "old(" + printExpr(e.value(), indent) + ")";
            case StmtExpr.Fresh e -> "fresh(" + printExpr(e.value(), indent) + ")";
            
            // Proof-related
            case StmtExpr.Assert e -> "assert " + printExpr(e.condition(), indent);
            case StmtExpr.Assume e -> "assume " + printExpr(e.condition(), indent);
            case StmtExpr.ProveBy e -> printProveBy(e, indent);
            case StmtExpr.ContractOf e -> printContractOf(e, indent);
            case StmtExpr.Abstract e -> "abstract";
            case StmtExpr.All e -> "all";
            case StmtExpr.Hole e -> "_";
        };
    }
    
    private String printIfThenElse(StmtExpr.IfThenElse expr, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append("if (").append(printExpr(expr.cond(), indent)).append(") ");
        sb.append(printExpr(expr.thenBranch(), indent));
        expr.elseBranch().ifPresent(elseBranch -> {
            sb.append(" else ");
            sb.append(printExpr(elseBranch, indent));
        });
        return sb.toString();
    }
    
    private String printBlock(StmtExpr.Block expr, int indent) {
        StringBuilder sb = new StringBuilder();
        expr.label().ifPresent(label -> sb.append(label).append(": "));
        sb.append("{\n");
        for (StmtExpr stmt : expr.statements()) {
            sb.append(indentStr(indent + 1));
            sb.append(printExpr(stmt, indent + 1));
            sb.append(";\n");
        }
        sb.append(indentStr(indent)).append("}");
        return sb.toString();
    }
    
    private String printLocalVariable(StmtExpr.LocalVariable expr, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append("var ").append(expr.name()).append(": ").append(printType(expr.type()));
        expr.initializer().ifPresent(init -> {
            sb.append(" = ").append(printExpr(init, indent));
        });
        return sb.toString();
    }
    
    private String printWhile(StmtExpr.While expr, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append("while (").append(printExpr(expr.cond(), indent)).append(")");
        expr.invariant().ifPresent(inv -> {
            sb.append("\n").append(indentStr(indent + 1));
            sb.append("invariant ").append(printExpr(inv, indent + 1));
        });
        expr.decreases().ifPresent(dec -> {
            sb.append("\n").append(indentStr(indent + 1));
            sb.append("decreases ").append(printExpr(dec, indent + 1));
        });
        sb.append(" ").append(printExpr(expr.body(), indent));
        return sb.toString();
    }
    
    private String printReturn(StmtExpr.Return expr) {
        return expr.value()
                .map(v -> "return " + printExpr(v, 0))
                .orElse("return");
    }
    
    private String printAssign(StmtExpr.Assign expr, int indent) {
        return printExpr(expr.target(), indent) + " = " + printExpr(expr.value(), indent);
    }
    
    private String printFieldSelect(StmtExpr.FieldSelect expr) {
        return printExpr(expr.target(), 0) + "." + expr.fieldName();
    }
    
    private String printPureFieldUpdate(StmtExpr.PureFieldUpdate expr, int indent) {
        return printExpr(expr.target(), indent) + " with { " + 
               expr.fieldName() + " = " + printExpr(expr.newValue(), indent) + " }";
    }
    
    private String printStaticCall(StmtExpr.StaticCall expr, int indent) {
        String args = expr.arguments().stream()
                .map(arg -> printExpr(arg, indent))
                .collect(Collectors.joining(", "));
        return expr.callee() + "(" + args + ")";
    }
    
    private String printPrimitiveOp(StmtExpr.PrimitiveOp expr, int indent) {
        List<StmtExpr> args = expr.arguments();
        return switch (expr.operator()) {
            // Unary operators
            case NOT -> "!" + printExprParens(args.get(0), indent);
            case NEG -> "-" + printExprParens(args.get(0), indent);
            
            // Binary operators
            case EQ -> printBinaryOp(args, " == ", indent);
            case NEQ -> printBinaryOp(args, " != ", indent);
            case AND -> printBinaryOp(args, " && ", indent);
            case OR -> printBinaryOp(args, " || ", indent);
            case ADD -> printBinaryOp(args, " + ", indent);
            case SUB -> printBinaryOp(args, " - ", indent);
            case MUL -> printBinaryOp(args, " * ", indent);
            case DIV -> printBinaryOp(args, " / ", indent);
            case MOD -> printBinaryOp(args, " % ", indent);
            case LT -> printBinaryOp(args, " < ", indent);
            case LEQ -> printBinaryOp(args, " <= ", indent);
            case GT -> printBinaryOp(args, " > ", indent);
            case GEQ -> printBinaryOp(args, " >= ", indent);
        };
    }
    
    private String printBinaryOp(List<StmtExpr> args, String op, int indent) {
        if (args.size() < 2) {
            return args.isEmpty() ? "" : printExpr(args.get(0), indent);
        }
        return printExprParens(args.get(0), indent) + op + printExprParens(args.get(1), indent);
    }
    
    private String printExprParens(StmtExpr expr, int indent) {
        String printed = printExpr(expr, indent);
        // Add parentheses for complex expressions
        if (expr instanceof StmtExpr.PrimitiveOp || 
            expr instanceof StmtExpr.IfThenElse ||
            expr instanceof StmtExpr.Forall ||
            expr instanceof StmtExpr.Exists) {
            return "(" + printed + ")";
        }
        return printed;
    }
    
    private String printReferenceEquals(StmtExpr.ReferenceEquals expr, int indent) {
        return printExpr(expr.lhs(), indent) + " === " + printExpr(expr.rhs(), indent);
    }
    
    private String printAsType(StmtExpr.AsType expr, int indent) {
        return printExpr(expr.target(), indent) + " as " + printType(expr.targetType());
    }
    
    private String printIsType(StmtExpr.IsType expr, int indent) {
        return printExpr(expr.target(), indent) + " is " + printType(expr.type());
    }
    
    private String printInstanceCall(StmtExpr.InstanceCall expr, int indent) {
        String args = expr.arguments().stream()
                .map(arg -> printExpr(arg, indent))
                .collect(Collectors.joining(", "));
        return printExpr(expr.target(), indent) + "." + expr.callee() + "(" + args + ")";
    }
    
    private String printForall(StmtExpr.Forall expr, int indent) {
        return "forall " + expr.name() + ": " + printType(expr.type()) + 
               " :: " + printExpr(expr.body(), indent);
    }
    
    private String printExists(StmtExpr.Exists expr, int indent) {
        return "exists " + expr.name() + ": " + printType(expr.type()) + 
               " :: " + printExpr(expr.body(), indent);
    }
    
    private String printProveBy(StmtExpr.ProveBy expr, int indent) {
        return printExpr(expr.value(), indent) + " by { " + printExpr(expr.proof(), indent) + " }";
    }
    
    private String printContractOf(StmtExpr.ContractOf expr, int indent) {
        String contractName = switch (expr.type()) {
            case READS -> "reads";
            case MODIFIES -> "modifies";
            case PRECONDITION -> "precondition";
            case POSTCONDITION -> "postcondition";
        };
        return contractName + "(" + printExpr(expr.function(), indent) + ")";
    }
    
    private String indentStr(int level) {
        return indentString.repeat(level);
    }

    
    // ========== Procedure Printing ==========
    
    /**
     * Pretty-prints a Procedure to a string.
     * 
     * @param procedure the procedure to print
     * @return a human-readable string representation
     */
    public String print(Procedure procedure) {
        return printProcedure(procedure, 0);
    }
    
    private String printProcedure(Procedure proc, int indent) {
        StringBuilder sb = new StringBuilder();
        
        // Procedure signature
        sb.append(indentStr(indent));
        if (!proc.deterministic()) {
            sb.append("nondet ");
        }
        sb.append("procedure ").append(proc.name()).append("(");
        sb.append(printParameters(proc.inputs()));
        sb.append("): ").append(printType(proc.output()));
        sb.append("\n");
        
        // Contracts
        if (!isDefaultPrecondition(proc.precondition())) {
            sb.append(indentStr(indent + 1));
            sb.append("requires ").append(printExpr(proc.precondition(), indent + 1));
            sb.append("\n");
        }
        
        proc.reads().ifPresent(reads -> {
            sb.append(indentStr(indent + 1));
            sb.append("reads ").append(printExpr(reads, indent + 1));
            sb.append("\n");
        });
        
        if (!isDefaultModifies(proc.modifies())) {
            sb.append(indentStr(indent + 1));
            sb.append("modifies ").append(printExpr(proc.modifies(), indent + 1));
            sb.append("\n");
        }
        
        if (!isDefaultDecreases(proc.decreases())) {
            sb.append(indentStr(indent + 1));
            sb.append("decreases ").append(printExpr(proc.decreases(), indent + 1));
            sb.append("\n");
        }
        
        // Body
        sb.append(printBody(proc.body(), indent));
        
        return sb.toString();
    }
    
    private String printParameters(List<Parameter> params) {
        return params.stream()
                .map(p -> p.name() + ": " + printType(p.type()))
                .collect(Collectors.joining(", "));
    }
    
    private String printBody(Body body, int indent) {
        return switch (body) {
            case Body.Transparent t -> {
                StringBuilder sb = new StringBuilder();
                sb.append(indentStr(indent)).append("{\n");
                sb.append(indentStr(indent + 1)).append(printExpr(t.body(), indent + 1));
                sb.append("\n").append(indentStr(indent)).append("}");
                yield sb.toString();
            }
            case Body.Opaque o -> {
                StringBuilder sb = new StringBuilder();
                sb.append(indentStr(indent + 1));
                sb.append("ensures ").append(printExpr(o.postcondition(), indent + 1));
                sb.append("\n");
                o.implementation().ifPresent(impl -> {
                    sb.append(indentStr(indent)).append("{\n");
                    sb.append(indentStr(indent + 1)).append(printExpr(impl, indent + 1));
                    sb.append("\n").append(indentStr(indent)).append("}");
                });
                yield sb.toString();
            }
            case Body.Abstract a -> {
                StringBuilder sb = new StringBuilder();
                sb.append(indentStr(indent + 1));
                sb.append("ensures ").append(printExpr(a.postcondition(), indent + 1));
                yield sb.toString();
            }
        };
    }
    
    private boolean isDefaultPrecondition(StmtExpr expr) {
        return expr instanceof StmtExpr.LiteralBool lb && lb.value();
    }
    
    private boolean isDefaultModifies(StmtExpr expr) {
        return expr instanceof StmtExpr.Block b && b.statements().isEmpty();
    }
    
    private boolean isDefaultDecreases(StmtExpr expr) {
        return expr instanceof StmtExpr.Block b && b.statements().isEmpty();
    }
    
    // ========== Program Printing ==========
    
    /**
     * Pretty-prints a Program to a string.
     * 
     * @param program the program to print
     * @return a human-readable string representation
     */
    public String print(Program program) {
        StringBuilder sb = new StringBuilder();
        
        // Static fields
        if (!program.staticFields().isEmpty()) {
            sb.append("// Static Fields\n");
            for (Field field : program.staticFields()) {
                sb.append(printField(field, 0));
                sb.append("\n");
            }
            sb.append("\n");
        }
        
        // Type definitions
        if (!program.types().isEmpty()) {
            sb.append("// Type Definitions\n");
            for (TypeDefinition typeDef : program.types()) {
                sb.append(printTypeDefinition(typeDef, 0));
                sb.append("\n\n");
            }
        }
        
        // Static procedures
        if (!program.staticProcedures().isEmpty()) {
            sb.append("// Static Procedures\n");
            for (Procedure proc : program.staticProcedures()) {
                sb.append(printProcedure(proc, 0));
                sb.append("\n\n");
            }
        }
        
        return sb.toString().stripTrailing();
    }
    
    // ========== Field Printing ==========
    
    /**
     * Pretty-prints a Field to a string.
     * 
     * @param field the field to print
     * @return a human-readable string representation
     */
    public String print(Field field) {
        return printField(field, 0);
    }
    
    private String printField(Field field, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indentStr(indent));
        if (field.isMutable()) {
            sb.append("var ");
        } else {
            sb.append("val ");
        }
        sb.append(field.name()).append(": ").append(printType(field.type()));
        return sb.toString();
    }
    
    // ========== TypeDefinition Printing ==========
    
    /**
     * Pretty-prints a TypeDefinition to a string.
     * 
     * @param typeDef the type definition to print
     * @return a human-readable string representation
     */
    public String print(TypeDefinition typeDef) {
        return printTypeDefinition(typeDef, 0);
    }
    
    private String printTypeDefinition(TypeDefinition typeDef, int indent) {
        return switch (typeDef) {
            case TypeDefinition.Composite c -> printCompositeType(c.type(), indent);
            case TypeDefinition.Constrained c -> printConstrainedType(c.type(), indent);
        };
    }
    
    private String printCompositeType(CompositeType type, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indentStr(indent)).append("type ").append(type.name());
        
        if (!type.extending().isEmpty()) {
            sb.append(" extends ");
            sb.append(String.join(", ", type.extending()));
        }
        
        sb.append(" {\n");
        
        // Fields
        for (Field field : type.fields()) {
            sb.append(printField(field, indent + 1));
            sb.append("\n");
        }
        
        // Instance procedures
        if (!type.instanceProcedures().isEmpty()) {
            if (!type.fields().isEmpty()) {
                sb.append("\n");
            }
            for (Procedure proc : type.instanceProcedures()) {
                sb.append(printProcedure(proc, indent + 1));
                sb.append("\n");
            }
        }
        
        sb.append(indentStr(indent)).append("}");
        return sb.toString();
    }
    
    private String printConstrainedType(ConstrainedType type, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indentStr(indent)).append("type ").append(type.name());
        sb.append(" = { ").append(type.valueName()).append(": ");
        sb.append(printType(type.base())).append(" | ");
        sb.append(printExpr(type.constraint(), indent));
        sb.append(" }\n");
        sb.append(indentStr(indent + 1)).append("witness ").append(printExpr(type.witness(), indent + 1));
        return sb.toString();
    }
    
    // ========== Parameter Printing ==========
    
    /**
     * Pretty-prints a Parameter to a string.
     * 
     * @param param the parameter to print
     * @return a human-readable string representation
     */
    public String print(Parameter param) {
        return param.name() + ": " + printType(param.type());
    }
    
    // ========== Body Printing ==========
    
    /**
     * Pretty-prints a Body to a string.
     * 
     * @param body the body to print
     * @return a human-readable string representation
     */
    public String print(Body body) {
        return printBody(body, 0);
    }
}
