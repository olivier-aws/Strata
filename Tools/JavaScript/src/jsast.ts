/**
 * TypeScript AST dialect generation and parsing.
 * Mirrors the Python strata/pythonast.py module.
 *
 * We define a hand-written subset dialect covering the TypeScript AST nodes
 * needed for Milestone 1 (functions, basic control flow, arithmetic, assertions).
 * Unlike Python's approach of introspecting the ast module, we manually define
 * the dialect because TypeScript's AST is much larger and more complex.
 */
import ts from "typescript";
import {
  Dialect,
  ArgDecl,
  OpDecl,
  Operation,
  Program,
  Init,
  Ident,
  NumLit,
  StrLit,
  BoolLit,
  OptionArg,
  Seq,
  Arg,
  QualifiedIdent,
  SourceRange,
} from "./base.js";

// --- Dialect generation ---

export function genDialect(): Dialect {
  const d = new Dialect("JavaScript");
  d.addImport("Init");

  // Syntactic categories
  const stmt = d.addSynCat("stmt");
  const expr = d.addSynCat("expr");
  const param = d.addSynCat("param");
  const typeAnnotation = d.addSynCat("typeAnnotation");

  const stmtRef = stmt.ref();
  const exprRef = expr.ref();
  const paramRef = param.ref();
  const typeRef = typeAnnotation.ref();

  // Type annotations
  d.addOp("NumberType", [], typeRef);
  d.addOp("BooleanType", [], typeRef);
  d.addOp("StringType", [], typeRef);
  d.addOp("VoidType", [], typeRef);
  d.addOp("BigIntType", [], typeRef);
  d.addOp("AnyType", [], typeRef);
  d.addOp("NamedType", [new ArgDecl("name", Init.Ident)], typeRef);

  // Parameters
  d.addOp(
    "Parameter",
    [
      new ArgDecl("name", Init.Ident),
      new ArgDecl("typeAnnotation", typeRef),
    ],
    paramRef,
  );

  // Expressions
  d.addOp("NumericLiteral", [new ArgDecl("value", Init.Num)], exprRef);
  d.addOp("BigIntLiteral", [new ArgDecl("value", Init.Num)], exprRef);
  d.addOp("StringLiteral", [new ArgDecl("value", Init.Str)], exprRef);
  d.addOp("TrueLiteral", [], exprRef);
  d.addOp("FalseLiteral", [], exprRef);
  d.addOp("Identifier", [new ArgDecl("name", Init.Ident)], exprRef);
  d.addOp(
    "BinaryExpr",
    [
      new ArgDecl("operator", Init.Ident),
      new ArgDecl("left", exprRef),
      new ArgDecl("right", exprRef),
    ],
    exprRef,
  );
  d.addOp(
    "PrefixUnaryExpr",
    [new ArgDecl("operator", Init.Ident), new ArgDecl("operand", exprRef)],
    exprRef,
  );
  d.addOp(
    "CallExpr",
    [
      new ArgDecl("callee", exprRef),
      new ArgDecl("args", Init.Seq, exprRef),
    ],
    exprRef,
  );
  d.addOp(
    "PropertyAccessExpr",
    [new ArgDecl("object", exprRef), new ArgDecl("name", Init.Ident)],
    exprRef,
  );
  d.addOp(
    "ConditionalExpr",
    [
      new ArgDecl("condition", exprRef),
      new ArgDecl("whenTrue", exprRef),
      new ArgDecl("whenFalse", exprRef),
    ],
    exprRef,
  );
  d.addOp(
    "ParenExpr",
    [new ArgDecl("expression", exprRef)],
    exprRef,
  );

  // Statements
  d.addOp(
    "VariableDecl",
    [
      new ArgDecl("name", Init.Ident),
      new ArgDecl("typeAnnotation", typeRef),
      new ArgDecl("initializer", exprRef),
    ],
    stmtRef,
  );
  d.addOp(
    "ExpressionStmt",
    [new ArgDecl("expression", exprRef)],
    stmtRef,
  );
  d.addOp(
    "ReturnStmt",
    [new ArgDecl("expression", exprRef)],
    stmtRef,
  );
  d.addOp(
    "IfStmt",
    [
      new ArgDecl("condition", exprRef),
      new ArgDecl("thenBlock", stmtRef),
      new ArgDecl("elseBlock", stmtRef),
    ],
    stmtRef,
  );
  d.addOp(
    "WhileStmt",
    [new ArgDecl("condition", exprRef), new ArgDecl("body", stmtRef)],
    stmtRef,
  );
  d.addOp(
    "Block",
    [new ArgDecl("statements", Init.Seq, stmtRef)],
    stmtRef,
  );
  d.addOp(
    "AssertStmt",
    [new ArgDecl("condition", exprRef)],
    stmtRef,
  );
  d.addOp(
    "AssignStmt",
    [new ArgDecl("target", exprRef), new ArgDecl("value", exprRef)],
    stmtRef,
  );

  // Top-level: function declaration
  d.addOp(
    "FunctionDecl",
    [
      new ArgDecl("name", Init.Ident),
      new ArgDecl("params", Init.Seq, paramRef),
      new ArgDecl("returnType", typeRef),
      new ArgDecl("body", stmtRef),
    ],
    stmtRef,
  );

  // Module (top-level command)
  d.addOp(
    "Module",
    [new ArgDecl("statements", Init.Seq, stmtRef)],
    Init.Command,
  );

  return d;
}

