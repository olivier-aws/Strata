/-
  Copyright Strata Contributors

  SPDX-License-Identifier: Apache-2.0 OR MIT
-/
module

import Strata.DDM.Ion
public import Strata.Languages.JavaScript.JavaScriptDialect

public section
namespace Strata.JavaScript

/-- Reads a pre-compiled Strata Ion file containing a JavaScript AST module. -/
private def readJavaScriptStrataBytes (strataPath : String) (bytes : ByteArray)
    : Except String (Array (Strata.JavaScript.stmt Strata.SourceRange)) := do
  if ! Ion.isIonFile bytes then
    throw <| s!"{strataPath} is not an Ion file."
  match Strata.Program.fromIon Strata.JavaScript.JavaScript_map Strata.JavaScript.JavaScript.name bytes with
  | .ok pgm =>
    let jsCmds ← pgm.commands.mapM fun cmd =>
      match Strata.JavaScript.Command.ofAst cmd with
      | .error msg =>
        throw s!"Error reading {strataPath}: {msg}"
      | .ok r => pure r
    let .isTrue _ := inferInstanceAs (Decidable (jsCmds.size = 1))
      | throw s!"Error reading {strataPath}: Expected JavaScript module"
    let .Module _ stmts := jsCmds[0]
    pure stmts.val
  | .error msg =>
    throw s!"Error reading {strataPath}: {msg}"

/--
Runs `npx tsx src/cli.ts js_to_strata` to convert a TypeScript file
into a Strata Ion file, and then reads it in.
-/
def typescriptToStrata (toolDir tsFile : System.FilePath)
    (nodeCmd : String := "npx") :
    EIO String (Array (Strata.JavaScript.stmt Strata.SourceRange)) := do
  let (_handle, strataFile) ←
    match ← IO.FS.createTempFile |>.toBaseIO with
    | .ok p => pure p
    | .error msg =>
      throw s!"Cannot create temporary file: {msg}"
  try
    let spawnArgs : IO.Process.SpawnArgs := {
        cmd := nodeCmd
        args := #["tsx", "src/cli.ts", "js_to_strata",
            tsFile.toString,
            strataFile.toString
          ]
        cwd := some toolDir
        inheritEnv := true
        stdin := .null
        stdout := .piped
        stderr := .piped
    }
    let child ←
            match ← IO.Process.spawn spawnArgs |>.toBaseIO with
            | .ok c => pure c
            | .error msg => throw s!"Could not run Node.js: {msg}"
    let stdout ← IO.asTask child.stdout.readToEnd Task.Priority.dedicated
    let stderr ←
          match ← child.stderr.readToEnd |>.toBaseIO with
          | .ok c => pure c
          | .error msg => throw s!"Could not read stderr from Node.js: {msg}"
    let exitCode ←
          match ← child.wait |>.toBaseIO with
          | .ok c => pure c
          | .error msg => throw s!"Could not wait for process exit code: {msg}"
    let _stdout ←
          match stdout.get with
          | .ok c => pure c
          | .error msg => throw s!"Could not read stdout: {msg}"
    if exitCode ≠ 0 then
      let msg := s!"Internal: Node.js js_to_strata failed (exitCode = {exitCode})\n"
      let msg := s!"{msg}Standard error:\n"
      let msg := stderr.splitOn.foldl (init := msg) fun msg ln => s!"{msg}  {ln}\n"
      throw <| msg
    let bytes ←
          match ← IO.FS.readBinFile strataFile |>.toBaseIO with
          | .ok b => pure b
          | .error msg =>
            throw <| s!"Error reading Strata temp file {strataFile}: {msg}"
    match readJavaScriptStrataBytes strataFile.toString bytes with
    | .ok stmts => pure stmts
    | .error msg => throw msg
  finally
    match ← IO.FS.removeFile strataFile |>.toBaseIO with
    | .ok () => pure ()
    | .error msg => throw s!"Internal: Error deleting temp file {strataFile}: {msg}"

/-- Reads a pre-compiled Strata Ion file containing JavaScript AST statements. -/
def readJavaScriptStrata (strataPath : String)
    : EIO String (Array (Strata.JavaScript.stmt Strata.SourceRange)) := do
  let bytes ←
    match ← IO.FS.readBinFile strataPath |>.toBaseIO with
    | .ok b => pure b
    | .error msg =>
      throw <| s!"Error reading {strataPath}: {msg}"
  match readJavaScriptStrataBytes strataPath bytes with
  | .ok r => pure r
  | .error msg => throw msg

end Strata.JavaScript
end
