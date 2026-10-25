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
      theme="codeduel"
      value={code}
      onChange={(value) => onChange(value ?? '')}
      loading={<div className="editor-loading">Opening Java editor…</div>}
      beforeMount={(editor) =>
        editor.editor.defineTheme('codeduel', {
          base: 'vs-dark',
          inherit: false,
          rules: [
            { token: '', foreground: 'E6E6E6' },
            { token: 'keyword', foreground: 'FFFFFF', fontStyle: 'bold' },
            { token: 'comment', foreground: 'AAAAAA' },
            { token: 'string', foreground: 'CCCCCC' },
            { token: 'number', foreground: 'CCCCCC' },
          ],
          colors: {
            'editor.background': '#111111',
            'editor.foreground': '#E6E6E6',
            'editorLineNumber.foreground': '#999999',
            'editorLineNumber.activeForeground': '#FFFFFF',
            'editor.lineHighlightBackground': '#222222',
            'editor.selectionBackground': '#444444',
            'editor.inactiveSelectionBackground': '#333333',
            'editorCursor.foreground': '#FFFFFF',
            'editorWidget.background': '#222222',
            'editorWidget.border': '#555555',
            'editorHoverWidget.background': '#222222',
            'editorHoverWidget.border': '#555555',
            'editorSuggestWidget.selectedBackground': '#444444',
            'editorSuggestWidget.highlightForeground': '#FFFFFF',
            'list.activeSelectionBackground': '#444444',
            'list.activeSelectionForeground': '#FFFFFF',
            'list.hoverBackground': '#333333',
            'list.highlightForeground': '#FFFFFF',
            'editorError.foreground': '#FFFFFF',
            'editorWarning.foreground': '#BBBBBB',
            'editorInfo.foreground': '#BBBBBB',
            focusBorder: '#BBBBBB',
          },
        })
      }
      onMount={(editor, api) => {
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
        bracketPairColorization: { enabled: false },
        ariaLabel: 'Java solution editor',
        accessibilitySupport: 'auto',
        smoothScrolling: true,
      }}
    />
  )
}
