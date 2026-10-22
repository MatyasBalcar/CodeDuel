import Editor, { loader } from '@monaco-editor/react'
import { useEffect, useRef } from 'react'
import * as monaco from 'monaco-editor/editor/editor.api.js'
import 'monaco-editor/languages/definitions/java/register.js'
import EditorWorker from 'monaco-editor/editor/editor.worker.js?worker'

;(self as typeof self & { MonacoEnvironment: { getWorker: () => Worker } }).MonacoEnvironment = {
  getWorker: () => new EditorWorker(),
}
loader.config({ monaco })

export default function CodeEditor({
  code,
  onChange,
  readOnly,
  onSubmit,
  onRun,
}: {
  code: string
  onChange: (code: string) => void
  readOnly: boolean
  onSubmit: () => void
  onRun: () => void
}) {
  const actions = useRef({ onSubmit, onRun })
  useEffect(() => {
    actions.current = { onSubmit, onRun }
  }, [onSubmit, onRun])
  return (
    <Editor
      height="100%"
      language="java"
      theme="vs-dark"
      value={code}
      onChange={(value) => onChange(value ?? '')}
      loading={<div className="editor-loading">Opening Java editor…</div>}
      beforeMount={(editor) =>
        editor.editor.defineTheme('codeduel', {
          base: 'vs-dark',
          inherit: true,
          rules: [],
          colors: {
            'editor.background': '#121b20',
            'editorLineNumber.foreground': '#667983',
            'editor.lineHighlightBackground': '#19252c',
            'editor.selectionBackground': '#314943',
          },
        })
      }
      onMount={(editor, api) => {
        api.editor.setTheme('codeduel')
        editor.addAction({
          id: 'codeduel-submit',
          label: 'Submit solution',
          keybindings: [api.KeyMod.CtrlCmd | api.KeyCode.Enter],
          run: () => actions.current.onSubmit(),
        })
        editor.addAction({
          id: 'codeduel-run',
          label: 'Run public examples',
          keybindings: [api.KeyMod.CtrlCmd | api.KeyMod.Shift | api.KeyCode.Enter],
          run: () => actions.current.onRun(),
        })
      }}
      options={{
        readOnly,
        minimap: { enabled: false },
        fontSize: 13,
        fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", monospace',
        lineHeight: 23,
        padding: { top: 18, bottom: 18 },
        scrollBeyondLastLine: false,
        automaticLayout: true,
        wordWrap: 'on',
        tabSize: 4,
        renderLineHighlight: 'line',
        ariaLabel: 'Java solution editor',
        accessibilitySupport: 'auto',
        smoothScrolling: true,
      }}
    />
  )
}
