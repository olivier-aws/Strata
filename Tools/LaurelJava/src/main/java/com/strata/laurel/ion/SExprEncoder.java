/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ion;

import com.amazon.ion.IonSexp;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonValue;
import com.amazon.ion.system.IonSystemBuilder;
import com.strata.laurel.ast.*;

import java.util.List;
import java.util.Optional;

/**
 * Encodes Laurel AST nodes to ION s-expressions.
 * 
 * <p>The encoding format matches the Lean FromIon deserialization format
 * used by the Strata framework.
 */
public class SExprEncoder {
    
    private final IonSystem ionSystem;
    private final SymbolTableBuilder symbolTable;
    
    // Laurel dialect prefix
    private static final String LAUREL = "Laurel";
    
    /**
     * Creates a new encoder with the given symbol table.
     * 
     * @param symbolTable the symbol table for interning symbols
     */
    public SExprEncoder(SymbolTableBuilder symbolTable) {
        this.ionSystem = IonSystemBuilder.standard().build();
        this.symbolTable = symbolTable;
    }
    
    /**
     * Creates a new encoder with a fresh symbol table.
     */
    public SExprEncoder() {
        this(new SymbolTableBuilder());
    }
    
    /**
     * Returns the symbol table used by this encoder.
     */
    public SymbolTableBuilder getSymbolTable() {
        return symbolTable;
    }
    
    /**
     * Returns the ION system used by this encoder.
     */
    public IonSystem getIonSystem() {
        return ionSystem;
    }

    // ========== Helper Methods ==========
    
    /**
     * Creates a symbol and interns it in the symbol table.
     */
    private IonValue symbol(String name) {
        symbolTable.intern(name);
        return ionSystem.newSymbol(name);
    }
    
    /**
     * Creates a qualified Laurel symbol (e.g., "Laurel.TVoid").
     */
    private IonValue laurelSymbol(String name) {
        return symbol(LAUREL + "." + name);
    }
    
    /**
     * Creates an s-expression with the given elements.
     */
    private IonSexp sexp(IonValue... elements) {
        IonSexp sexp = ionSystem.newEmptySexp();
        for (IonValue element : elements) {
            sexp.add(element.clone());
        }
        return sexp;
    }
    
    /**
     * Creates an s-expression from a list of elements.
     */
    private IonSexp sexpFromList(List<IonValue> elements) {
        IonSexp sexp = ionSystem.newEmptySexp();
        for (IonValue element : elements) {
            sexp.add(element.clone());
        }
        return sexp;
    }
    
    /**
     * Creates a null annotation value.
     */
    private IonValue nullValue() {
        return ionSystem.newNull();
    }
    
    /**
     * Creates an integer value.
     */
    private IonValue intValue(long value) {
        return ionSystem.newInt(value);
    }
    
    /**
     * Creates a string value.
     */
    private IonValue stringValue(String value) {
        return ionSystem.newString(value);
    }
    
    /**
     * Creates a boolean value encoded as Init.true or Init.false.
     */
    private IonValue boolValue(boolean value) {
        String sym = value ? "Init.true" : "Init.false";
        symbolTable.intern(sym);
        return sexp(symbol("op"), sexp(symbol(sym), nullValue()));
    }
    
    /**
     * Encodes an optional value.
     */
    private IonValue encodeOption(Optional<?> opt, java.util.function.Function<Object, IonValue> encoder) {
        symbolTable.intern("option");
        if (opt.isEmpty()) {
            return sexp(symbol("option"), nullValue());
        } else {
            @SuppressWarnings("unchecked")
            Object value = opt.get();
            return sexp(symbol("option"), nullValue(), encoder.apply(value));
        }
    }
    
    /**
     * Encodes a list as a seq.
     */
    private IonValue encodeSeq(List<?> list, java.util.function.Function<Object, IonValue> encoder) {
        symbolTable.intern("seq");
        IonSexp seq = ionSystem.newEmptySexp();
        seq.add(symbol("seq").clone());
        seq.add(nullValue().clone());
        for (Object item : list) {
            @SuppressWarnings("unchecked")
            IonValue encoded = encoder.apply(item);
            seq.add(encoded.clone());
        }
        return seq;
    }
    
    // ========== HighType Encoding ==========
    
