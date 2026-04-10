// Composant racine : état global, filtrage, orchestration des composants
import { useState, useMemo, useCallback } from 'react'
import { useTasks } from './hooks/useTasks.js'
import { today } from './utils/dates.js'
import Header      from './components/Header.jsx'
import Toolbar     from './components/Toolbar.jsx'
import TaskCard    from './components/TaskCard.jsx'
import TaskModal   from './components/TaskModal.jsx'
import CalendarView from './components/CalendarView.jsx'

const FILTER_FN = {
  all:      () => true,
  active:   t => !t.done && (!t.end || t.end >= today()),
  upcoming: t => !t.done && t.start && t.start > today(),
  overdue:  t => !t.done && t.end && t.end < today(),
  done:     t => t.done,
}

export default function App() {
  const { tasks, addTask, updateTask, deleteTask, toggleDone } = useTasks()
  const [view, setView]   = useState('list')           // 'list' | 'calendar'
  const [filter, setFilter] = useState('all')
  const [search, setSearch] = useState('')
  const [modal, setModal]   = useState({ open: false, taskId: null, prefill: null })

  const filtered = useMemo(() => {
    const q = search.toLowerCase().trim()
    return tasks
      .filter(FILTER_FN[filter])
      .filter(t => !q || t.title.toLowerCase().includes(q) || (t.desc || '').toLowerCase().includes(q))
      .sort((a, b) => {
        if (a.done !== b.done) return a.done ? 1 : -1
        if (a.end && b.end) return a.end.localeCompare(b.end)
        if (a.end) return -1
        if (b.end) return 1
        return b.createdAt.localeCompare(a.createdAt)
      })
  }, [tasks, filter, search])

  function openNew()    { setModal({ open: true, taskId: null, prefill: null }) }
  function openEdit(id) { setModal({ open: true, taskId: id,   prefill: null }) }
  function closeModal() { setModal({ open: false, taskId: null, prefill: null }) }

  // Appelé depuis CalendarView lors d'un clic/glisser sur une plage de dates
  const openFromCalendar = useCallback((start, end) => {
    setModal({ open: true, taskId: null, prefill: { start, end } })
  }, [])

  function handleSave(data) {
    modal.taskId ? updateTask(modal.taskId, data) : addTask(data)
    closeModal()
  }

  const editingTask = modal.taskId ? tasks.find(t => t.id === modal.taskId) : null

  return (
    <div className="min-h-screen bg-slate-50">
      <div className={`mx-auto px-4 py-10 transition-all ${view === 'calendar' ? 'max-w-5xl' : 'max-w-2xl'}`}>
        <Header onNew={openNew} view={view} onView={setView} />

        {view === 'list' ? (
          <>
            <Toolbar filter={filter} onFilter={setFilter} search={search} onSearch={setSearch} />

            {filtered.length === 0 ? (
              <p className="text-center text-slate-400 text-sm mt-16">
                Aucune tâche pour ce filtre.
              </p>
            ) : (
              <div className="flex flex-col gap-3">
                {filtered.map(t => (
                  <TaskCard
                    key={t.id}
                    task={t}
                    onToggle={toggleDone}
                    onEdit={openEdit}
                    onDelete={deleteTask}
                  />
                ))}
              </div>
            )}
          </>
        ) : (
          <CalendarView
            tasks={tasks}
            onNewTask={openFromCalendar}
            onEditTask={openEdit}
          />
        )}
      </div>

      {modal.open && (
        <TaskModal
          task={editingTask}
          prefill={!editingTask ? modal.prefill : null}
          onSave={handleSave}
          onClose={closeModal}
        />
      )}
    </div>
  )
}
