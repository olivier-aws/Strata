/-
  Copyright Strata Contributors

  SPDX-License-Identifier: Apache-2.0 OR MIT
-/
module

public import Strata.DDM.Integration.Lean

import Strata.DDM.AST
import Strata.DDM.Util.ByteArray
import Strata.DDM.Format
import Strata.DDM.BuiltinDialects.Init
public import Strata.DDM.Integration.Lean.OfAstM

public section
namespace Strata.JavaScript
#load_dialect "../../../Tools/JavaScript/dialects/JavaScript.dialect.st.ion"

#strata_gen JavaScript

end Strata.JavaScript
end
