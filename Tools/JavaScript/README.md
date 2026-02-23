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

## Architecture

The tool has three components:

1. **`src/base.ts`** — Core Strata AST datatypes and Ion serialization (mirrors `../Python/strata/base.py`)
2. **`src/jsast.ts`** — JavaScript DDM dialect definition and TypeScript AST → Strata translation (mirrors `../Python/strata/pythonast.py`)
3. **`src/cli.ts`** — CLI entry point (mirrors `../Python/strata/gen.py`)

## Dialect Generation

The JavaScript DDM dialect is defined programmatically in `src/jsast.ts`
(the `genDialect()` function). It is **not** hand-written — the committed
`dialects/JavaScript.dialect.st.ion` file is generated output.

To regenerate after modifying `genDialect()`:

```bash
./scripts/gen_dialect.sh
```

Or manually:

```bash
npx tsx src/cli.ts dialect dialects
```

After regenerating, you must rebuild the Lean dialect module:

```bash
# From the Strata repo root:
rm -f .lake/build/lib/lean/Strata/Languages/JavaScript/JavaScriptDialect.olean
lake build Strata.Languages.JavaScript.JavaScriptDialect
```

The Lean file `Strata/Languages/JavaScript/JavaScriptDialect.lean` loads
the binary Ion dialect via `#load_dialect` and auto-generates typed Lean
inductive types via `#strata_gen JavaScript`.

## Parsing TypeScript into Strata

```bash
npx tsx src/cli.ts js_to_strata input.ts output.ts.st.ion
```

Use `.txt` extension for text Ion (useful for debugging), any other
extension for binary Ion.

## Supported TypeScript Subset

See `StrataTest/Languages/JavaScript/TS_FEATURE_COVERAGE.md` for the
full feature coverage matrix.
