/-
  Copyright Strata Contributors

  SPDX-License-Identifier: Apache-2.0 OR MIT
-/

import Strata.Languages.Laurel.Laurel
import Strata.Languages.Laurel.Grammar.LaurelGrammar
import Strata.DDM.AST
import Strata.DDM.Ion

/-!
# Laurel Abstract AST to DDM Ion Serialization

Converts a `Laurel.Program` (the abstract Lean AST) into Strata DDM `Operation`
nodes matching the Laurel grammar, then serializes to Ion binary.
-/

namespace Strata.Laurel.ToIon

open Strata

private abbrev q (name : String) : QualifiedIdent := ⟨"Laurel", name⟩
private def noAnn : SourceRange := ⟨0, 0⟩

private def mkOp (name : String) (args : Array Arg) (ann := noAnn) : Arg :=
  .op { ann, name := q name, args }

private def mkIdent (s : String) : Arg := .ident noAnn s
private def mkNum (n : Int) : Arg :=
  if n >= 0 then .num noAnn n.toNat
  else mkOp "neg" #[.num noAnn (-n).toNat]
private def mkSeq (args : Array Arg) : Arg := .seq noAnn .none args
private def mkOption (v : Option Arg) : Arg := .option noAnn v
private def mkCommaSep (args : Array Arg) : Arg := .seq noAnn .comma args

-- ============================================================
-- Types
-- ============================================================

partial def typeToArg (t : HighTypeMd) : Arg :=
  match t.val with
  | .TInt => mkOp "intType" #[]
  | .TBool => mkOp "boolType" #[]
  | .TString => mkOp "stringType" #[]
  | .TFloat64 => mkOp "compositeType" #[mkIdent "float64"]
  | .TVoid => mkOp "compositeType" #[mkIdent "void"]
  | .UserDefined name => mkOp "compositeType" #[mkIdent name]
  | .TCore name => mkOp "compositeType" #[mkIdent name]
  | _ => mkOp "compositeType" #[mkIdent "unknown"]

-- ============================================================
-- Expressions / Statements
-- ============================================================

private def opName : Laurel.Operation → String
  | .Eq => "eq" | .Neq => "neq"
  | .And => "and" | .Or => "or" | .Not => "not" | .Implies => "implies"
  | .Neg => "neg" | .Add => "add" | .Sub => "sub" | .Mul => "mul"
  | .Div => "div" | .Mod => "mod" | .DivT => "divT" | .ModT => "modT"
  | .Lt => "lt" | .Leq => "le" | .Gt => "gt" | .Geq => "ge"

mutual

partial def stmtExprToArg (e : StmtExprMd) : Arg := stmtExprValToArg e.val

