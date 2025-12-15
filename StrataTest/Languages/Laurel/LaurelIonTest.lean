/-
  Copyright Strata Contributors
  SPDX-License-Identifier: Apache-2.0 OR MIT
-/

import StrataTest.Languages.Laurel.LaurelIon

namespace LaurelTest

open LaurelIon (LaurelProgram)

def deserializeProgram (bytes : ByteArray) : Except String LaurelProgram :=
  Strata.FromIon.deserialize bytes

partial def programToString (p : LaurelProgram) : String :=
  s!"Program(staticProcedures={p.staticProcedures.length}, staticFields={p.staticFields.length}, types={p.types.length})"

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

