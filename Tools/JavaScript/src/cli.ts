#!/usr/bin/env node
/**
 * CLI for Strata JavaScript/TypeScript front-end.
 *
 * Usage:
 *   tsx src/cli.ts dialect <output_dir>
 *   tsx src/cli.ts js_to_strata [--dialect <path>] <input.ts> <output.ts.st.ion>
 */
import fs from "fs";
import path from "path";
import ts from "typescript";
import { serializeToIonText, serializeToIonBinary } from "./base.js";
import { genDialect, Parser } from "./jsast.js";

function writeDialect(outputDir: string) {
  const dialect = genDialect();
  fs.mkdirSync(outputDir, { recursive: true });
  const outputPath = path.join(outputDir, `${dialect.name}.dialect.st.ion`);
  const bytes = serializeToIonBinary(dialect.toIon());
  fs.writeFileSync(outputPath, bytes);
  console.log(`Wrote ${dialect.name} dialect to ${outputPath}`);
}

function jsToStrata(inputPath: string, outputPath: string) {
  const dialect = genDialect();
  const parser = new Parser(dialect);

  // Create a TypeScript program for type checking
  const compilerOptions: ts.CompilerOptions = {
    strict: true,
    target: ts.ScriptTarget.ES2022,
    module: ts.ModuleKind.ES2022,
    noEmit: true,
  };

  const program = ts.createProgram([inputPath], compilerOptions);
  const checker = program.getTypeChecker();
  const sourceFile = program.getSourceFile(inputPath);

  if (!sourceFile) {
    console.error(`Could not read ${inputPath}`);
    process.exit(1);
  }

  // Check for TypeScript errors
  const diagnostics = ts.getPreEmitDiagnostics(program, sourceFile);
  for (const diag of diagnostics) {
    if (diag.category === ts.DiagnosticCategory.Error) {
      const msg = ts.flattenDiagnosticMessageText(diag.messageText, "\n");
      const pos = diag.start !== undefined && diag.file
        ? diag.file.getLineAndCharacterOfPosition(diag.start)
        : null;
      const loc = pos ? `${pos.line + 1}:${pos.character + 1}` : "";
      console.error(`${inputPath}(${loc}): error: ${msg}`);
    }
  }

  const strataProgram = parser.parseModule(sourceFile, checker);

  if (outputPath.endsWith(".txt")) {
    // Text Ion (for debugging)
    const text = serializeToIonText(strataProgram.toIon());
    fs.writeFileSync(outputPath, text, "utf-8");
  } else {
    // Binary Ion (default)
    const bytes = serializeToIonBinary(strataProgram.toIon());
    fs.writeFileSync(outputPath, bytes);
  }
  console.log(`Wrote Strata program to ${outputPath}`);
}

// --- Main ---

const args = process.argv.slice(2);
const command = args[0];

switch (command) {
  case "dialect": {
    const outputDir = args[1];
    if (!outputDir) {
      console.error("Usage: cli.ts dialect <output_dir>");
      process.exit(1);
    }
    writeDialect(outputDir);
    break;
  }
  case "js_to_strata": {
    let inputPath: string;
    let outputPath: string;
    if (args[1] === "--dialect") {
      // --dialect flag is accepted but ignored (we always generate inline)
      inputPath = args[3];
      outputPath = args[4];
    } else {
      inputPath = args[1];
      outputPath = args[2];
    }
    if (!inputPath || !outputPath) {
      console.error("Usage: cli.ts js_to_strata <input.ts> <output.ts.st.ion>");
      process.exit(1);
    }
    jsToStrata(path.resolve(inputPath), outputPath);
    break;
  }
  default:
    console.error("Usage: cli.ts <dialect|js_to_strata> [args...]");
    process.exit(1);
}