// --- TypeScript AST to Strata Operation translation ---

function getSourceRange(node: ts.Node, sourceFile: ts.SourceFile): SourceRange {
  return { start: node.getStart(sourceFile), end: node.getEnd() };
}

function binaryOpName(kind: ts.SyntaxKind): string {
  const map: Record<number, string> = {
    [ts.SyntaxKind.PlusToken]: "Plus",
    [ts.SyntaxKind.MinusToken]: "Minus",
    [ts.SyntaxKind.AsteriskToken]: "Star",
    [ts.SyntaxKind.SlashToken]: "Slash",
    [ts.SyntaxKind.PercentToken]: "Percent",
    [ts.SyntaxKind.EqualsEqualsEqualsToken]: "StrictEq",
    [ts.SyntaxKind.ExclamationEqualsEqualsToken]: "StrictNeq",
    [ts.SyntaxKind.LessThanToken]: "Lt",
    [ts.SyntaxKind.LessThanEqualsToken]: "Le",
    [ts.SyntaxKind.GreaterThanToken]: "Gt",
    [ts.SyntaxKind.GreaterThanEqualsToken]: "Ge",
    [ts.SyntaxKind.AmpersandAmpersandToken]: "And",
    [ts.SyntaxKind.BarBarToken]: "Or",
  };
  return map[kind] ?? `UnknownBinOp_${kind}`;
}

function prefixOpName(kind: ts.SyntaxKind): string {
  const map: Record<number, string> = {
    [ts.SyntaxKind.ExclamationToken]: "Not",
    [ts.SyntaxKind.MinusToken]: "Neg",
  };
  return map[kind] ?? `UnknownPrefixOp_${kind}`;
}

export class Parser {
  private dialect: Dialect;
  private ops: Map<string, OpDecl> = new Map();

  constructor(dialect: Dialect) {
    this.dialect = dialect;
    // Cache all ops
    for (const decl of dialect.decls) {
      if (decl instanceof OpDecl) {
        // skip
      }
    }
  }

  private op(name: string): OpDecl {
    return this.dialect.getOp(name);
  }

  private mk(name: string, args: Arg[], ann: SourceRange | null = null): Operation {
    return this.op(name).create(args, ann);
  }

  translateType(
    checker: ts.TypeChecker,
    node: ts.TypeNode | undefined,
    contextNode: ts.Node,
  ): Operation {
    if (!node) {
      // Infer type from checker
      const type = checker.getTypeAtLocation(contextNode);
      return this.typeFromTsType(checker, type);
    }
    if (ts.isTypeReferenceNode(node)) {
      return this.mk("NamedType", [new Ident(node.typeName.getText())]);
    }
    switch (node.kind) {
      case ts.SyntaxKind.NumberKeyword:
        return this.mk("NumberType", []);
      case ts.SyntaxKind.BooleanKeyword:
        return this.mk("BooleanType", []);
      case ts.SyntaxKind.StringKeyword:
        return this.mk("StringType", []);
      case ts.SyntaxKind.VoidKeyword:
        return this.mk("VoidType", []);
      case ts.SyntaxKind.BigIntKeyword:
        return this.mk("BigIntType", []);
      default:
        return this.mk("AnyType", []);
    }
  }

