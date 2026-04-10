// Modal de création et d'édition d'une tâche
import { useState, useEffect, useRef } from 'react'

const EMPTY = { title: '', desc: '', start: '', end: '', priority: 'medium' }

function CloseIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
      <path d="M18 6L6 18M6 6l12 12"/>
    </svg>
  )
}

// prefill : { start, end } pré-rempli depuis la vue calendrier (nouvelle tâche uniquement)
export default function TaskModal({ task, prefill, onSave, onClose }) {
  const [form, setForm] = useState(() => {
    if (task) return { title: task.title, desc: task.desc || '', start: task.start || '', end: task.end || '', priority: task.priority || 'medium' }
    return { ...EMPTY, start: prefill?.start || '', end: prefill?.end || '' }
  })
  const [error, setError] = useState('')
  const titleRef = useRef(null)

  // Focus sur le titre à l'ouverture
  useEffect(() => { titleRef.current?.focus() }, [])

  // Fermeture avec Échap
  useEffect(() => {
    const handle = (e) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', handle)
    return () => document.removeEventListener('keydown', handle)
  }, [onClose])

  function set(key, value) {
    setForm(f => ({ ...f, [key]: value }))
    setError('')
  }

  function handleSubmit(e) {
    e.preventDefault()
    if (!form.title.trim()) {
      setError('Le titre est obligatoire.')
      titleRef.current?.focus()
      return
    }
    if (form.start && form.end && form.end < form.start) {
      setError('La date de fin ne peut pas être antérieure à la date de début.')
      return
    }
    onSave({
      title:    form.title.trim(),
      desc:     form.desc.trim() || null,
      start:    form.start || null,
      end:      form.end   || null,
      priority: form.priority,
    })
  }

  return (
    <div
      className="fixed inset-0 bg-slate-900/50 flex items-center justify-center z-50 p-4"
      onClick={e => { if (e.target === e.currentTarget) onClose() }}
    >
      <div className="bg-white rounded-2xl w-full max-w-md shadow-2xl">

        {/* En-tête */}
        <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-slate-100">
          <h2 className="text-base font-bold text-slate-900">
            {task ? 'Modifier la tâche' : 'Nouvelle tâche'}
          </h2>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
          >
            <CloseIcon />
          </button>
        </div>

        {/* Formulaire */}
        <form onSubmit={handleSubmit} className="px-6 py-5 flex flex-col gap-4">

          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
              Titre <span className="text-red-400">*</span>
            </label>
            <input
              ref={titleRef}
              type="text"
              value={form.title}
              onChange={e => set('title', e.target.value)}
              placeholder="Nom de la tâche"
              className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg outline-none focus:border-indigo-400 transition-colors"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
              Description
            </label>
            <textarea
              value={form.desc}
              onChange={e => set('desc', e.target.value)}
              placeholder="Détails optionnels…"
              rows={3}
              className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg outline-none focus:border-indigo-400 transition-colors resize-y"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
                Date de début
              </label>
              <input
                type="date"
                value={form.start}
                onChange={e => set('start', e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg outline-none focus:border-indigo-400 transition-colors"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
                Date de fin
              </label>
              <input
                type="date"
                value={form.end}
                onChange={e => set('end', e.target.value)}
                className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg outline-none focus:border-indigo-400 transition-colors"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
              Priorité
            </label>
            <select
              value={form.priority}
              onChange={e => set('priority', e.target.value)}
              className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg outline-none focus:border-indigo-400 transition-colors bg-white"
            >
              <option value="low">Basse</option>
              <option value="medium">Moyenne</option>
              <option value="high">Haute</option>
            </select>
          </div>

          {error && (
            <p className="text-xs text-red-500 bg-red-50 border border-red-100 px-3 py-2 rounded-lg">
              {error}
            </p>
          )}

          <div className="flex justify-end gap-2 pt-1">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
            >
              Annuler
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 active:scale-95 rounded-lg transition-all"
            >
              Enregistrer
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
