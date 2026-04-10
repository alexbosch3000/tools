// En-tête de l'application avec titre et bouton de création
export default function Header({ onNew }) {
  return (
    <div className="flex items-center justify-between mb-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Mes tâches</h1>
        <p className="text-sm text-slate-400 mt-0.5">Gérez vos tâches et projets</p>
      </div>
      <button
        onClick={onNew}
        className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 active:scale-95 text-white px-4 py-2 rounded-lg font-medium text-sm transition-all"
      >
        <span className="text-base leading-none">+</span>
        Nouvelle tâche
      </button>
    </div>
  )
}