  private typeFromTsType(checker: ts.TypeChecker, type: ts.Type): Operation {
    const typeStr = checker.typeToString(type);
    switch (typeStr) {
      case "number":
        return this.mk("NumberType", []);
      case "boolean":
        return this.mk("BooleanType", []);
      case "string":
        return this.mk("StringType", []);
      case "void":
        return this.mk("VoidType", []);
      case "bigint":
        return this.mk("BigIntType", []);
      default:
        return this.mk("NamedType", [new Ident(typeStr)]);
    }
  }

  translateExpr(
    checker: ts.TypeChecker,
    node: ts.Expression,
    sf: ts.SourceFile,
  ): Operation {
    const ann = getSourceRange(node, sf);

    if (ts.isNumericLiteral(node)) {
      const val = Number(node.text);
      if (Number.isInteger(val)) {
        return this.mk("NumericLiteral", [new NumLit(val)], ann);
      }
      // For non-integer, still use NumLit but the Lean side will handle float
      return this.mk("NumericLiteral", [new NumLit(val)], ann);
    }

    if (ts.isBigIntLiteral(node)) {
      const val = BigInt(node.text.replace("n", ""));
      return this.mk("BigIntLiteral", [new NumLit(val)], ann);
    }

    if (ts.isStringLiteral(node)) {
      return this.mk("StringLiteral", [new StrLit(node.text)], ann);
    }

    if (node.kind === ts.SyntaxKind.TrueKeyword) {
      return this.mk("TrueLiteral", [], ann);
    }
    if (node.kind === ts.SyntaxKind.FalseKeyword) {
      return this.mk("FalseLiteral", [], ann);
    }

    if (ts.isIdentifier(node)) {
      return this.mk("Identifier", [new Ident(node.text)], ann);
    }

    if (ts.isBinaryExpression(node)) {
      // Handle assignment
      if (node.operatorToken.kind === ts.SyntaxKind.EqualsToken) {
        return this.mk(
          "AssignStmt",
          [
            this.translateExpr(checker, node.left, sf),
            this.translateExpr(checker, node.right, sf),
          ],
          ann,
        );
      }
      return this.mk(
        "BinaryExpr",
        [
          new Ident(binaryOpName(node.operatorToken.kind)),
          this.translateExpr(checker, node.left, sf),
          this.translateExpr(checker, node.right, sf),
        ],
        ann,
      );
    }

    if (ts.isPrefixUnaryExpression(node)) {
      return this.mk(
        "PrefixUnaryExpr",
        [
          new Ident(prefixOpName(node.operator)),
          this.translateExpr(checker, node.operand, sf),
        ],
        ann,
      );
    }

    if (ts.isCallExpression(node)) {
      const args = node.arguments.map((a) => this.translateExpr(checker, a, sf));
      return this.mk(
        "CallExpr",
        [this.translateExpr(checker, node.expression, sf), new Seq(args)],
        ann,
      );
    }

    if (ts.isPropertyAccessExpression(node)) {
      return this.mk(
        "PropertyAccessExpr",
        [
          this.translateExpr(checker, node.expression, sf),
          new Ident(node.name.text),
        ],
        ann,
      );
    }

    if (ts.isConditionalExpression(node)) {
      return this.mk(
        "ConditionalExpr",
        [
          this.translateExpr(checker, node.condition, sf),
          this.translateExpr(checker, node.whenTrue, sf),
          this.translateExpr(checker, node.whenFalse, sf),
        ],
        ann,
      );
    }

    if (ts.isParenthesizedExpression(node)) {
      return this.mk(
        "ParenExpr",
        [this.translateExpr(checker, node.expression, sf)],
        ann,
      );
    }

    // Fallback: unsupported expression
    throw new Error(`Unsupported expression: ${ts.SyntaxKind[node.kind]} at ${ann.start}-${ann.end}`);
  }

