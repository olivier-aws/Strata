/*
 * Copyright Strata Contributors
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */
package com.strata.laurel.ion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds and manages a symbol table for ION serialization.
 * 
 * <p>The symbol table collects and interns symbols during serialization,
 * assigning each unique symbol a numeric ID. This allows the ION output
 * to use compact symbol IDs instead of full strings.
 * 
 * <p>The symbol table is written as a local symbol table annotation at
 * the beginning of the ION output.
 */
public class SymbolTableBuilder {
    
    // LinkedHashMap preserves insertion order for deterministic output
    private final Map<String, Integer> symbolToId = new LinkedHashMap<>();
    private final List<String> symbols = new ArrayList<>();
    
    /**
     * Creates a new empty symbol table builder.
     */
    public SymbolTableBuilder() {
        // Start with system symbols offset
        // ION system symbols start at ID 1, local symbols start after
    }
    
    /**
     * Interns a symbol and returns its ID.
     * 
     * <p>If the symbol has already been interned, returns the existing ID.
     * Otherwise, adds the symbol to the table and returns a new ID.
     * 
     * @param symbol the symbol string to intern
     * @return the symbol ID
     */
    public int intern(String symbol) {
        return symbolToId.computeIfAbsent(symbol, s -> {
            int id = symbols.size();
            symbols.add(s);
            return id;
        });
    }
    
    /**
     * Checks if a symbol has already been interned.
     * 
     * @param symbol the symbol to check
     * @return true if the symbol is in the table
     */
    public boolean contains(String symbol) {
        return symbolToId.containsKey(symbol);
    }
    
    /**
     * Gets the ID of an already-interned symbol.
     * 
     * @param symbol the symbol to look up
     * @return the symbol ID, or -1 if not found
     */
    public int getId(String symbol) {
        return symbolToId.getOrDefault(symbol, -1);
    }
    
    /**
     * Returns the list of symbols in insertion order.
     * 
     * @return unmodifiable list of symbols
     */
    public List<String> getSymbols() {
        return List.copyOf(symbols);
    }
    
    /**
     * Returns the number of symbols in the table.
     * 
     * @return the symbol count
     */
    public int size() {
        return symbols.size();
    }
    
    /**
     * Clears all symbols from the table.
     */
    public void clear() {
        symbolToId.clear();
        symbols.clear();
    }
}
