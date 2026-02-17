/-
  Copyright Strata Contributors

  SPDX-License-Identifier: Apache-2.0 OR MIT
-/

import Strata.Languages.Laurel.Laurel
import Strata.Languages.JavaScript.JavaScriptDialect
import Strata.DL.Imperative.MetaData
import Strata.Languages.Core.Expressions

/-!
# JavaScript/TypeScript to Laurel Translation

Translates the auto-generated JavaScript AST (from `#strata_gen`) into
the Laurel intermediate representation for verification.

## Supported subset (Milestone 1)
- Function declarations with typed parameters and return types
- Variable declarations with initializers
- Assignments
- `if`/`else`, `while`, blocks
- `return`, `console.assert()`
- Arithmetic, comparisons, boolean operators, unary minus/not
- Function calls
-/

namespace Strata.JavaScript

open Laurel
open Strata (SourceRange Ann)

-- ============================================================
-- Helpers
-- ============================================================

private def defaultMd : Imperative.MetaData Core.Expression :=
  let elt := ⟨ Imperative.MetaDataElem.Field.label "fileRange",
               .fileRange ⟨ ⟨"js"⟩, 0, 0 ⟩ ⟩
  #[elt]

private def srToMd (filePath : String) (sr : SourceRange) : Imperative.MetaData Core.Expression :=
  let elt := ⟨ Imperative.MetaData.fileRange,
               .fileRange ⟨ .file filePath, sr ⟩ ⟩
  #[elt]

private def mkTy (ty : HighType) : HighTypeMd := { val := ty, md := defaultMd }
private def mkE (e : StmtExpr) : StmtExprMd := { val := e, md := defaultMd }
private def mkELoc (e : StmtExpr) (md : Imperative.MetaData Core.Expression) : StmtExprMd :=
  { val := e, md := md }

-- ============================================================
-- Type translation
-- ============================================================

def translateType (t : typeAnnotation SourceRange) : HighTypeMd :=
  match t with
  | .NumberType _  => mkTy .TFloat64
  | .BooleanType _ => mkTy .TBool
  | .StringType _  => mkTy .TString
  | .VoidType _    => mkTy .TVoid
  | .BigIntType _  => mkTy .TInt
  | .AnyType _     => mkTy (.TCore "JsAny")
  | .NamedType _ n => mkTy (.UserDefined n.val)

-- ============================================================
-- Expression translation
-- ============================================================

private def binOpToLaurel (op : String) : Option Laurel.Operation :=
  match op with
  | "Plus"     => some .Add
  | "Minus"    => some .Sub
  | "Star"     => some .Mul
  | "Slash"    => some .Div
  | "Percent"  => some .Mod
  | "StrictEq" => some .Eq
  | "StrictNeq"=> some .Neq
  | "Lt"       => some .Lt
  | "Le"       => some .Leq
  | "Gt"       => some .Gt
  | "Ge"       => some .Geq
  | "And"      => some .And
  | "Or"       => some .Or
  | _          => none

private def prefixOpToLaurel (op : String) : Option Laurel.Operation :=
  match op with
  | "Not" => some .Not
  | "Neg" => some .Neg
  | _     => none

mutual

partial def translateExpr (fp : String) (e : expr SourceRange)
    : Except String StmtExprMd := do
  match e with
  | .NumericLiteral _ n =>
    return mkE (.LiteralInt n.val)
  | .BigIntLiteral _ n =>
    return mkE (.LiteralInt n.val)
  | .StringLiteral _ s =>
    return mkE (.LiteralString s.val)
  | .TrueLiteral _ =>
    return mkE (.LiteralBool true)
  | .FalseLiteral _ =>
    return mkE (.LiteralBool false)
  | .Identifier _ name =>
    return mkE (.Identifier name.val)
  | .BinaryExpr sr op lhs rhs => do
    let lhsE ← translateExpr fp lhs
    let rhsE ← translateExpr fp rhs
    match binOpToLaurel op.val with
    | some laurelOp =>
      return mkELoc (.PrimitiveOp laurelOp [lhsE, rhsE]) (srToMd fp sr)
    | none =>
      throw s!"Unsupported binary operator: {op.val}"
  | .PrefixUnaryExpr sr op operand => do
    let operandE ← translateExpr fp operand
    match prefixOpToLaurel op.val with
    | some laurelOp =>
      return mkELoc (.PrimitiveOp laurelOp [operandE]) (srToMd fp sr)
    | none =>
      throw s!"Unsupported prefix operator: {op.val}"
  | .CallExpr sr callee args => do
    let argsE ← args.val.toList.mapM (translateExpr fp)
    -- Extract function name for StaticCall
    match callee with
    | .Identifier _ name =>
      return mkELoc (.StaticCall name.val argsE) (srToMd fp sr)
    | .PropertyAccessExpr _ obj method =>
      let objE ← translateExpr fp obj
      return mkELoc (.InstanceCall objE method.val argsE) (srToMd fp sr)
    | _ =>
      return mkE .Hole
  | .PropertyAccessExpr sr obj field => do
    let objE ← translateExpr fp obj
    return mkELoc (.FieldSelect objE field.val) (srToMd fp sr)
  | .ConditionalExpr sr cond thenE elseE => do
    let condE ← translateExpr fp cond
    let thenV ← translateExpr fp thenE
    let elseV ← translateExpr fp elseE
    return mkELoc (.IfThenElse condE thenV (some elseV)) (srToMd fp sr)
  | .ParenExpr _ inner =>
    translateExpr fp inner

