/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ion;

import com.amazon.ion.*;
import com.amazon.ion.system.IonBinaryWriterBuilder;
import com.amazon.ion.system.IonSystemBuilder;
import com.strata.laurel.ast.Program;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Main entry point for serializing Laurel programs to ION binary format.
 * 
 * <p>The serialized output includes:
 * <ol>
 *   <li>A local symbol table containing all symbols used in the program</li>
 *   <li>The encoded program as an ION s-expression</li>
 * </ol>
 * 
 * <p>The output format is compatible with the Lean Strata framework's
 * {@code FromIon} deserialization.
 * 
 * <p>Example usage:
 * <pre>{@code
 * Program program = new ProgramBuilder()
 *     .addProcedure(...)
 *     .build();
 * 
 * byte[] ionBytes = IonSerializer.serialize(program);
 * }</pre>
 */
public class IonSerializer {
    
    private final IonSystem ionSystem;
    
    /**
     * Creates a new IonSerializer.
     */
    public IonSerializer() {
        this.ionSystem = IonSystemBuilder.standard().build();
    }
    
    /**
     * Serializes a Laurel program to ION binary format.
     * 
     * @param program the program to serialize
     * @return the ION binary bytes
     * @throws IonSerializationException if serialization fails
     */
    public byte[] serialize(Program program) {
        try {
            // Create encoder with fresh symbol table
            SExprEncoder encoder = new SExprEncoder();
            
            // Encode the program (this populates the symbol table)
            IonValue encodedProgram = encoder.encode(program);
            
            // Build the output with symbol table and encoded program
            return buildIonOutput(encoder.getSymbolTable(), encodedProgram);
        } catch (Exception e) {
            throw new IonSerializationException("Failed to serialize program", e);
        }
    }
    
    /**
     * Serializes a Laurel program to ION binary format.
     * 
     * <p>Static convenience method.
     * 
     * @param program the program to serialize
     * @return the ION binary bytes
     * @throws IonSerializationException if serialization fails
     */
    public static byte[] serializeProgram(Program program) {
        return new IonSerializer().serialize(program);
    }
    
    /**
     * Builds the final ION output with symbol table and encoded value.
     */
    private byte[] buildIonOutput(SymbolTableBuilder symbolTable, IonValue encodedProgram) 
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Don't create a manual symbol table - let the Ion writer handle it automatically
        // Just write the program directly
        try (IonWriter writer = IonBinaryWriterBuilder.standard().build(baos)) {
            encodedProgram.writeTo(writer);
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Creates the local symbol table struct.
     * 
     * <p>Format:
     * <pre>
     * $ion_symbol_table::{
     *   imports: $ion_symbol_table,
     *   symbols: ["symbol1", "symbol2", ...]
     * }
     * </pre>
     */
    private IonStruct createSymbolTableStruct(List<String> symbols) {
        IonStruct struct = ionSystem.newEmptyStruct();
        
        // Add annotation
        struct.addTypeAnnotation("$ion_symbol_table");
        
        // Add imports field (import the system symbol table)
        struct.put("imports", ionSystem.newSymbol("$ion_symbol_table"));
        
        // Add symbols field
        IonList symbolList = ionSystem.newEmptyList();
        for (String symbol : symbols) {
            symbolList.add(ionSystem.newString(symbol));
        }
        struct.put("symbols", symbolList);
        
        return struct;
    }
    
    /**
     * Exception thrown when ION serialization fails.
     */
    public static class IonSerializationException extends RuntimeException {
        public IonSerializationException(String message) {
            super(message);
        }
        
        public IonSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
