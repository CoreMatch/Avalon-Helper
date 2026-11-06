# Contributing

**First of all, welcome to contribute to the Avalon-Helper project.**

## Basic Guidelines

### Use an IDE.

Specifically, IntelliJ IDEA. Download the (free) Community Edition of it [here](https://www.jetbrains.com/idea/download/). Some people use
other tools, like VS Code, but I would personally not recommend them for Java development.

### Always test your changes.

Do not submit something without at least running the game to see if it compiles.  
If you are submitting a new implementation, make sure it has a name and description and that it works properly.

## Style Guidelines

## Style guide

### Kotlin :
[Import this files to IDEA](files/Style.xml)

### Follow the formatting guidelines.

This means.

- There needs to be spaces around the parentheses. `if (condition) {`, `SomeType s = (SomeType) object`.
- Identical curly brackets.
- Indentation with 4 spaces
- `camelCase`, **even for constants or enumerations. Why? Because `SCREAMING_CASE` is hard to read, annoying to type, and doesn't do
  anything useful. Constants are more dangerous than variables**, not less. Any reasonable IDE should highlight them for you.
- Don't use underscores for anything.
- Don't use parenthesis-free `if/else` statements. `if(x) statement else statement2` should **never** be done. In very specific cases,
  parenthesis-free if statements are not allowed in a single line. `if(cond) return;` will not be allowed.
- Choose multi-line javadoc whenever possible
  ```
  /**
    * @return for example
    */
  instead of single-line javadoc.
  ``` 

- Short method/variable names (multiple long words should be avoided if possible, especially for variables).
- Use wildcard imports - `import some.package.*` - for everything. This makes the use of incorrect classes more obvious (*e.g.
  com.github.dr.rwserver.util.Timer vs java.util.Timer*) and makes the code look cleaner. Of course, you can use automatic import

### Use `struct` collections and classes whenever possible.

Instead of using `java.util.List`, `java.util.HashMap` and other standard Java collections, use `Seq`, `ObjectMap` and other equivalents
from `arc.struct`.
Why? Because that's what the rest of the code base uses, and standard collections have a lot of flaws and usability issues associated with
them.
In the rare cases where concurrency is needed, you can use standard Java classes for this purpose (e.g. `CopyOnWriteArrayList`).

What you'll usually need to change:

- `HashSet` -> `ObjectSet`
- `HashMap` -> `ObjectMap`
- `List` / `ArrayList` / `Stack` -> `Seq`
- *Many others*

### Avoid boxed types (Integer, Boolean)

Never create variables or collections with boxed types `Seq<Integer>` or `ObjectMap<Integer, ...>`. Use the collections specialized for this
task, e.g. `IntSeq` and `IntMap`.

### Do not allocate anything if possible.

Never allocate `new` objects in the main loop. If you absolutely require new objects, use `Pools` to obtain and free object instances.
Otherwise, use the `Tmp` variables for things like vector/shape operations, or create `static` variables for re-use.
If using a list, make it a static variable and clear it every time it is used. Re-use as much as possible.

### Avoid bloated code and unnecessary getters/setters.

This is situational, but in essence what it means is to avoid using any sort of getters and setters unless absolutely necessary. Public or
protected fields should suffice for most things.
If something needs to be encapsulated in the future, IntelliJ can handle it with a few clicks.

### Do not create methods unless necessary.

Unless a block of code is very large or used in more than 1-2 places, don't split it up into a separate method. Making unnecessary methods
only creates confusion, and may slightly decrease performance.

### Java can refer to alibaba's JavaDoc specification