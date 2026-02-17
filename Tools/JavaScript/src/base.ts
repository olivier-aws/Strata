/**
 * Core Strata AST datatypes and Ion serialization.
 * Mirrors the Python strata/base.py module.
 */
import { dom, makeTextWriter, makeBinaryWriter, IonTypes } from "ion-js";

type IonValue = dom.Value;

function ionSymbol(s: string): IonValue { return new dom.Symbol(s); }
function ionNull(): IonValue { return dom.Value.from(null); }
function ionInt(n: number | bigint): IonValue { return new dom.Integer(n); }
function ionString(s: string): IonValue { return new dom.String(s); }
function ionSexp(...args: IonValue[]): IonValue { return new dom.SExpression(args); }

// --- Source ranges ---

export interface SourceRange {
  start: number;
  end: number;
}

function sourceRangeToIon(sr: SourceRange | null): IonValue {
  if (sr === null) return ionNull();
  return ionSexp(ionInt(sr.start), ionInt(sr.end));
}

// --- Qualified identifiers ---

export class QualifiedIdent {
  constructor(public dialect: string, public name: string) {}
  toString(): string { return `${this.dialect}.${this.name}`; }
  toIon(): IonValue { return ionSymbol(this.toString()); }
}

// --- Arg types ---

export type Arg = Operation | Ident | NumLit | StrLit | BoolLit | OptionArg | Seq;

export class Ident {
  constructor(public value: string, public ann: SourceRange | null = null) {}
  toIon(): IonValue {
    return ionSexp(ionSymbol("ident"), sourceRangeToIon(this.ann), ionString(this.value));
  }
}

export class NumLit {
  constructor(public value: number | bigint, public ann: SourceRange | null = null) {}
  toIon(): IonValue {
    return ionSexp(ionSymbol("num"), sourceRangeToIon(this.ann), ionInt(this.value));
  }
}

export class StrLit {
  constructor(public value: string, public ann: SourceRange | null = null) {}
  toIon(): IonValue {
    return ionSexp(ionSymbol("strlit"), sourceRangeToIon(this.ann), ionString(this.value));
  }
}

export class BoolLit {
  constructor(public value: boolean, public ann: SourceRange | null = null) {}
  toIon(): IonValue {
    const sym = this.value ? "Init.boolTrue" : "Init.boolFalse";
    return ionSexp(ionSymbol("op"), ionSexp(ionSymbol(sym), sourceRangeToIon(this.ann)));
  }
}

export class OptionArg {
  constructor(public value: Arg | null = null, public ann: SourceRange | null = null) {}
  toIon(): IonValue {
    if (this.value === null) {
      return ionSexp(ionSymbol("option"), sourceRangeToIon(this.ann));
    }
    return ionSexp(ionSymbol("option"), sourceRangeToIon(this.ann), argToIon(this.value));
  }
}

export class Seq {
  constructor(public values: Arg[], public ann: SourceRange | null = null) {}
  toIon(): IonValue {
    return ionSexp(ionSymbol("seq"), sourceRangeToIon(this.ann), ...this.values.map(argToIon));
  }
}

export function argToIon(a: Arg): IonValue {
  if (a instanceof Operation) {
    return ionSexp(ionSymbol("op"), a.toIon());
  }
  return a.toIon();
}

// --- Operations ---

export class OpDecl {
  public ident: QualifiedIdent;
  constructor(public dialect: string, public name: string) {
    this.ident = new QualifiedIdent(dialect, name);
  }
  create(args: Arg[], ann: SourceRange | null = null): Operation {
    return new Operation(this, args, ann);
  }
}

export class Operation {
  constructor(
    public decl: OpDecl,
    public args: Arg[],
    public ann: SourceRange | null = null,
  ) {}
  toIon(): IonValue {
    return ionSexp(
      this.decl.ident.toIon(),
      sourceRangeToIon(this.ann),
      ...this.args.map(argToIon),
    );
  }
}

// --- Dialect ---

export class SynCatDecl {
  public ident: QualifiedIdent;
  constructor(public dialect: string, public name: string) {
    this.ident = new QualifiedIdent(dialect, name);
  }
  ref(): QualifiedIdent { return this.ident; }
  toIon(): IonValue {
    return new dom.Struct([
      ["type", ionSymbol("syncat")],
      ["name", ionString(this.name)],
    ]);
  }
}

