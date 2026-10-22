import { useEffect, useRef } from 'react'
import { ErrorNotice } from '../../components/Feedback'

export function ForfeitDialog({
  onClose,
  onConfirm,
  pending,
  error,
}: {
  onClose: () => void
  onConfirm: () => void
  pending: boolean
  error: unknown
}) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    dialog.current?.showModal()
  }, [])
  return (
    <dialog
      ref={dialog}
      className="panel confirm-dialog"
      aria-labelledby="forfeit-title"
      onCancel={(event) => {
        event.preventDefault()
        if (!pending) onClose()
      }}
    >
      <h2 id="forfeit-title">Leave this battle?</h2>
      <p>Forfeiting awards the game to your opponent. Ranked battles will update your rating.</p>
      <ErrorNotice error={error} />
      <div>
        <button className="button secondary" onClick={onClose} disabled={pending} autoFocus>
          Keep playing
        </button>
        <button className="button danger-button" onClick={onConfirm} disabled={pending}>
          Forfeit battle
        </button>
      </div>
    </dialog>
  )
}
