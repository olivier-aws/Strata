/-
  Copyright Strata Contributors

  SPDX-License-Identifier: Apache-2.0 OR MIT
-/

import StrataTest.Languages.Laurel.LaurelIon
import Strata.DDM.Util.Ion.Lean

namespace LaurelTest

open LaurelIon (LaurelHighType LaurelOperation LaurelContractType LaurelStmtExpr
                LaurelParameter LaurelBody LaurelProcedure LaurelField
                LaurelCompositeType LaurelConstrainedType LaurelTypeDefinition LaurelProgram)

partial def highTypeToString (t : LaurelHighType) : String :=
  match t with
  | .TVoid => "TVoid"
  | .TBool => "TBool"
  | .TInt => "TInt"
  | .TFloat64 => "TFloat64"
  | .UserDefined name => s!"UserDefined({name})"
  | .Applied base args =>
    let argsStr := args.map highTypeToString |>.intersperse ", " |> String.join
    s!"Applied({highTypeToString base}, [{argsStr}])"
  | .Pure base => s!"Pure({highTypeToString base})"
  | .Intersection types =>
    let typesStr := types.map highTypeToString |>.intersperse ", " |> String.join
    s!"Intersection([{typesStr}])"

def operationToString (op : LaurelOperation) : String :=
  match op with
  | .Eq => "Eq" | .Neq => "Neq" | .And => "And" | .Or => "Or" | .Not => "Not"
  | .Neg => "Neg" | .Add => "Add" | .Sub => "Sub" | .Mul => "Mul" | .Div => "Div"
  | .Mod => "Mod" | .Lt => "Lt" | .Leq => "Leq" | .Gt => "Gt" | .Geq => "Geq"

def contractTypeToString (ct : LaurelContractType) : String :=
  match ct with
  | .Reads => "Reads" | .Modifies => "Modifies"
  | .Precondition => "Precondition" | .PostCondition => "PostCondition"

def optionToString {α} (opt : Option α) (f : α → String) : String :=
  match opt with | none => "None" | some v => s!"Some({f v})"

def listToString {α} (lst : List α) (f : α → String) : String :=
  s!"[{lst.map f |>.intersperse ", " |> String.join}]"

partial def stmtExprToString (e : LaurelStmtExpr) : String :=
  match e with
  | .IfThenElse cond thenB elseB =>
    s!"IfThenElse({stmtExprToString cond}, {stmtExprToString thenB}, {optionToString elseB stmtExprToString})"
  | .Block stmts label =>
    s!"Block({listToString stmts stmtExprToString}, {optionToString label id})"
  | .LocalVariable name type init =>
    s!"LocalVariable({name}, {highTypeToString type}, {optionToString init stmtExprToString})"
  | .While cond inv dec body =>
    s!"While({stmtExprToString cond}, {optionToString inv stmtExprToString}, {optionToString dec stmtExprToString}, {stmtExprToString body})"
  | .Exit target => s!"Exit({target})"
  | .Return value => s!"Return({optionToString value stmtExprToString})"
  | .LiteralInt value => s!"LiteralInt({value})"
  | .LiteralBool value => s!"LiteralBool({value})"
  | .Identifier name => s!"Identifier({name})"
  | .Assign target value => s!"Assign({stmtExprToString target}, {stmtExprToString value})"
  | .FieldSelect target fieldName => s!"FieldSelect({stmtExprToString target}, {fieldName})"
  | .PureFieldUpdate target fieldName newValue =>
    s!"PureFieldUpdate({stmtExprToString target}, {fieldName}, {stmtExprToString newValue})"
  | .StaticCall callee args => s!"StaticCall({callee}, {listToString args stmtExprToString})"
  | .PrimitiveOp op args => s!"PrimitiveOp({operationToString op}, {listToString args stmtExprToString})"
  | .This => "This"
  | .ReferenceEquals lhs rhs => s!"ReferenceEquals({stmtExprToString lhs}, {stmtExprToString rhs})"
  | .AsType target targetType => s!"AsType({stmtExprToString target}, {highTypeToString targetType})"
  | .IsType target type => s!"IsType({stmtExprToString target}, {highTypeToString type})"
  | .InstanceCall target callee args =>
    s!"InstanceCall({stmtExprToString target}, {callee}, {listToString args stmtExprToString})"
  | .Forall name type body => s!"Forall({name}, {highTypeToString type}, {stmtExprToString body})"
  | .Exists name type body => s!"Exists({name}, {highTypeToString type}, {stmtExprToString body})"
  | .Assigned name => s!"Assigned({stmtExprToString name})"
  | .Old value => s!"Old({stmtExprToString value})"
  | .Fresh value => s!"Fresh({stmtExprToString value})"
  | .Assert cond => s!"Assert({stmtExprToString cond})"
  | .Assume cond => s!"Assume({stmtExprToString cond})"
  | .ProveBy value proof => s!"ProveBy({stmtExprToString value}, {stmtExprToString proof})"
  | .ContractOf type func => s!"ContractOf({contractTypeToString type}, {stmtExprToString func})"
  | .Abstract => "Abstract"
  | .All => "All"
  | .Hole => "Hole"

