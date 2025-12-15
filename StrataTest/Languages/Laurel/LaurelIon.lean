/-
  Copyright Strata Contributors

  SPDX-License-Identifier: Apache-2.0 OR MIT
-/

/-
This file provides ION serialization/deserialization for Laurel AST types.
It is used for round-trip testing with the Java Laurel ION library.
-/

import Strata.Languages.Laurel.Laurel
import Strata.DDM.Ion
import Strata.DDM.Util.Ion.Lean

-- Namespace to avoid conflicts with Strata types
-- Using LaurelIon instead of Laurel.Ion to avoid conflict with Ion namespace
namespace LaurelIon

-- Aliases to avoid ambiguity with Strata types
abbrev LaurelOperation := _root_.Operation
abbrev LaurelHighType := _root_.HighType
abbrev LaurelContractType := _root_.ContractType
abbrev LaurelStmtExpr := _root_.StmtExpr
abbrev LaurelParameter := _root_.Parameter
abbrev LaurelBody := _root_.Body
abbrev LaurelProcedure := _root_.Procedure
abbrev LaurelField := _root_.Field
abbrev LaurelCompositeType := _root_.CompositeType
abbrev LaurelConstrainedType := _root_.ConstrainedType
abbrev LaurelTypeDefinition := _root_.TypeDefinition
abbrev LaurelProgram := _root_.Program

-- ========== FromIon instances for Laurel types ==========

