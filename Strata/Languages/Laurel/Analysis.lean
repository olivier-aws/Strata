/-
  Copyright Strata Contributors

  SPDX-License-Identifier: Apache-2.0 OR MIT
-/

import Strata.Languages.Laurel.LaurelToCoreTranslator
import Strata.Languages.Laurel.LaurelFormat
import Strata.Languages.Core.Verifier
import Strata.DL.Imperative.MetaData

/-!
# Shared Laurel Analysis Framework

Common infrastructure for language-specific analysis commands that go through
the Laurel pipeline: Source AST → Laurel → Core → VCG → SMT.

Eliminates duplication between `pyAnalyzeLaurelCommand`, `jsAnalyzeLaurelCommand`,
and future language analysis commands.
-/

namespace Strata.Analysis

open Strata

-- ============================================================
-- Source file resolution
-- ============================================================

/-- Information about a source file used for mapping byte offsets to line/column. -/
structure SourceFileInfo where
  path : String
  fileMap : Lean.FileMap

/-- Try to find and read a source file corresponding to an Ion file path.
    Strips `.st.ion` from the path and tries each candidate extension.

    Examples:
    - `tryReadSourceFile "foo.ts.st.ion" [""]` → tries `foo.ts`
    - `tryReadSourceFile "foo.st.ion" [".ts", ".js"]` → tries `foo.ts`, `foo.js`
    - `tryReadSourceFile "foo.python.st.ion" [".py"]` with strip `".python.st.ion"` → tries `foo.py`
-/
def tryReadSourceFile (ionPath : String) (stripSuffix : String := ".st.ion")
    (extraExtensions : List String := []) : IO (Option SourceFileInfo) := do
  let base := if ionPath.endsWith stripSuffix
              then (ionPath.dropEnd stripSuffix.length).toString
              else ionPath
  let candidates := [base] ++ extraExtensions.map (base ++ ·)
  for path in candidates do
    try
      let content ← IO.FS.readFile path
      return some { path, fileMap := Lean.FileMap.ofString content }
    catch _ => pure ()
  return none

/-- Resolve the source path for metadata: use the actual source file if found,
    otherwise fall back to the Ion file path. -/
def resolveSourcePath (sourceOpt : Option SourceFileInfo) (ionPath : String) : String :=
  match sourceOpt with
  | some info => info.path
  | none => ionPath

-- ============================================================
-- Verification result formatting
-- ============================================================

/-- Format a single verification result with source location information.
    Produces output like:
    - `assert(97): ✅ pass (at line 9, col 0)`
    - `Assertion failed at line 5, col 0: assert(53): ❌ fail`
-/
def formatVcResult (vcResult : Core.VCResult) (sourceOpt : Option SourceFileInfo) : String :=
  let (locationPrefix, locationSuffix) := match Imperative.getFileRange vcResult.obligation.metadata with
    | some fr =>
      if fr.range.isNone then ("", "")
      else
        match sourceOpt with
        | some info =>
          match fr.file with
          | .file path =>
            if path == info.path then
              let pos := info.fileMap.toPosition fr.range.start
              match vcResult.result with
              | .fail => (s!"Assertion failed at line {pos.line}, col {pos.column}: ", "")
              | _ => ("", s!" (at line {pos.line}, col {pos.column})")
            else
              match vcResult.result with
              | .fail => (s!"Assertion failed at byte {fr.range.start}: ", "")
              | _ => ("", s!" (at byte {fr.range.start})")
        | none =>
          match vcResult.result with
          | .fail => (s!"Assertion failed at byte {fr.range.start}: ", "")
          | _ => ("", s!" (at byte {fr.range.start})")
    | none => ("", "")
  s!"{locationPrefix}{vcResult.obligation.label}: {Std.format vcResult.result}{locationSuffix}"

/-- Format all verification results. -/
def formatVcResults (vcResults : Core.VCResults) (sourceOpt : Option SourceFileInfo) : String :=
  let lines := vcResults.map (formatVcResult · sourceOpt)
  "\n".intercalate lines.toList

-- ============================================================
-- Laurel analysis pipeline
-- ============================================================

/-- Run the Laurel → Core → VCG → SMT pipeline on a Laurel program.
    This is the shared backend for all language-specific analysis commands. -/
def analyzeLaurelProgram
    (laurelProgram : Laurel.Program)
    (sourceOpt : Option SourceFileInfo)
    (verbose : Bool)
    (preludeDecls : List Core.Decl := [])
    (solver : String := "z3") : IO Unit := do
  if verbose then
    IO.println "\n==== Laurel Program ===="
    IO.println f!"{laurelProgram}"

  match Strata.Laurel.translate laurelProgram with
  | .error diagnostics =>
    throw <| IO.Error.userError s!"Laurel to Core translation failed: {diagnostics.map (·.message)}"
  | .ok (coreProgram, _) =>
    let coreProgram := { decls := preludeDecls ++ coreProgram.decls }
    if verbose then
      IO.println "\n==== Core Program ===="
      IO.print coreProgram

    let verboseMode := VerboseMode.ofBool verbose
    let vcResults ← IO.FS.withTempDir (fun tempDir =>
        EIO.toIO
          (fun f => IO.Error.userError (toString f))
          (Core.verify coreProgram tempDir .none
            { Options.default with
              stopOnFirstError := false,
              verbose := verboseMode,
              removeIrrelevantAxioms := true,
              solver := solver }))

    IO.println "\n==== Verification Results ===="
    IO.println (formatVcResults vcResults sourceOpt)

end Strata.Analysis
