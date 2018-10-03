# Ludwig

Planck, Max Karl Ernst **Ludwig** Planck!

## What is this?

`Ludwig` is an experiment. It takes the JavaScriptCore engine, covers it in
Clojure bindings, melts it via a radioactive REPL and shoots it
through the steaming hot pipes of GraalVM's native-image tool.

The result is a red-hot liquid, it flows whereever it wants and if you
try to touch it, you might get burned.

## Again, what is this thing?

This project primarily tries to show how to build non trivial applications
in Clojure and compile them down to native code through GraalVM. This is
not and likely will never be a replacement for
[planck](https://github.com/planck-repl/planck) or 
[lumo](https://github.com/anmonteiro/lumo).

The path to JavaScriptCore and its headers is set in `deps.edn` of the
[com.github.lxbr/jsc](https://github.com/lxbr/jsc)
project. `Ludwig` works on macos out of the box
but may work on other systems when the aforementioned paths are set appropriately.
The name of the JavaScriptCore native library is hard coded as `JavaScriptCore`.

## Now, how do I run it?

### On the JVM

1. At the project root run `scripts/build_js`.
2. Then start a REPL and chant the magic spell...

```clojure
(require 'com.github.lxbr.ludwig.main)

(in-ns 'com.github.lxbr.ludwig.main)

(-main)
```

...and a self-hosted ClojureScript REPL will appear.

### As native binary

This project has been developed with *GraalVM Community Edition 1.0 RC6*.

1. [Download](https://www.graalvm.org/downloads/) GraalVM.
2. Set the environment variable `GRAALVM_HOME` (see *Preparation* in [example](https://www.graalvm.org/docs/examples/java-simple-stream-benchmark/)).
3. At the project root run `scripts/build_js`.
4. Then run `scripts/build_native`.
5. After the scripts finished, call `./ludwig`.

* Does compilation fail?
  * Try uncommenting the `--report-unsupported-elements-at-runtime` flag in the `com.github.lxbr.ludwig.native-image` namespace.
  * Compilation will take longer and the binary will be bigger.

* Is macos asking you to install Java 6?
  * Have a look at this: https://support.apple.com/kb/dl1572

## Clojure and GraalVM

Compiling Clojure sources to native code works surprisingly well,
except when it doesn't. Here are a few things to keep in mind.

* make sure you compile all necessary Clojure files to class files
  * remember to create the directory pointed to by `clojure.core/*compile-path*`
  * then use `clojure.core/compile` on namespace symbols
  * using the direct-linking option is a good idea
  * don't forget to put the compile path into the classpath argument to 
	the native-image tool

* code that calls `ClassLoader.defineClass` is problematic
  * build aborts with error
  * avoid error by passing `--report-unsupported-elements-at-runtime` flag
  * but build time and artifact size go up by 2x-3x
  * current solution: comment out all code paths that can reach `defineClass`
  * not always easy/possible
	* cannot comment out LispReader's read-eval functionality
	  because then Clojure itself doesn't build anymore
    * needed to fork tools.reader for `Ludwig` to work

* debugging Java is hard
  * not a GraalVM specific issue
  * debugging private and final fields/methods in 3rd party libs is a pain
  * trial and error is incredibly slow
  * native-image (for `Ludwig`) can take a minute or more to build the artifact
  * the feedback loop is almost non-existent
  * try functionality in Clojure at the REPL before compiling to native
  * this gives you a tighter feedback loop and some confidence that native
	compilation will succeed
  
* reflection is the enemy
  * type hint everything you can
  * what you cannot type hint, you put into [reflect.json](https://github.com/oracle/graal/blob/master/substratevm/REFLECTION.md)
  
* static/at most ones initialization
  * initialization code is run at compile time
	* static initializers
	* static variable definitions
  * this is a feature(!) to move computation from runtime to compile time
  * the problem: code can be initialized at compile time and never at runtime
  * this can cause trouble, be careful with static initialization (singleton pattern, etc.)
  * native libraries must be loaded in the entry point method not in static initializers
  * unless it produces constants that don't depend on time, avoid creating vars via `def`
	* `defn` is fine, unless it closes over values that depend on time
	* instead initialize state in the entry point method
	* this guarantees your code will run when invoking the binary

* documentation for GraalVM and native-image is in flux
  * some features are mainly documented in github issues,
    no proper documentation yet, the project is fast moving

* integration of classpath resources into binary artifact
  * `-H:IncludeResources=<Java regexp that matches resources to be included in the image>`
  * when you deal with resources from jars, works as expected given relative paths
  * separate regex patterns by pipes `(jni/Darwin/libjffi-1.2.jnilib)|...`
  * when you want to include a file from the filesystem
	* the regex is applied to the absolute path
	* a regex of `file.bin` does not work for the file `/Users/foobar/project/file.bin`
	* use the regex `.*/file.bin` instead
	* resources can then be loaded as usual, e.g. via `clojure.java.io/resource`

* proxy classes (code gen in general)
  * JNR (and JNA) did not work because of JVM runtime code gen
  * for that reason `Ludwig` uses `jffi` directly

* callbacks from native code to JVM still need JNI
  * callbacks (called *Closure*s) in `jffi` work in the REPL
  * but give you segfaults when compiled to a native binary
  * the reason is unclear, maybe some static initialization issue

* tools.deps helps with building classpath argument to native-image tool

## Future Experiments

* compile ClojureScript Compiler and Google Closure Compiler to native code
  * no need for self hosting => better perf?
  * see https://github.com/oracle/graal/issues/428
  * and https://gist.github.com/ChadKillingsworth/137e8064a9933379726b4d23254a3214

* use Rhino/Nashorn/GraalJS as evaluation environment
  * solves the "callback via JNI" problem?!
  
* mimic NodeJS
  * binary that runs JS with support for IO
  * access to JVM library eco-system
  * how does threading work?
  * how useful are concurrently running JS engine instances?
  
* GraalVM uses libffi itself, should we use that instead of jffi?
  * would make GraalVM a dependency
  * better integration for callbacks?
  
* when the program fails we return a non-zero exit code
  * **and** print the app-state as edn
  * we can then analyze that data in a Clojure REPL
  * starting the program with a debug flag, we can pass that data back into the program
    and reproduce the app-state and the bug

## Should I use native-image in all my Clojure projects?
  
**No**, GraalVM is still in the release candidate stage, failing code is costly
because compilation and debugging are slow, many issues still have to be found.
**But** the results are immensely promising. Being able to write high level code
and then compile it down to a relatively small binary that needs little runtime memory
is a dream come true. With Clojure we can now reach JVM, JS and Native!

GraalVM promises a lot of value, we should keep an eye on it.

## License

This project is licensed under the terms of the Eclipse Public License 1.0 (EPL).