partial def stmtExprValToArg : StmtExpr → Arg
  | .LiteralBool b => mkOp "literalBool" #[if b then .op ⟨noAnn, ⟨"Init", "boolTrue"⟩, #[]⟩
                                                  else .op ⟨noAnn, ⟨"Init", "boolFalse"⟩, #[]⟩]
  | .LiteralInt n => mkOp "int" #[mkNum n]
  | .LiteralString s => mkOp "string" #[.strlit noAnn s]
  | .Identifier name => mkOp "identifier" #[mkIdent name]
  | .PrimitiveOp op args =>
    match op, args with
    | .Not, [a] => mkOp "not" #[stmtExprToArg a]
    | .Neg, [a] => mkOp "neg" #[stmtExprToArg a]
    | _, [l, r] => mkOp (opName op) #[stmtExprToArg l, stmtExprToArg r]
    | _, _ => mkOp "identifier" #[mkIdent "unsupported_op"]
  | .Assign targets value =>
    match targets with
    | [t] => mkOp "assign" #[stmtExprToArg t, stmtExprToArg value]
    | _ => mkOp "identifier" #[mkIdent "unsupported_multi_assign"]
  | .LocalVariable name ty init =>
    mkOp "varDecl" #[
      mkIdent name,
      mkOption (some (mkOp "optionalType" #[typeToArg ty])),
      mkOption (init.map fun e => mkOp "optionalAssignment" #[stmtExprToArg e])
    ]
  | .IfThenElse cond thenB elseB =>
    mkOp "ifThenElse" #[
      stmtExprToArg cond,
      stmtExprToArg thenB,
      mkOption (elseB.map fun e => mkOp "optionalElse" #[stmtExprToArg e])
    ]
  | .While cond invs _ body =>
    mkOp "while" #[
      stmtExprToArg cond,
      mkSeq (invs.toArray.map fun i => mkOp "invariantClause" #[stmtExprToArg i]),
      stmtExprToArg body
    ]
  | .Block stmts _ =>
    mkOp "block" #[mkSeq (stmts.toArray.map stmtExprToArg)]
  | .Return (some v) => mkOp "return" #[stmtExprToArg v]
  | .Return none => mkOp "return" #[mkOp "identifier" #[mkIdent "void"]]
  | .Assert cond => mkOp "assert" #[stmtExprToArg cond]
  | .Assume cond => mkOp "assume" #[stmtExprToArg cond]
  | .StaticCall name args =>
    mkOp "call" #[
      mkOp "identifier" #[mkIdent name],
      mkCommaSep (args.toArray.map stmtExprToArg)
    ]
  | .InstanceCall target method args =>
    mkOp "call" #[
      mkOp "fieldAccess" #[stmtExprToArg target, mkIdent method],
      mkCommaSep (args.toArray.map stmtExprToArg)
    ]
  | .FieldSelect target field =>
    mkOp "fieldAccess" #[stmtExprToArg target, mkIdent field]
  | .Forall name ty body =>
    mkOp "forallExpr" #[mkIdent name, typeToArg ty, stmtExprToArg body]
  | .Exists name ty body =>
    mkOp "existsExpr" #[mkIdent name, typeToArg ty, stmtExprToArg body]
  | .Hole => mkOp "identifier" #[mkIdent "__hole__"]
  | _ => mkOp "identifier" #[mkIdent "__unsupported__"]

end

-- ============================================================
-- Parameters and Procedures
-- ============================================================

def parameterToArg (p : Laurel.Parameter) : Arg :=
  mkOp "parameter" #[mkIdent p.name, typeToArg p.type]

def procedureToArg (proc : Laurel.Procedure) : Arg :=
  let params := mkCommaSep (proc.inputs.toArray.map parameterToArg)
  let retType := match proc.outputs with
    | [o] => mkOption (some (mkOp "optionalReturnType" #[typeToArg o.type]))
    | _ => mkOption none
  let retParams := match proc.outputs with
    | [] => mkOption none
    | outs => mkOption (some (mkOp "returnParameters" #[
        mkCommaSep (outs.toArray.map parameterToArg)]))
  let requires := match proc.precondition.val with
    | .LiteralBool true => mkOption none
    | _ => mkOption (some (mkOp "optionalRequires" #[stmtExprToArg proc.precondition]))
  let ensures := match proc.body with
    | .Opaque posts _ _ => mkSeq (posts.toArray.map fun p =>
        mkOp "ensuresClause" #[stmtExprToArg p])
    | _ => mkSeq #[]
  let modifies := match proc.body with
    | .Opaque _ _ mods => mkSeq (mods.toArray.map fun m =>
        mkOp "modifiesClause" #[mkCommaSep #[stmtExprToArg m]])
    | _ => mkSeq #[]
  let body := match proc.body with
    | .Transparent b => mkOption (some (mkOp "optionalBody" #[stmtExprToArg b]))
    | .Opaque _ (some impl) _ => mkOption (some (mkOp "optionalBody" #[stmtExprToArg impl]))
    | _ => mkOption none
  mkOp "procedure" #[mkIdent proc.name, params, retType, retParams, requires, ensures, modifies, body]

-- ============================================================
-- Program
-- ============================================================

def programToStrata (prog : Laurel.Program) : Strata.Program :=
  let topLevels := prog.staticProcedures.toArray.map fun proc =>
    mkOp "topLevelProcedure" #[procedureToArg proc]
  let moduleOp : Strata.Operation := {
    ann := noAnn
    name := q "program"
    args := #[mkSeq topLevels]
  }
  { dialects := Laurel_map
    dialect := "Laurel"
    commands := #[moduleOp] }

def programToIonBytes (prog : Laurel.Program) : ByteArray :=
  (programToStrata prog).toIon

end Strata.Laurel.ToIon
