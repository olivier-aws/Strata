/**
 * AST node definitions for the Laurel intermediate verification language.
 * 
 * <p>This package contains immutable record types representing the Laurel AST:
 * <ul>
 *   <li>{@code HighType} - Type hierarchy (primitives, user-defined, applied, pure, intersection)</li>
 *   <li>{@code StmtExpr} - Statement/Expression hierarchy (unified construct)</li>
 *   <li>{@code Operation} - Primitive operations enum</li>
 *   <li>{@code Body} - Procedure body variants</li>
 *   <li>{@code Procedure} - Procedure record with contracts</li>
 *   <li>{@code Parameter} - Parameter record</li>
 *   <li>{@code Field} - Field record</li>
 *   <li>{@code CompositeType} - Composite type record</li>
 *   <li>{@code ConstrainedType} - Constrained type record</li>
 *   <li>{@code TypeDefinition} - Type definition sealed interface</li>
 *   <li>{@code ContractType} - Contract type enum</li>
 *   <li>{@code Program} - Program record</li>
 * </ul>
 * 
 * <p>All AST classes are immutable records or sealed interfaces to ensure
 * type-safe construction and prevent invalid AST states.
 */
package com.strata.laurel.ast;