    /**
     * Encodes a HighType to an ION s-expression.
     * 
     * @param type the type to encode
     * @return the ION s-expression
     */
    public IonValue encode(HighType type) {
        return switch (type) {
            case HighType.TVoid t -> sexp(symbol("ident"), nullValue(), laurelSymbol("TVoid"));
            case HighType.TBool t -> sexp(symbol("ident"), nullValue(), laurelSymbol("TBool"));
            case HighType.TInt t -> sexp(symbol("ident"), nullValue(), laurelSymbol("TInt"));
            case HighType.TFloat64 t -> sexp(symbol("ident"), nullValue(), laurelSymbol("TFloat64"));
            case HighType.UserDefined t -> encodeUserDefinedType(t);
            case HighType.Applied t -> encodeAppliedType(t);
            case HighType.Pure t -> encodePureType(t);
            case HighType.Intersection t -> encodeIntersectionType(t);
        };
    }
    
    private IonValue encodeUserDefinedType(HighType.UserDefined type) {
        symbolTable.intern("ident");
        symbolTable.intern("strlit");
        return sexp(
            symbol("ident"),
            nullValue(),
            laurelSymbol("UserDefined"),
            sexp(symbol("strlit"), nullValue(), stringValue(type.name()))
        );
    }
    
    private IonValue encodeAppliedType(HighType.Applied type) {
        symbolTable.intern("ident");
        IonSexp result = ionSystem.newEmptySexp();
        result.add(symbol("ident").clone());
        result.add(nullValue().clone());
        result.add(laurelSymbol("Applied").clone());
        result.add(encode(type.base()).clone());
        for (HighType arg : type.typeArguments()) {
            result.add(encode(arg).clone());
        }
        return result;
    }
    
    private IonValue encodePureType(HighType.Pure type) {
        symbolTable.intern("ident");
        return sexp(
            symbol("ident"),
            nullValue(),
            laurelSymbol("Pure"),
            encode(type.base())
        );
    }
    
    private IonValue encodeIntersectionType(HighType.Intersection type) {
        symbolTable.intern("ident");
        IonSexp result = ionSystem.newEmptySexp();
        result.add(symbol("ident").clone());
        result.add(nullValue().clone());
        result.add(laurelSymbol("Intersection").clone());
        for (HighType t : type.types()) {
            result.add(encode(t).clone());
        }
        return result;
    }

    // ========== StmtExpr Encoding ==========
    
    /**
     * Encodes a StmtExpr to an ION s-expression.
     * 
     * @param expr the expression/statement to encode
     * @return the ION s-expression
     */
    public IonValue encode(StmtExpr expr) {
        return switch (expr) {
            // Statement-like
            case StmtExpr.IfThenElse e -> encodeIfThenElse(e);
            case StmtExpr.Block e -> encodeBlock(e);
            case StmtExpr.LocalVariable e -> encodeLocalVariable(e);
            case StmtExpr.While e -> encodeWhile(e);
            case StmtExpr.Exit e -> encodeExit(e);
            case StmtExpr.Return e -> encodeReturn(e);
            
            // Expression-like
            case StmtExpr.LiteralInt e -> encodeLiteralInt(e);
            case StmtExpr.LiteralBool e -> encodeLiteralBool(e);
            case StmtExpr.Identifier e -> encodeIdentifier(e);
            case StmtExpr.Assign e -> encodeAssign(e);
            case StmtExpr.FieldSelect e -> encodeFieldSelect(e);
            case StmtExpr.PureFieldUpdate e -> encodePureFieldUpdate(e);
            case StmtExpr.StaticCall e -> encodeStaticCall(e);
            case StmtExpr.PrimitiveOp e -> encodePrimitiveOp(e);
            
            // Instance-related
            case StmtExpr.This e -> encodeThis();
            case StmtExpr.ReferenceEquals e -> encodeReferenceEquals(e);
            case StmtExpr.AsType e -> encodeAsType(e);
            case StmtExpr.IsType e -> encodeIsType(e);
            case StmtExpr.InstanceCall e -> encodeInstanceCall(e);
            
            // Verification-specific
            case StmtExpr.Forall e -> encodeForall(e);
            case StmtExpr.Exists e -> encodeExists(e);
            case StmtExpr.Assigned e -> encodeAssigned(e);
            case StmtExpr.Old e -> encodeOld(e);
            case StmtExpr.Fresh e -> encodeFresh(e);
            
            // Proof-related
            case StmtExpr.Assert e -> encodeAssert(e);
            case StmtExpr.Assume e -> encodeAssume(e);
            case StmtExpr.ProveBy e -> encodeProveBy(e);
            case StmtExpr.ContractOf e -> encodeContractOf(e);
            case StmtExpr.Abstract e -> encodeAbstract();
            case StmtExpr.All e -> encodeAll();
            case StmtExpr.Hole e -> encodeHole();
        };
    }
    
