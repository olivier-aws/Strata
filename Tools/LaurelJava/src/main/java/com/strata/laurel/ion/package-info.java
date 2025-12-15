/**
 * ION serialization for Laurel ASTs.
 * 
 * <p>This package provides serialization of Laurel ASTs to Amazon ION binary
 * format, compatible with the Lean Strata framework's FromIon deserialization:
 * <ul>
 *   <li>{@code IonSerializer} - Main serialization entry point</li>
 *   <li>{@code SymbolTableBuilder} - Collects and interns symbols</li>
 *   <li>{@code SExprEncoder} - Encodes AST nodes to ION s-expressions</li>
 * </ul>
 * 
 * <p>The serialization format matches exactly what the Lean FromIon
 * implementation expects, enabling interoperability between Java and Lean.
 */
package com.strata.laurel.ion;