def highTypeFromIon (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM LaurelHighType := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "HighType" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "HighType" args 3
  match ← Strata.FromIonM.asSymbolString "HighType kind" args[0] with
  | "ident" =>
    let name ← Strata.FromIonM.asSymbolString "HighType name" args[2]
    match name with
    | "Laurel.TVoid" => return .TVoid
    | "Laurel.TBool" => return .TBool
    | "Laurel.TInt" => return .TInt
    | "Laurel.TFloat64" => return .TFloat64
    | "Laurel.UserDefined" =>
      let ⟨p⟩ ← Strata.FromIonM.checkArgMin "UserDefined" args 4
      let nameArg ← Strata.FromIonM.asSexp "UserDefined name" args[3]
      let ⟨np⟩ ← Strata.FromIonM.checkArgMin "UserDefined name sexp" nameArg.val 3
      let typeName ← Strata.FromIonM.asString "UserDefined type name" nameArg.val[2]
      return .UserDefined typeName
    | "Laurel.Applied" =>
      let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Applied" args 4
      let base ← highTypeFromIon args[3]
      let typeArgs ← args.attach.mapM_off (start := 4) fun ⟨e, _⟩ =>
        highTypeFromIon e
      return .Applied base typeArgs.toList
    | "Laurel.Pure" =>
      let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Pure" args 4
      let base ← highTypeFromIon args[3]
      return .Pure base
    | "Laurel.Intersection" =>
      let types ← args.attach.mapM_off (start := 3) fun ⟨e, _⟩ =>
        highTypeFromIon e
      return .Intersection types.toList
    | s => throw s!"Unknown HighType: {s}"
  | s => throw s!"Expected 'ident' for HighType, got: {s}"
termination_by v
decreasing_by
  · have p : sizeOf args[3] < sizeOf args := by decreasing_tactic
    decreasing_tactic
  · have p : sizeOf e < sizeOf args := by decreasing_tactic
    decreasing_tactic
  · have p : sizeOf args[3] < sizeOf args := by decreasing_tactic
    decreasing_tactic
  · have p : sizeOf e < sizeOf args := by decreasing_tactic
    decreasing_tactic

instance : Strata.FromIon LaurelHighType where
  fromIon := highTypeFromIon

def operationFromIon (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM LaurelOperation := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "Operation" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Operation" args 3
  match ← Strata.FromIonM.asSymbolString "Operation kind" args[0] with
  | "ident" =>
    let name ← Strata.FromIonM.asSymbolString "Operation name" args[2]
    match name with
    | "Laurel.Eq" => return .Eq
    | "Laurel.Neq" => return .Neq
    | "Laurel.And" => return .And
    | "Laurel.Or" => return .Or
    | "Laurel.Not" => return .Not
    | "Laurel.Neg" => return .Neg
    | "Laurel.Add" => return .Add
    | "Laurel.Sub" => return .Sub
    | "Laurel.Mul" => return .Mul
    | "Laurel.Div" => return .Div
    | "Laurel.Mod" => return .Mod
    | "Laurel.Lt" => return .Lt
    | "Laurel.Leq" => return .Leq
    | "Laurel.Gt" => return .Gt
    | "Laurel.Geq" => return .Geq
    | s => throw s!"Unknown Operation: {s}"
  | s => throw s!"Expected 'ident' for Operation, got: {s}"

instance : Strata.FromIon LaurelOperation where
  fromIon := operationFromIon

def contractTypeFromIon (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM LaurelContractType := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "ContractType" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "ContractType" args 3
  match ← Strata.FromIonM.asSymbolString "ContractType kind" args[0] with
  | "ident" =>
    let name ← Strata.FromIonM.asSymbolString "ContractType name" args[2]
    match name with
    | "Laurel.Reads" => return .Reads
    | "Laurel.Modifies" => return .Modifies
    | "Laurel.Precondition" => return .Precondition
    | "Laurel.PostCondition" => return .PostCondition
    | s => throw s!"Unknown ContractType: {s}"
  | s => throw s!"Expected 'ident' for ContractType, got: {s}"

instance : Strata.FromIon LaurelContractType where
  fromIon := contractTypeFromIon

-- Helper to parse optional values
def parseOption {α} (v : _root_.Ion.Ion _root_.Ion.SymbolId) (parser : _root_.Ion.Ion _root_.Ion.SymbolId → Strata.FromIonM α) : Strata.FromIonM (Option α) := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "option" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "option" args 2
  match ← Strata.FromIonM.asSymbolString "option kind" args[0] with
  | "option" =>
    if args.size = 2 then
      return none
    else
      some <$> parser args[2]!
  | s => throw s!"Expected 'option', got: {s}"

-- Helper to parse string literals
def parseStrlit (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM String := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "strlit" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "strlit" args 3
  match ← Strata.FromIonM.asSymbolString "strlit kind" args[0] with
  | "strlit" => Strata.FromIonM.asString "strlit value" args[2]
  | s => throw s!"Expected 'strlit', got: {s}"

-- Helper to parse sequences
def parseSeq {α} (v : _root_.Ion.Ion _root_.Ion.SymbolId) (parser : _root_.Ion.Ion _root_.Ion.SymbolId → Strata.FromIonM α) : Strata.FromIonM (List α) := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "seq" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "seq" args 2
  match ← Strata.FromIonM.asSymbolString "seq kind" args[0] with
  | "seq" =>
    let items ← args.attach.mapM_off (start := 2) fun ⟨e, _⟩ => parser e
    return items.toList
  | s => throw s!"Expected 'seq', got: {s}"

-- Helper to parse boolean values
def parseBool (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM Bool := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "bool op" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "bool op" args 2
  match ← Strata.FromIonM.asSymbolString "bool op kind" args[0] with
  | "op" =>
    let ⟨inner, _⟩ ← Strata.FromIonM.asSexp "bool inner" args[1]
    let ⟨ip⟩ ← Strata.FromIonM.checkArgMin "bool inner" inner 1
    let name ← Strata.FromIonM.asSymbolString "bool value" inner[0]
    match name with
    | "Init.true" => return true
    | "Init.false" => return false
    | s => throw s!"Expected Init.true or Init.false, got: {s}"
  | s => throw s!"Expected 'op' for bool, got: {s}"

-- Helper to parse integer literals
def parseInt (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM Int := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "num" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "num" args 3
  match ← Strata.FromIonM.asSymbolString "num kind" args[0] with
  | "num" => Strata.FromIonM.asInt args[2]
  | s => throw s!"Expected 'num', got: {s}"


-- Forward declarations for mutual recursion
mutual
partial def stmtExprFromIon (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM LaurelStmtExpr := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "StmtExpr" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "StmtExpr" args 2
  let name ← Strata.FromIonM.asSymbolString "StmtExpr name" args[0]
  match name with
  | "Laurel.IfThenElse" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "IfThenElse" args 5
    let cond ← stmtExprFromIon args[2]
    let thenBranch ← stmtExprFromIon args[3]
    let elseBranch ← parseOption args[4] stmtExprFromIon
    return .IfThenElse cond thenBranch elseBranch
  | "Laurel.Block" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Block" args 4
    let stmts ← parseSeq args[2] stmtExprFromIon
    let label ← parseOption args[3] parseStrlit
    return .Block stmts label
  | "Laurel.LocalVariable" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "LocalVariable" args 5
    let name ← parseStrlit args[2]
    let type ← highTypeFromIon args[3]
    let init ← parseOption args[4] stmtExprFromIon
    return .LocalVariable name type init
  | "Laurel.While" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "While" args 6
    let cond ← stmtExprFromIon args[2]
    let inv ← parseOption args[3] stmtExprFromIon
    let dec ← parseOption args[4] stmtExprFromIon
    let body ← stmtExprFromIon args[5]
    return .While cond inv dec body
  | "Laurel.Exit" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Exit" args 3
    let target ← parseStrlit args[2]
    return .Exit target
  | "Laurel.Return" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Return" args 3
    let value ← parseOption args[2] stmtExprFromIon
    return .Return value
  | "Laurel.LiteralInt" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "LiteralInt" args 3
    let value ← parseInt args[2]
    return .LiteralInt value
  | "Laurel.LiteralBool" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "LiteralBool" args 3
    let value ← parseBool args[2]
    return .LiteralBool value
  | "Laurel.Identifier" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Identifier" args 3
    let name ← parseStrlit args[2]
    return .Identifier name
  | "Laurel.Assign" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Assign" args 4
    let target ← stmtExprFromIon args[2]
    let value ← stmtExprFromIon args[3]
    return .Assign target value
  | "Laurel.FieldSelect" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "FieldSelect" args 4
    let target ← stmtExprFromIon args[2]
    let fieldName ← parseStrlit args[3]
    return .FieldSelect target fieldName
  | "Laurel.PureFieldUpdate" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "PureFieldUpdate" args 5
    let target ← stmtExprFromIon args[2]
    let fieldName ← parseStrlit args[3]
    let newValue ← stmtExprFromIon args[4]
    return .PureFieldUpdate target fieldName newValue
  | "Laurel.StaticCall" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "StaticCall" args 4
    let callee ← parseStrlit args[2]
    let arguments ← parseSeq args[3] stmtExprFromIon
    return .StaticCall callee arguments
  | "Laurel.PrimitiveOp" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "PrimitiveOp" args 4
    let op ← operationFromIon args[2]
    let arguments ← parseSeq args[3] stmtExprFromIon
    return .PrimitiveOp op arguments
  | "Laurel.This" => return .This
  | "Laurel.ReferenceEquals" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "ReferenceEquals" args 4
    let lhs ← stmtExprFromIon args[2]
    let rhs ← stmtExprFromIon args[3]
    return .ReferenceEquals lhs rhs
  | "Laurel.AsType" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "AsType" args 4
    let target ← stmtExprFromIon args[2]
    let targetType ← highTypeFromIon args[3]
    return .AsType target targetType
  | "Laurel.IsType" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "IsType" args 4
    let target ← stmtExprFromIon args[2]
    let type ← highTypeFromIon args[3]
    return .IsType target type
  | "Laurel.InstanceCall" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "InstanceCall" args 5
    let target ← stmtExprFromIon args[2]
    let callee ← parseStrlit args[3]
    let arguments ← parseSeq args[4] stmtExprFromIon
    return .InstanceCall target callee arguments
  | "Laurel.Forall" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Forall" args 5
    let name ← parseStrlit args[2]
    let type ← highTypeFromIon args[3]
    let body ← stmtExprFromIon args[4]
    return .Forall name type body
  | "Laurel.Exists" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Exists" args 5
    let name ← parseStrlit args[2]
    let type ← highTypeFromIon args[3]
    let body ← stmtExprFromIon args[4]
    return .Exists name type body
  | "Laurel.Assigned" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Assigned" args 3
    let name ← stmtExprFromIon args[2]
    return .Assigned name
  | "Laurel.Old" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Old" args 3
    let value ← stmtExprFromIon args[2]
    return .Old value
  | "Laurel.Fresh" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Fresh" args 3
    let value ← stmtExprFromIon args[2]
    return .Fresh value
  | "Laurel.Assert" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Assert" args 3
    let cond ← stmtExprFromIon args[2]
    return .Assert cond
  | "Laurel.Assume" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Assume" args 3
    let cond ← stmtExprFromIon args[2]
    return .Assume cond
  | "Laurel.ProveBy" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "ProveBy" args 4
    let value ← stmtExprFromIon args[2]
    let proof ← stmtExprFromIon args[3]
    return .ProveBy value proof
  | "Laurel.ContractOf" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "ContractOf" args 4
    let type ← contractTypeFromIon args[2]
    let func ← stmtExprFromIon args[3]
    return .ContractOf type func
  | "Laurel.Abstract" => return .Abstract
  | "Laurel.All" => return .All
  | "Laurel.Hole" => return .Hole
  | s => throw s!"Unknown StmtExpr: {s}"

partial def parameterFromIon (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM LaurelParameter := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "Parameter" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Parameter" args 4
  let name ← Strata.FromIonM.asSymbolString "Parameter name" args[0]
  match name with
  | "Laurel.Parameter" =>
    let paramName ← parseStrlit args[2]
    let paramType ← highTypeFromIon args[3]
    return { name := paramName, type := paramType }
  | s => throw s!"Expected Laurel.Parameter, got: {s}"

partial def bodyFromIon (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM LaurelBody := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "Body" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Body" args 2
  let name ← Strata.FromIonM.asSymbolString "Body name" args[0]
  match name with
  | "Laurel.Transparent" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Transparent" args 3
    let body ← stmtExprFromIon args[2]
    return .Transparent body
  | "Laurel.Opaque" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Opaque" args 4
    let postcondition ← stmtExprFromIon args[2]
    let implementation ← parseOption args[3] stmtExprFromIon
    return .Opaque postcondition implementation
  | "Laurel.Abstract" =>
    let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Abstract body" args 3
    let postcondition ← stmtExprFromIon args[2]
    return .Abstract postcondition
  | s => throw s!"Unknown Body type: {s}"

partial def procedureFromIon (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM LaurelProcedure := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "Procedure" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Procedure" args 11
  let name ← Strata.FromIonM.asSymbolString "Procedure name" args[0]
  match name with
  | "Laurel.Procedure" =>
    let procName ← parseStrlit args[2]
    let inputs ← parseSeq args[3] parameterFromIon
    let output ← highTypeFromIon args[4]
    let precondition ← stmtExprFromIon args[5]
    let decreases ← stmtExprFromIon args[6]
    let deterministic ← parseBool args[7]
    let reads ← parseOption args[8] stmtExprFromIon
    let modifies ← stmtExprFromIon args[9]
    let body ← bodyFromIon args[10]
    return {
      name := procName
      inputs := inputs
      output := output
      precondition := precondition
      decreases := decreases
      deterministic := deterministic
      reads := reads
      modifies := modifies
      body := body
    }
  | s => throw s!"Expected Laurel.Procedure, got: {s}"

partial def fieldFromIon (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM LaurelField := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "Field" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Field" args 5
  let name ← Strata.FromIonM.asSymbolString "Field name" args[0]
  match name with
  | "Laurel.Field" =>
    let fieldName ← parseStrlit args[2]
    let isMutable ← parseBool args[3]
    let fieldType ← highTypeFromIon args[4]
    return { name := fieldName, isMutable := isMutable, type := fieldType }
  | s => throw s!"Expected Laurel.Field, got: {s}"

partial def compositeTypeFromIon (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM LaurelCompositeType := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "CompositeType" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "CompositeType" args 6
  let name ← Strata.FromIonM.asSymbolString "CompositeType name" args[0]
  match name with
  | "Laurel.CompositeType" =>
    let typeName ← parseStrlit args[2]
    let extending ← parseSeq args[3] parseStrlit
    let fields ← parseSeq args[4] fieldFromIon
    let instanceProcedures ← parseSeq args[5] procedureFromIon
    return {
      name := typeName
      extending := extending
      fields := fields
      instanceProcedures := instanceProcedures
    }
  | s => throw s!"Expected Laurel.CompositeType, got: {s}"

partial def constrainedTypeFromIon (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM LaurelConstrainedType := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "ConstrainedType" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "ConstrainedType" args 7
  let name ← Strata.FromIonM.asSymbolString "ConstrainedType name" args[0]
  match name with
  | "Laurel.ConstrainedType" =>
    let typeName ← parseStrlit args[2]
    let base ← highTypeFromIon args[3]
    let valueName ← parseStrlit args[4]
    let constraint ← stmtExprFromIon args[5]
    let witness ← stmtExprFromIon args[6]
    return {
      name := typeName
      base := base
      valueName := valueName
      constraint := constraint
      witness := witness
    }
  | s => throw s!"Expected Laurel.ConstrainedType, got: {s}"

partial def typeDefinitionFromIon (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM LaurelTypeDefinition := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "TypeDefinition" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "TypeDefinition" args 3
  let name ← Strata.FromIonM.asSymbolString "TypeDefinition name" args[0]
  match name with
  | "Laurel.Composite" =>
    let compositeType ← compositeTypeFromIon args[2]
    return .Composite compositeType
  | "Laurel.Constrained" =>
       let constrainedType ← constrainedTypeFromIon args[2]
       return .Constrained constrainedType
  | s => throw s!"Unknown TypeDefinition type: {s}"

partial def programFromIon (v : _root_.Ion.Ion _root_.Ion.SymbolId) : Strata.FromIonM LaurelProgram := do
  let ⟨args, ap⟩ ← Strata.FromIonM.asSexp "Program" v
  let ⟨p⟩ ← Strata.FromIonM.checkArgMin "Program" args 5
  let name ← Strata.FromIonM.asSymbolString "Program name" args[0]
  match name with
  | "Laurel.Program" =>
    let staticProcedures ← parseSeq args[2] procedureFromIon
    let staticFields ← parseSeq args[3] fieldFromIon
    let types ← parseSeq args[4] typeDefinitionFromIon
    return {
      staticProcedures := staticProcedures
      staticFields := staticFields
      types := types
    }
  | s => throw s!"Expected Laurel.Program, got: {s}"
end

instance : Strata.FromIon LaurelStmtExpr where
  fromIon := stmtExprFromIon

instance : Strata.FromIon LaurelParameter where
  fromIon := parameterFromIon

instance : Strata.FromIon LaurelBody where
  fromIon := bodyFromIon

instance : Strata.FromIon LaurelProcedure where
  fromIon := procedureFromIon

instance : Strata.FromIon LaurelField where
  fromIon := fieldFromIon

instance : Strata.FromIon LaurelCompositeType where
  fromIon := compositeTypeFromIon

instance : Strata.FromIon LaurelConstrainedType where
  fromIon := constrainedTypeFromIon

instance : Strata.FromIon LaurelTypeDefinition where
  fromIon := typeDefinitionFromIon

instance : Strata.FromIon LaurelProgram where
  fromIon := programFromIon

end LaurelIon