    // --- Statement-like ---
    
    private IonValue encodeIfThenElse(StmtExpr.IfThenElse e) {
        return sexp(
            laurelSymbol("IfThenElse"),
            nullValue(),
            encode(e.cond()),
            encode(e.thenBranch()),
            encodeOptionStmtExpr(e.elseBranch())
        );
    }
    
    private IonValue encodeBlock(StmtExpr.Block e) {
        IonSexp stmts = ionSystem.newEmptySexp();
        stmts.add(symbol("seq").clone());
        stmts.add(nullValue().clone());
        for (StmtExpr stmt : e.statements()) {
            stmts.add(encode(stmt).clone());
        }
        return sexp(
            laurelSymbol("Block"),
            nullValue(),
            stmts,
            encodeOptionString(e.label())
        );
    }
    
    private IonValue encodeLocalVariable(StmtExpr.LocalVariable e) {
        return sexp(
            laurelSymbol("LocalVariable"),
            nullValue(),
            sexp(symbol("strlit"), nullValue(), stringValue(e.name())),
            encode(e.type()),
            encodeOptionStmtExpr(e.initializer())
        );
    }
    
    private IonValue encodeWhile(StmtExpr.While e) {
        return sexp(
            laurelSymbol("While"),
            nullValue(),
            encode(e.cond()),
            encodeOptionStmtExpr(e.invariant()),
            encodeOptionStmtExpr(e.decreases()),
            encode(e.body())
        );
    }
    
    private IonValue encodeExit(StmtExpr.Exit e) {
        return sexp(
            laurelSymbol("Exit"),
            nullValue(),
            sexp(symbol("strlit"), nullValue(), stringValue(e.target()))
        );
    }
    
    private IonValue encodeReturn(StmtExpr.Return e) {
        return sexp(
            laurelSymbol("Return"),
            nullValue(),
            encodeOptionStmtExpr(e.value())
        );
    }
    
    // --- Expression-like ---
    
    private IonValue encodeLiteralInt(StmtExpr.LiteralInt e) {
        return sexp(
            laurelSymbol("LiteralInt"),
            nullValue(),
            sexp(symbol("num"), nullValue(), intValue(e.value()))
        );
    }
    
    private IonValue encodeLiteralBool(StmtExpr.LiteralBool e) {
        return sexp(
            laurelSymbol("LiteralBool"),
            nullValue(),
            boolValue(e.value())
        );
    }
    
    private IonValue encodeIdentifier(StmtExpr.Identifier e) {
        return sexp(
            laurelSymbol("Identifier"),
            nullValue(),
            sexp(symbol("strlit"), nullValue(), stringValue(e.name()))
        );
    }
    
    private IonValue encodeAssign(StmtExpr.Assign e) {
        return sexp(
            laurelSymbol("Assign"),
            nullValue(),
            encode(e.target()),
            encode(e.value())
        );
    }
    
    private IonValue encodeFieldSelect(StmtExpr.FieldSelect e) {
        return sexp(
            laurelSymbol("FieldSelect"),
            nullValue(),
            encode(e.target()),
            sexp(symbol("strlit"), nullValue(), stringValue(e.fieldName()))
        );
    }
    
    private IonValue encodePureFieldUpdate(StmtExpr.PureFieldUpdate e) {
        return sexp(
            laurelSymbol("PureFieldUpdate"),
            nullValue(),
            encode(e.target()),
            sexp(symbol("strlit"), nullValue(), stringValue(e.fieldName())),
            encode(e.newValue())
        );
    }
    
    private IonValue encodeStaticCall(StmtExpr.StaticCall e) {
        IonSexp args = ionSystem.newEmptySexp();
        args.add(symbol("seq").clone());
        args.add(nullValue().clone());
        for (StmtExpr arg : e.arguments()) {
            args.add(encode(arg).clone());
        }
        return sexp(
            laurelSymbol("StaticCall"),
            nullValue(),
            sexp(symbol("strlit"), nullValue(), stringValue(e.callee())),
            args
        );
    }
    