export class ArgDecl {
  constructor(public name: string, public kind: QualifiedIdent, public kindArg?: QualifiedIdent) {}
  toIon(): IonValue {
    let innerSexp: IonValue;
    if (this.kindArg) {
      // Parameterized category like Seq(stmt) or Option(expr)
      // Format: (category (null Init.Seq (null JavaScript.stmt)))
      innerSexp = ionSexp(ionNull(), this.kind.toIon(), ionSexp(ionNull(), this.kindArg.toIon()));
    } else {
      innerSexp = ionSexp(ionNull(), this.kind.toIon());
    }
    return new dom.Struct([
      ["name", ionString(this.name)],
      ["type", ionSexp(ionSymbol("category"), innerSexp)],
    ]);
  }
}

export class OpDeclFull {
  public ident: QualifiedIdent;
  constructor(
    public dialect: string,
    public name: string,
    public args: ArgDecl[],
    public result: QualifiedIdent,
  ) {
    this.ident = new QualifiedIdent(dialect, name);
  }
  toIon(): IonValue {
    const fields: [string, IonValue][] = [
      ["type", ionSymbol("op")],
      ["name", ionString(this.name)],
    ];
    if (this.args.length > 0) {
      fields.push(["args", new dom.List(this.args.map((a) => a.toIon()))]);
    }
    fields.push(["result", this.result.toIon()]);
    return new dom.Struct(fields);
  }
}

export class Dialect {
  public decls: (SynCatDecl | OpDeclFull)[] = [];
  public imports: string[] = [];
  private cats = new Map<string, SynCatDecl>();
  private ops = new Map<string, OpDecl>();

  constructor(public name: string) {}

  addImport(name: string) { this.imports.push(name); }

  addSynCat(name: string): SynCatDecl {
    const decl = new SynCatDecl(this.name, name);
    this.cats.set(name, decl);
    this.decls.push(decl);
    return decl;
  }

  addOp(name: string, args: ArgDecl[], result: QualifiedIdent): OpDecl {
    const full = new OpDeclFull(this.name, name, args, result);
    this.decls.push(full);
    const op = new OpDecl(this.name, name);
    this.ops.set(name, op);
    return op;
  }

  getOp(name: string): OpDecl {
    const op = this.ops.get(name);
    if (!op) throw new Error(`Op ${name} not found in dialect ${this.name}`);
    return op;
  }

  toIon(): IonValue[] {
    const header = ionSexp(ionSymbol("dialect"), ionString(this.name));
    const imports = this.imports.map((i) =>
      new dom.Struct([
        ["type", ionSymbol("import")],
        ["name", ionString(i)],
      ]),
    );
    return [header, ...imports, ...this.decls.map((d) => d.toIon())];
  }
}

// --- Program ---

export class Program {
  public commands: Operation[] = [];
  constructor(public dialect: Dialect) {}

  add(command: Operation) { this.commands.push(command); }

  toIon(): IonValue[] {
    return [
      ionSexp(ionSymbol("program"), ionString(this.dialect.name)),
      ...this.commands.map((c) => c.toIon()),
    ];
  }
}

// --- Init dialect (built-in categories) ---

export const Init = {
  Command: new QualifiedIdent("Init", "Command"),
  Ident: new QualifiedIdent("Init", "Ident"),
  Num: new QualifiedIdent("Init", "Num"),
  Str: new QualifiedIdent("Init", "Str"),
  Seq: new QualifiedIdent("Init", "Seq"),
  Option: new QualifiedIdent("Init", "Option"),
};

// --- Serialization ---

export function serializeToIonText(values: IonValue[]): string {
  const list = new dom.List(values);
  const w = makeTextWriter();
  list.writeTo(w);
  w.close();
  return Buffer.from(w.getBytes()).toString();
}

export function serializeToIonBinary(values: IonValue[]): Uint8Array {
  const list = new dom.List(values);
  const w = makeBinaryWriter();
  list.writeTo(w);
  w.close();
  return w.getBytes();
}
