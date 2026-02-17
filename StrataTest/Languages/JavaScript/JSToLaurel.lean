/-
  Copyright Strata Contributors

  SPDX-License-Identifier: Apache-2.0 OR MIT
-/
import Strata.Languages.JavaScript.JavaScript
import Strata.Languages.Laurel.LaurelFormat

open Strata.JavaScript
open Strata.Laurel

private def bodyExpr : Body → Option StmtExprMd
  | .Transparent b => some b
  | _ => none

-- Test: Full round-trip TypeScript → Ion → JS AST → Laurel → print
/--
info: Translated 3 procedures:
  procedure abs(x) -> 1 outputs
  procedure max(a, b) -> 1 outputs
  procedure sum(n) -> 1 outputs

--- abs ---
{ if x >= 0 then { return x } else { return -x } }

--- max ---
{ if a >= b then { return a } else { return b } }

--- sum ---
{ var s: float64 := 0; var i: float64 := 0; while i < n { i := i + 1; s := s + i }; return s }
-/
#guard_msgs in
#eval show IO Unit from do
  let stmts ← readJavaScriptStrata "Tools/JavaScript/test/basic.ts.st.ion"
    |>.toIO (fun e => .userError e)
  match jsToLaurel stmts (filePath := "basic.ts") with
  | .error msg => IO.println s!"Translation error: {msg}"
  | .ok program =>
    IO.println s!"Translated {program.staticProcedures.length} procedures:"
    for proc in program.staticProcedures do
      IO.println s!"  procedure {proc.name}({", ".intercalate (proc.inputs.map fun p => s!"{p.name}")}) -> {proc.outputs.length} outputs"
    IO.println ""
    for proc in program.staticProcedures do
      IO.println s!"--- {proc.name} ---"
      match bodyExpr proc.body with
      | some b => IO.println (toString (formatStmtExpr b))
      | none => IO.println "<opaque>"
      IO.println ""

-- Test: console.assert translates to Laurel Assert, with __main__ wrapper
/--
info: Translated 2 procedures:
  __main__: { assert abs(5) == 5; assert abs(-3) == 3; assert abs(0) == 0 }
  abs: { if x >= 0 then { return x } else { return -x } }
-/
#guard_msgs in
#eval show IO Unit from do
  let stmts ← readJavaScriptStrata "Tools/JavaScript/test/assert.ts.st.ion"
    |>.toIO (fun e => .userError e)
  match jsToLaurel stmts (filePath := "assert.ts") with
  | .error msg => IO.println s!"Translation error: {msg}"
  | .ok program =>
    IO.println s!"Translated {program.staticProcedures.length} procedures:"
    for proc in program.staticProcedures do
      let bodyStr := match bodyExpr proc.body with
        | some b => toString (formatStmtExpr b)
        | none => "<opaque>"
      IO.println s!"  {proc.name}: {bodyStr}"
