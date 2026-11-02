// Filter chips and search field for the task list.
const FILTERS = [
  { key: 'all',      label: 'Toutes' },
  { key: 'active',   label: 'En cours' },
  { key: 'upcoming', label: 'À venir' },
  { key: 'overdue',  label: 'En retard' },
  { key: 'done',     label: 'Terminées' },
]

export default function Toolbar({ filter, onFilter, search, onSearch }) {
  return (
    <div className="flex flex-wrap gap-3 items-center mb-6">
      <div className="flex flex-wrap gap-1.5">
        {FILTERS.map(({ key, label }) => (
          <button
            key={key}
            onClick={() => onFilter(key)}
            className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition-colors ${
              filter === key
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'bg-white text-slate-500 border border-slate-200 hover:border-indigo-300 hover:text-indigo-600'
            }`}
          >
            {label}
          </button>
        ))}
      </div>
      <input
        type="text"
        value={search}
        onChange={e => onSearch(e.target.value)}
        placeholder="Rechercher…"
        className="flex-1 min-w-[150px] px-3.5 py-1.5 text-sm border border-slate-200 rounded-full bg-white outline-none focus:border-indigo-400 transition-colors placeholder:text-slate-300"
      />
    </div>
  )
}