    private IonValue encodePrimitiveOp(StmtExpr.PrimitiveOp e) {
        IonSexp args = ionSystem.newEmptySexp();
        args.add(symbol("seq").clone());
        args.add(nullValue().clone());
        for (StmtExpr arg : e.arguments()) {
            args.add(encode(arg).clone());
        }
        return sexp(
            laurelSymbol("PrimitiveOp"),
            nullValue(),
            encodeOperation(e.operator()),
            args
        );
    }
    
    // --- Instance-related ---
    
    private IonValue encodeThis() {
        return sexp(laurelSymbol("This"), nullValue());
    }
    
    private IonValue encodeReferenceEquals(StmtExpr.ReferenceEquals e) {
        return sexp(
            laurelSymbol("ReferenceEquals"),
            nullValue(),
            encode(e.lhs()),
            encode(e.rhs())
        );
    }
    
    private IonValue encodeAsType(StmtExpr.AsType e) {
        return sexp(
            laurelSymbol("AsType"),
            nullValue(),
            encode(e.target()),
            encode(e.targetType())
        );
    }
    
    private IonValue encodeIsType(StmtExpr.IsType e) {
        return sexp(
            laurelSymbol("IsType"),
            nullValue(),
            encode(e.target()),
            encode(e.type())
        );
    }
    
    private IonValue encodeInstanceCall(StmtExpr.InstanceCall e) {
        IonSexp args = ionSystem.newEmptySexp();
        args.add(symbol("seq").clone());
        args.add(nullValue().clone());
        for (StmtExpr arg : e.arguments()) {
            args.add(encode(arg).clone());
        }
        return sexp(
            laurelSymbol("InstanceCall"),
            nullValue(),
            encode(e.target()),
            sexp(symbol("strlit"), nullValue(), stringValue(e.callee())),
            args
        );
    }
    
    // --- Verification-specific ---
    
    private IonValue encodeForall(StmtExpr.Forall e) {
        return sexp(
            laurelSymbol("Forall"),
            nullValue(),
            sexp(symbol("strlit"), nullValue(), stringValue(e.name())),
            encode(e.type()),
            encode(e.body())
        );
    }
    
    private IonValue encodeExists(StmtExpr.Exists e) {
        return sexp(
            laurelSymbol("Exists"),
            nullValue(),
            sexp(symbol("strlit"), nullValue(), stringValue(e.name())),
            encode(e.type()),
            encode(e.body())
        );
    }
    
    private IonValue encodeAssigned(StmtExpr.Assigned e) {
        return sexp(
            laurelSymbol("Assigned"),
            nullValue(),
            encode(e.name())
        );
    }
    
    private IonValue encodeOld(StmtExpr.Old e) {
        return sexp(
            laurelSymbol("Old"),
            nullValue(),
            encode(e.value())
        );
    }
    
    private IonValue encodeFresh(StmtExpr.Fresh e) {
        return sexp(
            laurelSymbol("Fresh"),
            nullValue(),
            encode(e.value())
        );
    }
    
    // --- Proof-related ---
    
    private IonValue encodeAssert(StmtExpr.Assert e) {
        return sexp(
            laurelSymbol("Assert"),
            nullValue(),
            encode(e.condition())
        );
    }
    
    private IonValue encodeAssume(StmtExpr.Assume e) {
        return sexp(
            laurelSymbol("Assume"),
            nullValue(),
            encode(e.condition())
        );
    }
    
    private IonValue encodeProveBy(StmtExpr.ProveBy e) {
        return sexp(
            laurelSymbol("ProveBy"),
            nullValue(),
            encode(e.value()),
            encode(e.proof())
        );
    }
    
    private IonValue encodeContractOf(StmtExpr.ContractOf e) {
        return sexp(
            laurelSymbol("ContractOf"),
            nullValue(),
            encodeContractType(e.type()),
            encode(e.function())
        );
    }
    
    private IonValue encodeAbstract() {
        return sexp(laurelSymbol("Abstract"), nullValue());
    }
    
    private IonValue encodeAll() {
        return sexp(laurelSymbol("All"), nullValue());
    }
    
