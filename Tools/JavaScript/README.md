# Strata JavaScript/TypeScript Front-End

This directory contains a Node.js tool for parsing TypeScript files into
Strata's Ion-based format, following the Python model.

## Prerequisites

- Node.js 20+
- npm

## Installation

```bash
npm install
```

## Generating the DDM dialect

```bash
npx tsx src/cli.ts dialect dialects
```

This writes `dialects/JavaScript.dialect.st.ion`.

## Parsing TypeScript into Strata

```bash
npx tsx src/cli.ts js_to_strata input.ts output.ts.st.ion
```

Use `.ion` extension for binary Ion, any other extension for text Ion (useful for debugging).

## Supported TypeScript subset

- Function declarations with typed parameters and return types
- Variable declarations (`let`/`const`) with type annotations and initializers
- Assignments
- `if`/`else`, `while`, blocks
- `return` statements
- `console.assert()` → translated to `AssertStmt`
- Arithmetic: `+`, `-`, `*`, `/`, `%`
- Comparisons: `===`, `!==`, `<`, `<=`, `>`, `>=`
- Boolean operators: `&&`, `||`, `!`
- Unary minus
- Function calls
- Property access (`obj.field`)
- Ternary expressions (`c ? a : b`)
- Parenthesized expressions
- Type annotations: `number`, `boolean`, `string`, `void`, `bigint`, named types

## Architecture

Following the Python model from `Tools/Python/`:

1. **`src/base.ts`** — Core Strata AST datatypes and Ion serialization (mirrors `strata/base.py`)
2. **`src/jsast.ts`** — JavaScript dialect definition and TypeScript AST → Strata translation (mirrors `strata/pythonast.py`)
3. **`src/cli.ts`** — CLI entry point (mirrors `strata/gen.py`)

The tool uses the TypeScript Compiler API for parsing and type checking,
giving us full type information on every AST node.
