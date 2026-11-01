// Card displaying a single task with its actions.
import { formatDate, isPast, isToday } from '../utils/dates.js'

const PRIORITY = {
  low:    { label: 'Basse',   pill: 'bg-slate-100 text-slate-500',  border: 'border-l-slate-300' },
  medium: { label: 'Moyenne', pill: 'bg-amber-50 text-amber-600',   border: 'border-l-amber-400' },
  high:   { label: 'Haute',   pill: 'bg-red-50 text-red-500',       border: 'border-l-red-400'   },
}

function DateBadge({ iso, isEnd }) {
  if (!iso) return null
  const past = isEnd && isPast(iso)
  const tod  = isEnd && isToday(iso)
  return (
    <span className={`inline-flex items-center gap-0.5 text-xs px-2 py-0.5 rounded-full font-medium ${
      past ? 'bg-red-50 text-red-500' :
      tod  ? 'bg-amber-50 text-amber-600' :
             'bg-blue-50 text-blue-500'
    }`}>
      {isEnd ? (past ? '⚠ ' : '⏰ ') : '▶ '}{formatDate(iso)}
    </span>
  )
}

function CheckIcon() {
  return (
    <svg width="10" height="8" viewBox="0 0 10 8" fill="none">
      <path d="M1 4L3.5 6.5L9 1" stroke="white" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  )
}

function EditIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
    </svg>
  )
}

function TrashIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="3 6 5 6 21 6"/>
      <path d="M19 6l-1 14H6L5 6"/>
      <path d="M10 11v6M14 11v6"/>
      <path d="M9 6V4h6v2"/>
    </svg>
  )
}

export default function TaskCard({ task: t, onToggle, onEdit, onDelete }) {
  const p = PRIORITY[t.priority] || PRIORITY.medium

  function handleDelete() {
    if (window.confirm('Supprimer cette tâche ?')) onDelete(t.id)
  }

  return (
    <div className={`bg-white border border-slate-200 border-l-4 ${p.border} rounded-xl p-4 flex gap-3 items-start shadow-sm hover:shadow-md transition-shadow ${t.done ? 'opacity-60' : ''}`}>

      {/* Checkbox */}
      <button
        onClick={() => onToggle(t.id)}
        aria-label={t.done ? 'Marquer non terminé' : 'Marquer terminé'}
        className={`mt-0.5 w-5 h-5 rounded-full border-2 flex-shrink-0 flex items-center justify-center transition-colors ${
          t.done
            ? 'bg-emerald-500 border-emerald-500'
            : 'border-slate-300 hover:border-indigo-400'
        }`}
      >
        {t.done && <CheckIcon />}
      </button>

      {/* Content */}
      <div className="flex-1 min-w-0">
        <p className={`font-semibold text-sm text-slate-800 ${t.done ? 'line-through text-slate-400' : ''}`}>
          {t.title}
        </p>
        {t.desc && (
          <p className="text-xs text-slate-500 mt-0.5 whitespace-pre-wrap">{t.desc}</p>
        )}
        <div className="flex flex-wrap gap-1.5 mt-2">
          <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${p.pill}`}>
            {p.label}
          </span>
          <DateBadge iso={t.start} isEnd={false} />
          <DateBadge iso={t.end}   isEnd={true} />
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-0.5 flex-shrink-0">
        <button
          onClick={() => onEdit(t.id)}
          aria-label="Modifier"
          className="p-1.5 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
        >
          <EditIcon />
        </button>
        <button
          onClick={handleDelete}
          aria-label="Supprimer"
          className="p-1.5 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
        >
          <TrashIcon />
        </button>
      </div>
    </div>
  )
}