    private IonValue encodeHole() {
        return sexp(laurelSymbol("Hole"), nullValue());
    }
    
    // --- Helper encoders ---
    
    private IonValue encodeOptionStmtExpr(Optional<StmtExpr> opt) {
        symbolTable.intern("option");
        if (opt.isEmpty()) {
            return sexp(symbol("option"), nullValue());
        } else {
            return sexp(symbol("option"), nullValue(), encode(opt.get()));
        }
    }
    
    private IonValue encodeOptionString(Optional<String> opt) {
        symbolTable.intern("option");
        if (opt.isEmpty()) {
            return sexp(symbol("option"), nullValue());
        } else {
            return sexp(symbol("option"), nullValue(), 
                sexp(symbol("strlit"), nullValue(), stringValue(opt.get())));
        }
    }
    
    private IonValue encodeOperation(Operation op) {
        String opName = switch (op) {
            case EQ -> "Eq";
            case NEQ -> "Neq";
            case AND -> "And";
            case OR -> "Or";
            case NOT -> "Not";
            case NEG -> "Neg";
            case ADD -> "Add";
            case SUB -> "Sub";
            case MUL -> "Mul";
            case DIV -> "Div";
            case MOD -> "Mod";
            case LT -> "Lt";
            case LEQ -> "Leq";
            case GT -> "Gt";
            case GEQ -> "Geq";
        };
        return sexp(symbol("ident"), nullValue(), laurelSymbol(opName));
    }
    
    private IonValue encodeContractType(ContractType type) {
        String typeName = switch (type) {
            case READS -> "Reads";
            case MODIFIES -> "Modifies";
            case PRECONDITION -> "Precondition";
            case POSTCONDITION -> "PostCondition";
        };
        return sexp(symbol("ident"), nullValue(), laurelSymbol(typeName));
    }

    // ========== Procedure and Body Encoding ==========
    
    /**
     * Encodes a Procedure to an ION s-expression.
     * 
     * @param proc the procedure to encode
     * @return the ION s-expression
     */
    public IonValue encode(Procedure proc) {
        // Encode inputs as a seq
        IonSexp inputs = ionSystem.newEmptySexp();
        inputs.add(symbol("seq").clone());
        inputs.add(nullValue().clone());
        for (Parameter param : proc.inputs()) {
            inputs.add(encodeParameter(param).clone());
        }
        
        return sexp(
            laurelSymbol("Procedure"),
            nullValue(),
            sexp(symbol("strlit"), nullValue(), stringValue(proc.name())),
            inputs,
            encode(proc.output()),
            encode(proc.precondition()),
            encode(proc.decreases()),
            boolValue(proc.deterministic()),
            encodeOptionStmtExpr(proc.reads()),
            encode(proc.modifies()),
            encode(proc.body())
        );
    }
    
    /**
     * Encodes a Parameter to an ION s-expression.
     * 
     * @param param the parameter to encode
     * @return the ION s-expression
     */
    public IonValue encodeParameter(Parameter param) {
        return sexp(
            laurelSymbol("Parameter"),
            nullValue(),
            sexp(symbol("strlit"), nullValue(), stringValue(param.name())),
            encode(param.type())
        );
    }
    
    /**
     * Encodes a Body to an ION s-expression.
     * 
     * @param body the body to encode
     * @return the ION s-expression
     */
    public IonValue encode(Body body) {
        return switch (body) {
            case Body.Transparent b -> encodeTransparentBody(b);
            case Body.Opaque b -> encodeOpaqueBody(b);
            case Body.Abstract b -> encodeAbstractBody(b);
        };
    }
    
    private IonValue encodeTransparentBody(Body.Transparent body) {
        return sexp(
            laurelSymbol("Transparent"),
            nullValue(),
            encode(body.body())
        );
    }
    
    private IonValue encodeOpaqueBody(Body.Opaque body) {
        return sexp(
            laurelSymbol("Opaque"),
            nullValue(),
            encode(body.postcondition()),
            encodeOptionStmtExpr(body.implementation())
        );
    }
    
    private IonValue encodeAbstractBody(Body.Abstract body) {
        return sexp(
            laurelSymbol("Abstract"),
            nullValue(),
            encode(body.postcondition())
        );
    }

    // ========== TypeDefinition Encoding ==========
    