-- ============================================================
-- Statement translation
-- ============================================================

partial def translateStmt (fp : String) (s : stmt SourceRange)
    : Except String StmtExprMd := do
  match s with
  | .VariableDecl sr name ty init => do
    let laurelTy := translateType ty
    let initE ← translateExpr fp init
    return mkELoc (.LocalVariable name.val laurelTy (some initE)) (srToMd fp sr)

  | .AssignStmt sr target value => do
    let targetE ← translateExpr fp target
    let valueE ← translateExpr fp value
    return mkELoc (.Assign [targetE] valueE) (srToMd fp sr)

  | .ExpressionStmt _ e => do
    translateExpr fp e

  | .ReturnStmt sr e => do
    let eE ← translateExpr fp e
    return mkELoc (.Return (some eE)) (srToMd fp sr)

  | .AssertStmt sr cond => do
    let condE ← translateExpr fp cond
    return mkELoc (.Assert condE) (srToMd fp sr)

  | .IfStmt sr cond thenBranch elseBranch => do
    let condE ← translateExpr fp cond
    let thenE ← translateStmt fp thenBranch
    let elseE ← translateStmt fp elseBranch
    -- Check if else is an empty block
    let elseOpt := match elseBranch with
      | .Block _ stmts => if stmts.val.isEmpty then none else some elseE
      | _ => some elseE
    return mkELoc (.IfThenElse condE thenE elseOpt) (srToMd fp sr)

  | .WhileStmt sr cond body => do
    let condE ← translateExpr fp cond
    let bodyE ← translateStmt fp body
    return mkELoc (.While condE [] none bodyE) (srToMd fp sr)

  | .Block sr stmts => do
    let stmtsE ← stmts.val.toList.mapM (translateStmt fp)
    return mkELoc (.Block stmtsE none) (srToMd fp sr)

  | .FunctionDecl _ _ _ _ _ =>
    -- Handled at module level
    throw "FunctionDecl should be handled at module level"

end

-- ============================================================
-- Function translation
-- ============================================================

def translateFunction (fp : String) (s : stmt SourceRange)
    : Except String Laurel.Procedure := do
  match s with
  | .FunctionDecl _ name params returnType body => do
    let inputs : List Laurel.Parameter := params.val.toList.map fun p =>
      match p with
      | .Parameter _ pName pType =>
        { name := pName.val, type := translateType pType }

    let retTy := translateType returnType
    let outputs : List Laurel.Parameter :=
      match retTy.val with
      | .TVoid => []
      | _ => [{ name := "result", type := retTy }]

    let bodyE ← translateStmt fp body

    return {
      name := name.val
      inputs := inputs
      outputs := outputs
      precondition := mkE (.LiteralBool true)
      determinism := .deterministic none
      decreases := none
      body := .Transparent bodyE
      md := defaultMd
    }
  | _ => throw "Expected FunctionDecl"

-- ============================================================
-- Module translation
-- ============================================================

/-- Translate a JavaScript/TypeScript module to a Laurel Program. -/
def jsToLaurel (stmts : Array (stmt SourceRange)) (filePath : String := "")
    : Except String Laurel.Program := do
  let mut procedures : List Laurel.Procedure := []
  let mut topLevelStmts : List StmtExprMd := []

  for s in stmts do
    match s with
    | .FunctionDecl _ _ _ _ _ =>
      let proc ← translateFunction filePath s
      procedures := procedures ++ [proc]
    | _ =>
      let e ← translateStmt filePath s
      topLevelStmts := topLevelStmts ++ [e]

  -- If there are top-level statements, wrap them in a __main__ procedure
  if !topLevelStmts.isEmpty then
    let mainBody := mkE (.Block topLevelStmts none)
    let mainProc : Laurel.Procedure := {
      name := "__main__"
      inputs := []
      outputs := []
      precondition := mkE (.LiteralBool true)
      determinism := .deterministic none
      decreases := none
      body := .Transparent mainBody
      md := defaultMd
    }
    procedures := mainProc :: procedures

  return {
    staticProcedures := procedures
    staticFields := []
    types := []
    constants := []
  }

end Strata.JavaScript
