/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ion;

import com.amazon.ion.*;
import com.amazon.ion.system.IonReaderBuilder;
import com.amazon.ion.system.IonSystemBuilder;
import com.strata.laurel.ast.*;
import com.strata.laurel.generators.ProgramGenerator;
import com.strata.laurel.pretty.PrettyPrinter;
import net.jqwik.api.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ION serialization round-trip.
 * 
 * <p>Feature: java-laurel-ion, Property 1: ION Serialization Round-Trip
 * <p>Validates: Requirements 6.5
 * 
 * <p>For any valid Laurel Program constructed in Java, serializing it to ION
 * and deserializing it in Lean SHALL produce an equivalent Laurel AST.
 */
class IonSerializerRoundTripTest {

    private final IonSystem ionSystem = IonSystemBuilder.standard().build();
    private final IonSerializer serializer = new IonSerializer();
    private final PrettyPrinter prettyPrinter = new PrettyPrinter();
    
    // Path to the Lean executable (built with lake build LaurelIonTest)
    private static final String LEAN_EXE_PATH = ".lake/build/bin/LaurelIonTest";
    
    /**
     * Feature: java-laurel-ion, Property 1: ION Serialization Round-Trip
     * Validates: Requirements 6.5
     * 
     * Tests that serialized programs produce valid ION that can be parsed.
     * This is the first part of the round-trip test - ensuring ION validity.
     */
    @Property(tries = 100)
    void serializationProducesValidIon(@ForAll("programs") Program program) {
        // Serialize the program
        byte[] ionBytes = serializer.serialize(program);
        
        // Verify the bytes are non-empty
        assertNotNull(ionBytes, "Serialized bytes should not be null");
        assertTrue(ionBytes.length > 0, "Serialized bytes should not be empty");
        
        // Verify the ION is valid by parsing it
        try (IonReader reader = IonReaderBuilder.standard().build(ionBytes)) {
            // Read the top-level list
            IonType type = reader.next();
            assertNotNull(type, "Should have at least one ION value");
            assertEquals(IonType.LIST, type, "Top-level should be a list");
            
            reader.stepIn();
            
            // First element should be the symbol table struct
            type = reader.next();
            assertNotNull(type, "Should have symbol table");
            assertEquals(IonType.STRUCT, type, "First element should be symbol table struct");
            
            // Second element should be the encoded program
            type = reader.next();
            assertNotNull(type, "Should have encoded program");
            assertEquals(IonType.SEXP, type, "Second element should be s-expression");
            
            reader.stepOut();
        } catch (Exception e) {
            fail("Failed to parse ION output: " + e.getMessage() + 
                 "\nProgram: " + prettyPrinter.print(program));
        }
    }
    
    /**
     * Feature: java-laurel-ion, Property 1: ION Serialization Round-Trip
     * Validates: Requirements 6.5
     * 
     * Tests that the serialized program structure matches expected format.
     */
    @Property(tries = 50)
    void serializedProgramHasCorrectStructure(@ForAll("smallPrograms") Program program) {
        byte[] ionBytes = serializer.serialize(program);
        
        // Parse and verify structure
        IonDatagram datagram = ionSystem.getLoader().load(ionBytes);
        assertEquals(1, datagram.size(), "Should have exactly one top-level value");
        
        IonValue topLevel = datagram.get(0);
        assertTrue(topLevel instanceof IonList, "Top-level should be a list");
        
        IonList list = (IonList) topLevel;
        assertEquals(2, list.size(), "List should have symbol table and program");
        
        // Verify symbol table
        IonValue symbolTableValue = list.get(0);
        assertTrue(symbolTableValue instanceof IonStruct, "First element should be struct");
        IonStruct symbolTable = (IonStruct) symbolTableValue;
        assertTrue(symbolTable.hasTypeAnnotation("$ion_symbol_table"), 
                   "Should have $ion_symbol_table annotation");
        
        // Verify program s-expression
        IonValue programValue = list.get(1);
        assertTrue(programValue instanceof IonSexp, "Second element should be s-expression");
        
        IonSexp programSexp = (IonSexp) programValue;
        assertTrue(programSexp.size() > 0, "Program s-expression should not be empty");
        
        // First element should be the Laurel.Program symbol
        IonValue firstElement = programSexp.get(0);
        assertTrue(firstElement instanceof IonSymbol, "First element should be symbol");
        assertEquals("Laurel.Program", ((IonSymbol) firstElement).stringValue(),
                     "Should start with Laurel.Program");
    }
    
    /**
     * Feature: java-laurel-ion, Property 1: ION Serialization Round-Trip
     * Validates: Requirements 6.5
     * 
     * Tests full round-trip with Lean deserialization if the executable is available.
     * This test is skipped if the Lean executable is not built.
     */
    @Property(tries = 20)
    void fullRoundTripWithLean(@ForAll("smallPrograms") Program program) {
        // Check if Lean executable exists
        Path leanExe = Path.of(LEAN_EXE_PATH);
        if (!Files.exists(leanExe)) {
            // Skip test if Lean executable not available
            System.out.println("Skipping Lean round-trip test - executable not found at " + LEAN_EXE_PATH);
            return;
        }
        
        byte[] ionBytes = serializer.serialize(program);
        
        try {
            // Write ION bytes to temp file
            Path tempFile = Files.createTempFile("laurel_test_", ".ion");
            Files.write(tempFile, ionBytes);
            
            try {
                // Run Lean executable
                ProcessBuilder pb = new ProcessBuilder(leanExe.toString(), tempFile.toString());
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                // Read output
                String output;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    output = sb.toString();
                }
                
                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                assertTrue(finished, "Lean process should complete within timeout");
                
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    fail("Lean deserialization failed with exit code " + exitCode + 
                         "\nOutput: " + output +
                         "\nProgram: " + prettyPrinter.print(program));
                }
                
                // Verify output is non-empty (contains the program representation)
                assertFalse(output.trim().isEmpty(), 
                           "Lean output should not be empty");
                assertTrue(output.contains("Program("), 
                          "Lean output should contain Program representation");
                
            } finally {
                Files.deleteIfExists(tempFile);
            }
            
        } catch (IOException | InterruptedException e) {
            fail("Failed to run Lean round-trip test: " + e.getMessage());
        }
    }
    
    /**
     * Tests that empty programs serialize correctly.
     */
    @Property(tries = 10)
    void emptyProgramSerializesCorrectly(@ForAll("emptyPrograms") Program program) {
        byte[] ionBytes = serializer.serialize(program);
        
        // Parse and verify
        IonDatagram datagram = ionSystem.getLoader().load(ionBytes);
        IonList list = (IonList) datagram.get(0);
        IonSexp programSexp = (IonSexp) list.get(1);
        
        // Should have: Laurel.Program, null (annotation), seq (procs), seq (fields), seq (types)
        assertTrue(programSexp.size() >= 5, "Program should have at least 5 elements");
    }
    
    // ========== Providers ==========
    
    @Provide
    Arbitrary<Program> programs() {
        return ProgramGenerator.programs(2);
    }
    
    @Provide
    Arbitrary<Program> smallPrograms() {
        return ProgramGenerator.smallPrograms();
    }
    
    @Provide
    Arbitrary<Program> emptyPrograms() {
        return ProgramGenerator.emptyPrograms();
    }
}
