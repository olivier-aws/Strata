# Laurel Java ION Library

Java library for constructing Laurel ASTs and serializing them to Amazon ION format for consumption by the Lean-based Strata verification framework.

## Overview

The Laurel dialect is an intermediate verification language in the Strata framework, designed to support deductive verification, property-based testing, and data-flow analysis for languages like Java, Python, and JavaScript.

This library enables Java developers to:
- Programmatically construct Laurel ASTs
- Serialize them to ION binary format
- Interoperate with the Lean Strata verification framework

## Requirements

- Java 17 or later
- Maven 3.6 or later

## Building

```bash
cd Tools/LaurelJava
mvn clean install
```

## Running Tests

```bash
mvn test
```

## Usage

Add the dependency to your Maven project:

```xml
<dependency>
    <groupId>com.strata</groupId>
    <artifactId>laurel-java</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Example

```java
import com.strata.laurel.ast.*;
import com.strata.laurel.builder.*;
import com.strata.laurel.ion.*;

// Build a simple procedure
Procedure proc = new ProcedureBuilder()
    .name("add")
    .input("x", new HighType.TInt())
    .input("y", new HighType.TInt())
    .output(new HighType.TInt())
    .transparentBody(
        new StmtExpr.PrimitiveOp(
            Operation.ADD,
            List.of(
                new StmtExpr.Identifier("x"),
                new StmtExpr.Identifier("y")
            )
        )
    )
    .build();

// Build a program
Program program = new ProgramBuilder()
    .staticProcedure(proc)
    .build();

// Serialize to ION
byte[] ionBytes = IonSerializer.serialize(program);
```

## Package Structure

- `com.strata.laurel.ast` - AST node definitions (immutable records)
- `com.strata.laurel.builder` - Fluent builders for complex AST nodes
- `com.strata.laurel.ion` - ION serialization
- `com.strata.laurel.pretty` - Pretty printing for debugging

## License

See the LICENSE files in the repository root.