  translateStmt(
    checker: ts.TypeChecker,
    node: ts.Statement,
    sf: ts.SourceFile,
  ): Operation {
    const ann = getSourceRange(node, sf);

    if (ts.isVariableStatement(node)) {
      // Handle first declaration only for now
      const decl = node.declarationList.declarations[0];
      if (!decl) throw new Error("Empty variable declaration");
      const name = decl.name.getText(sf);
      const typeNode = decl.type;
      const type = this.translateType(checker, typeNode, decl);
      const init = decl.initializer
        ? this.translateExpr(checker, decl.initializer, sf)
        : this.mk("Identifier", [new Ident("undefined")]);
      return this.mk("VariableDecl", [new Ident(name), type, init], ann);
    }

    if (ts.isExpressionStatement(node)) {
      // Check for console.assert
      if (
        ts.isCallExpression(node.expression) &&
        ts.isPropertyAccessExpression(node.expression.expression)
      ) {
        const prop = node.expression.expression;
        if (
          ts.isIdentifier(prop.expression) &&
          prop.expression.text === "console" &&
          prop.name.text === "assert"
        ) {
          const arg = node.expression.arguments[0];
          if (arg) {
            return this.mk(
              "AssertStmt",
              [this.translateExpr(checker, arg, sf)],
              ann,
            );
          }
        }
      }

      // Check for assignment expression statement
      const expr = node.expression;
      if (ts.isBinaryExpression(expr) && expr.operatorToken.kind === ts.SyntaxKind.EqualsToken) {
        return this.mk(
          "AssignStmt",
          [
            this.translateExpr(checker, expr.left, sf),
            this.translateExpr(checker, expr.right, sf),
          ],
          ann,
        );
      }

      return this.mk(
        "ExpressionStmt",
        [this.translateExpr(checker, node.expression, sf)],
        ann,
      );
    }

    if (ts.isReturnStatement(node)) {
      const expr = node.expression
        ? this.translateExpr(checker, node.expression, sf)
        : this.mk("Identifier", [new Ident("undefined")]);
      return this.mk("ReturnStmt", [expr], ann);
    }

    if (ts.isIfStatement(node)) {
      const cond = this.translateExpr(checker, node.expression, sf);
      const thenBlock = this.translateStmt(checker, node.thenStatement, sf);
      const elseBlock = node.elseStatement
        ? this.translateStmt(checker, node.elseStatement, sf)
        : this.mk("Block", [new Seq([])]);
      return this.mk("IfStmt", [cond, thenBlock, elseBlock], ann);
    }

    if (ts.isWhileStatement(node)) {
      return this.mk(
        "WhileStmt",
        [
          this.translateExpr(checker, node.expression, sf),
          this.translateStmt(checker, node.statement, sf),
        ],
        ann,
      );
    }

    if (ts.isBlock(node)) {
      const stmts = node.statements.map((s) => this.translateStmt(checker, s, sf));
      return this.mk("Block", [new Seq(stmts)], ann);
    }

    throw new Error(`Unsupported statement: ${ts.SyntaxKind[node.kind]} at ${ann.start}-${ann.end}`);
  }

  translateFunctionDecl(
    checker: ts.TypeChecker,
    node: ts.FunctionDeclaration,
    sf: ts.SourceFile,
  ): Operation {
    const ann = getSourceRange(node, sf);
    const name = node.name?.text ?? "<anonymous>";

    const params = node.parameters.map((p) => {
      const pName = p.name.getText(sf);
      const pType = this.translateType(checker, p.type, p);
      return this.op("Parameter").create([new Ident(pName), pType]);
    });

    const returnType = this.translateType(checker, node.type, node);
    const body = node.body
      ? this.translateStmt(checker, node.body, sf)
      : this.mk("Block", [new Seq([])]);

    return this.mk(
      "FunctionDecl",
      [new Ident(name), new Seq(params), returnType, body],
      ann,
    );
  }

  parseModule(
    sourceFile: ts.SourceFile,
    checker: ts.TypeChecker,
  ): Program {
    const program = new Program(this.dialect);
    const stmts: Operation[] = [];

    for (const stmt of sourceFile.statements) {
      if (ts.isFunctionDeclaration(stmt)) {
        stmts.push(this.translateFunctionDecl(checker, stmt, sourceFile));
      } else {
        try {
          stmts.push(this.translateStmt(checker, stmt, sourceFile));
        } catch (e) {
          // Skip unsupported top-level statements for now
          console.error(`Warning: ${(e as Error).message}`);
        }
      }
    }

    const module = this.op("Module").create([new Seq(stmts)]);
    program.add(module);
    return program;
  }
}
