/-
  Copyright Strata Contributors

  SPDX-License-Identifier: Apache-2.0 OR MIT
-/
import Strata.Languages.JavaScript.ReadJavaScript
import Strata.DDM.Format

open Strata.JavaScript

-- Test 1: Read and count statements
/--
info: Read 3 top-level statements
-/
#guard_msgs in
#eval show IO Unit from do
  let stmts ← readJavaScriptStrata "Tools/JavaScript/test/basic.ts.st.ion"
    |>.toIO (fun e => .userError e)
  IO.println s!"Read {stmts.size} top-level statements"

-- Test 2: Print the generic Strata IR using the DDM format
/--
info: program JavaScript;
Module(FunctionDecl(abs, Parameter(x, NumberType), NumberType, Block(IfStmt(BinaryExpr(Ge, Identifier(x), NumericLiteral(0)), Block(ReturnStmt(Identifier(x))), Block(ReturnStmt(PrefixUnaryExpr(Neg, Identifier(x)))))))FunctionDecl(max, Parameter(a, NumberType)Parameter(b, NumberType), NumberType, Block(IfStmt(BinaryExpr(Ge, Identifier(a), Identifier(b)), Block(ReturnStmt(Identifier(a))), Block(ReturnStmt(Identifier(b))))))FunctionDecl(sum, Parameter(n, NumberType), NumberType, Block(VariableDecl(s, NumberType, NumericLiteral(0))VariableDecl(i, NumberType, NumericLiteral(0))WhileStmt(BinaryExpr(Lt, Identifier(i), Identifier(n)), Block(AssignStmt(Identifier(i), BinaryExpr(Plus, Identifier(i), NumericLiteral(1)))AssignStmt(Identifier(s), BinaryExpr(Plus, Identifier(s), Identifier(i)))))ReturnStmt(Identifier(s)))))
-/
#guard_msgs in
#eval show IO Unit from do
  let bytes ← IO.FS.readBinFile "Tools/JavaScript/test/basic.ts.st.ion"
  match Strata.Program.fromIon Strata.JavaScript.JavaScript_map Strata.JavaScript.JavaScript.name bytes with
  | .ok pgm => IO.println (toString pgm)
  | .error msg => IO.println s!"Error: {msg}"
