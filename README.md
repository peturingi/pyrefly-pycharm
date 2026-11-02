# pyrefly-pycharm

pyrefly-pycharm runs [Pyrefly](https://pyrefly.org), Meta's fast Python type checker, inside PyCharm as a
language server, using Red Hat's open-source [LSP4IJ](https://github.com/redhat-developer/lsp4ij).

Pyrefly becomes the source of truth for type errors, hover types and inlay hints. PyCharm keeps doing
what it does well natively: completion, navigation, refactoring and formatting.

Not affiliated with Meta or JetBrains. Source and releases: <https://github.com/peturingi/pyrefly-pycharm>

Written by Claude (Anthropic) in Claude Code. No human wrote or edited the code or this README.

## Why not PyCharm's built-in Pyrefly type engine?

PyCharm 2026.2 can use Pyrefly as its "type engine". That integration feeds Pyrefly types into
PyCharm's own type evaluation, which runs synchronously, from many threads, under the IDE's global
read lock. In build 262.x its per-file cache (`LspTypeEvalContext.cacheElementToStringType`) is a
plain `java.util.WeakHashMap` that is read without a lock and written under two different locks.
Concurrent access corrupts the map into a cycle, a background thread spins forever inside
`WeakHashMap.get`/`put` while holding the read lock, and the IDE's write lock can never be taken
again. The IDE freezes until it is killed. In Remote Development the thin client keeps accepting
typing that never reaches the frozen backend, so that work is lost.

pyrefly-pycharm takes the other route: Pyrefly runs as a normal language server and every request is
asynchronous. Nothing calls Pyrefly while PyCharm holds a lock.

## Requirements

- PyCharm 2026.2 or later
- [LSP4IJ](https://plugins.jetbrains.com/plugin/23257-lsp4ij) 0.21.0 or later (from JetBrains Marketplace)
- `pyrefly` installed, ideally in the project's virtualenv: `pip install pyrefly`

**Remote Development:** install LSP4IJ and pyrefly-pycharm on the *host* (backend), where your project and
`pyrefly` live, not only on the client.

## Limitations

- **Local interpreters only.** Docker, WSL and SSH interpreters are not supported. pyrefly-pycharm
  runs `pyrefly` on the machine where the IDE (or the Remote Development backend) runs and only passes
  a local interpreter path to it. With a remote interpreter, Pyrefly still starts but does not get the
  interpreter, so third-party imports will not resolve. Remote Development, where the whole IDE
  backend runs on the remote machine, does work.
- **One interpreter per project.** The project interpreter is used, or the first module's
  interpreter if the project has none. Projects whose modules use different interpreters get that
  one interpreter for all of them. Not something the author needs; agent-authored pull requests
  are welcome (see [Contributing](#contributing)).
- Tested on PyCharm 2026.2.4 with LSP4IJ 0.21.0 and Pyrefly 1.4.0, on macOS and Linux.

## Install

1. Download `pyrefly-pycharm-<version>.zip` from the GitHub releases (or build it, see below).
2. *Settings | Plugins | ⚙ | Install Plugin from Disk…* and pick the zip.
3. Switch PyCharm's own type engine back to the built-in one (Settings | Python) so Pyrefly does not
   run twice and the freeze described above cannot happen.
4. Open a Python file. When Pyrefly starts, pyrefly-pycharm offers to turn off PyCharm's own type
   inspections so the two checkers stop disagreeing. You can also do this at any time from
   *Tools | Pyrefly*, and turn them back on there.

## How Pyrefly is found

1. The path set in *Settings | Tools | Pyrefly*
2. `pyrefly` next to the project interpreter (the virtualenv's `bin/`), so editor and CLI match
3. `pyrefly` on `PATH`
4. `~/.local/bin`, `~/.cargo/bin`, `/opt/homebrew/bin`, `/usr/local/bin`

The project interpreter is also passed to Pyrefly as `pythonPath`, so imports resolve against the
same environment PyCharm uses.

## Settings

*Settings | Tools | Pyrefly*:

| Setting | Default | Notes |
|---|---|---|
| Type errors and warnings | on | Shown through the **Pyrefly** inspection (group *Pyrefly*), so it also works with *Inspect Code* and inspection profiles. |
| Preset without a Pyrefly config | `auto` | Pyrefly's `typeCheckingMode` for projects without `pyrefly.toml` / `[tool.pyrefly]`. `auto` falls back to `basic`, which reports only high-confidence errors. Choose `default` or run `pyrefly init` to see everything. |
| Check all project files | off | Pyrefly `diagnosticMode = workspace`. |
| Hover, inlay hints, quick fixes | on | |
| Completion, parameter info, navigation | off | PyCharm's own versions stay active; turning these on duplicates results. |
| Exclude PyCharm's "Excluded" folders | on | Sent to Pyrefly as `extraProjectExcludes`. |

Rename, formatting, folding, semantic highlighting, document symbols and code lenses are never
routed to Pyrefly.

Which errors Pyrefly reports is configured the usual way, in `pyrefly.toml` or `[tool.pyrefly]` in
`pyproject.toml`. Suppress one error with `# pyrefly: ignore`.

The LSP traffic can be inspected in LSP4IJ's *Language Servers* tool window (server
"Pyrefly (pyrefly-pycharm)"), which also has start/stop/restart controls.

## Build

Needs a JDK 21+ to run Gradle (a JDK 21 toolchain is used for compiling).

```sh
./gradlew buildPlugin          # -> build/distributions/pyrefly-pycharm-<version>.zip
./gradlew runIde               # sandbox PyCharm with pyrefly-pycharm and LSP4IJ installed
./gradlew runIde -PrunIdeArgs=/path/to/project,/path/to/project/file.py
```

By default the build downloads PyCharm `platformVersion` (see `gradle.properties`), which is several
GB. To build against a PyCharm you already have installed, create a `local.properties` (git-ignored)
next to `build.gradle.kts`:

```properties
platformLocalPath=/Applications/PyCharm.app
```

## Contributing

Report bugs and submit changes through GitHub
[issues](https://github.com/peturingi/pyrefly-pycharm/issues) and
[pull requests](https://github.com/peturingi/pyrefly-pycharm/pulls). This is the submission channel
referred to in Section 6 of the license.

Changes must be written by an AI agent, not by hand. Say in the pull request which agent made the
change (the model if you know it, otherwise the product). Hand-written changes cannot be accepted,
and the license does not allow publishing them, so describe the problem or the wanted behaviour in
an issue instead.

## License

Source-available under the [INGI Agent-Authored Derivatives License, version 0.3](LICENSE), an
experimental license that has not been reviewed by a lawyer. It is not an open-source license. In
short:

- Anyone may use, run, build, copy and share pyrefly-pycharm, including commercially.
- Anyone may modify it and share the result **if the changes are written by an AI agent**; copies
  must keep the license and say which agent made the changes.
- Changes written by a person may only be used privately by that person and may not be shared.

The LICENSE file is authoritative. The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`)
is Gradle's and keeps its Apache-2.0 license. Pyrefly and LSP4IJ are separate projects under their
own licenses and are not included.