    /**
     * Encodes a TypeDefinition to an ION s-expression.
     * 
     * @param typeDef the type definition to encode
     * @return the ION s-expression
     */
    public IonValue encode(TypeDefinition typeDef) {
        return switch (typeDef) {
            case TypeDefinition.Composite c -> encodeCompositeTypeDef(c);
            case TypeDefinition.Constrained c -> encodeConstrainedTypeDef(c);
        };
    }
    
    private IonValue encodeCompositeTypeDef(TypeDefinition.Composite typeDef) {
        return sexp(
            laurelSymbol("Composite"),
            nullValue(),
            encodeCompositeType(typeDef.type())
        );
    }
    
    private IonValue encodeConstrainedTypeDef(TypeDefinition.Constrained typeDef) {
        return sexp(
            laurelSymbol("Constrainted"),  // Note: matches Lean typo "Constrainted"
            nullValue(),
            encodeConstrainedType(typeDef.type())
        );
    }
    
    /**
     * Encodes a CompositeType to an ION s-expression.
     * 
     * @param type the composite type to encode
     * @return the ION s-expression
     */
    public IonValue encodeCompositeType(CompositeType type) {
        // Encode extending list
        IonSexp extending = ionSystem.newEmptySexp();
        extending.add(symbol("seq").clone());
        extending.add(nullValue().clone());
        for (String ext : type.extending()) {
            extending.add(sexp(symbol("strlit"), nullValue(), stringValue(ext)).clone());
        }
        
        // Encode fields
        IonSexp fields = ionSystem.newEmptySexp();
        fields.add(symbol("seq").clone());
        fields.add(nullValue().clone());
        for (Field field : type.fields()) {
            fields.add(encodeField(field).clone());
        }
        
        // Encode instance procedures
        IonSexp procs = ionSystem.newEmptySexp();
        procs.add(symbol("seq").clone());
        procs.add(nullValue().clone());
        for (Procedure proc : type.instanceProcedures()) {
            procs.add(encode(proc).clone());
        }
        
        return sexp(
            laurelSymbol("CompositeType"),
            nullValue(),
            sexp(symbol("strlit"), nullValue(), stringValue(type.name())),
            extending,
            fields,
            procs
        );
    }
    
    /**
     * Encodes a Field to an ION s-expression.
     * 
     * @param field the field to encode
     * @return the ION s-expression
     */
    public IonValue encodeField(Field field) {
        return sexp(
            laurelSymbol("Field"),
            nullValue(),
            sexp(symbol("strlit"), nullValue(), stringValue(field.name())),
            boolValue(field.isMutable()),
            encode(field.type())
        );
    }
    
    /**
     * Encodes a ConstrainedType to an ION s-expression.
     * 
     * @param type the constrained type to encode
     * @return the ION s-expression
     */
    public IonValue encodeConstrainedType(ConstrainedType type) {
        return sexp(
            laurelSymbol("ConstrainedType"),
            nullValue(),
            sexp(symbol("strlit"), nullValue(), stringValue(type.name())),
            encode(type.base()),
            sexp(symbol("strlit"), nullValue(), stringValue(type.valueName())),
            encode(type.constraint()),
            encode(type.witness())
        );
    }

    // ========== Program Encoding ==========
    
    /**
     * Encodes a Program to an ION s-expression.
     * 
     * @param program the program to encode
     * @return the ION s-expression
     */
    public IonValue encode(Program program) {
        // Encode static procedures
        IonSexp procs = ionSystem.newEmptySexp();
        procs.add(symbol("seq").clone());
        procs.add(nullValue().clone());
        for (Procedure proc : program.staticProcedures()) {
            procs.add(encode(proc).clone());
        }
        
        // Encode static fields
        IonSexp fields = ionSystem.newEmptySexp();
        fields.add(symbol("seq").clone());
        fields.add(nullValue().clone());
        for (Field field : program.staticFields()) {
            fields.add(encodeField(field).clone());
        }
        
        // Encode type definitions
        IonSexp types = ionSystem.newEmptySexp();
        types.add(symbol("seq").clone());
        types.add(nullValue().clone());
        for (TypeDefinition typeDef : program.types()) {
            types.add(encode(typeDef).clone());
        }
        
        return sexp(
            laurelSymbol("Program"),
            nullValue(),
            procs,
            fields,
            types
        );
    }
}
