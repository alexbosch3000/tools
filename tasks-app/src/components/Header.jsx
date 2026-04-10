// En-tête avec titre, bascule liste/calendrier et bouton de création
function ListIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
      <line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/>
      <line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>
    </svg>
  )
}
function CalIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
      <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/>
      <line x1="3" y1="10" x2="21" y2="10"/>
    </svg>
  )
}

export default function Header({ onNew, view, onView }) {
  return (
    <div className="flex items-center justify-between mb-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Mes tâches</h1>
        <p className="text-sm text-slate-400 mt-0.5">Gérez vos tâches et projets</p>
      </div>

      <div className="flex items-center gap-2">
        {/* Bascule vue */}
        <div className="flex bg-slate-100 rounded-lg p-0.5 gap-0.5">
          <button
            onClick={() => onView('list')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-semibold transition-all ${
              view === 'list'
                ? 'bg-white shadow-sm text-slate-800'
                : 'text-slate-400 hover:text-slate-600'
            }`}
          >
            <ListIcon /> Liste
          </button>
          <button
            onClick={() => onView('calendar')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-semibold transition-all ${
              view === 'calendar'
                ? 'bg-white shadow-sm text-slate-800'
                : 'text-slate-400 hover:text-slate-600'
            }`}
          >
            <CalIcon /> Calendrier
          </button>
        </div>

        <button
          onClick={onNew}
          className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 active:scale-95 text-white px-4 py-2 rounded-lg font-medium text-sm transition-all"
        >
          <span className="text-base leading-none">+</span>
          Nouvelle tâche
        </button>
      </div>
    </div>
  )
}
