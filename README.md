# graph-traversal

Algorithms for graph traversal (don't use this, use [Loom](https://github.com/aysylu/loom) instead)

## Usage

[![Build Status](https://travis-ci.com/HughPowell/graph-traversal.svg?branch=main)](https://travis-ci.com/HughPowell/graph-traversal)

Designed to be run from the REPL,

```shell
lein repl
```

There are 2 namespaces, `graph-traversal.graph` and `graph-traversal.algorithm`.

To generate a random graph with 5 vertices and 10 edges

```clojure
(require '[graph-traversal.graph :as graph])

(graph/random-graph 5 10)
```

## License

Copyright © 2020 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