def parameterToString (p : LaurelParameter) : String :=
  s!"Parameter({p.name}, {highTypeToString p.type})"

partial def bodyToString (b : LaurelBody) : String :=
  match b with
  | .Transparent body => s!"Transparent({stmtExprToString body})"
  | .Opaque postcondition impl =>
    s!"Opaque({stmtExprToString postcondition}, {optionToString impl stmtExprToString})"
  | .Abstract postcondition => s!"Abstract({stmtExprToString postcondition})"

partial def procedureToString (p : LaurelProcedure) : String :=
  s!"Procedure({p.name}, {listToString p.inputs parameterToString}, {highTypeToString p.output}, " ++
  s!"{stmtExprToString p.precondition}, {stmtExprToString p.decreases}, {p.deterministic}, " ++
  s!"{optionToString p.reads stmtExprToString}, {stmtExprToString p.modifies}, {bodyToString p.body})"

def fieldToString (f : LaurelField) : String :=
  s!"Field({f.name}, {f.isMutable}, {highTypeToString f.type})"

partial def compositeTypeToString (ct : LaurelCompositeType) : String :=
  s!"CompositeType({ct.name}, {listToString ct.extending id}, " ++
  s!"{listToString ct.fields fieldToString}, {listToString ct.instanceProcedures procedureToString})"

partial def constrainedTypeToString (ct : LaurelConstrainedType) : String :=
  s!"ConstrainedType({ct.name}, {highTypeToString ct.base}, {ct.valueName}, " ++
  s!"{stmtExprToString ct.constraint}, {stmtExprToString ct.witness})"

partial def typeDefinitionToString (td : LaurelTypeDefinition) : String :=
  match td with
  | .Composite ct => s!"Composite({compositeTypeToString ct})"
  | .Constrained ct => s!"Constrained({constrainedTypeToString ct})"

partial def programToString (p : LaurelProgram) : String :=
  s!"Program({listToString p.staticProcedures procedureToString}, " ++
  s!"{listToString p.staticFields fieldToString}, " ++
  s!"{listToString p.types typeDefinitionToString})"

def deserializeProgram (bytes : ByteArray) : Except String LaurelProgram :=
  Strata.FromIon.deserialize bytes

end LaurelTest

def main (args : List String) : IO Unit := do
  if args.length < 1 then
    IO.eprintln "Usage: LaurelIonTest <ion_file_path>"
    return
  let filePath := args[0]!
  let bytes ← IO.FS.readBinFile filePath
  match LaurelTest.deserializeProgram bytes with
  | .ok program =>
    IO.println (LaurelTest.programToString program)
  | .error msg =>
    IO.eprintln s!"Error deserializing ION: {msg}"
