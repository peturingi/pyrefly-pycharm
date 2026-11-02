package dk.ingi.pyreflypycharm.inspection

import com.redhat.devtools.lsp4ij.inspections.LSPLocalInspectionToolBase

/**
 * Inspection that carries Pyrefly's diagnostics. Shows up as "Pyrefly" under the "Pyrefly" group in
 * Settings | Editor | Inspections and in Code | Inspect Code.
 */
class PyreflyInspection : LSPLocalInspectionToolBase() {
    companion object {
        const val ID = "PyreflyPyCharm"
    }
}
