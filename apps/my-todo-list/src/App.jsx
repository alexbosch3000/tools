// Root component: holds UI state and wires the list and calendar views to the task store.
import { useState, useMemo, useCallback } from 'react'
import { useTasks } from './hooks/useTasks.js'
import { selectTasks } from './utils/tasks.js'
import Header      from './components/Header.jsx'
import Toolbar     from './components/Toolbar.jsx'
import TaskCard    from './components/TaskCard.jsx'
import TaskModal   from './components/TaskModal.jsx'
import CalendarView from './components/CalendarView.jsx'

export default function App() {
  const { tasks, addTask, updateTask, deleteTask, toggleDone } = useTasks()
  const [view, setView]   = useState('list')           // 'list' | 'calendar'
  const [filter, setFilter] = useState('all')
  const [search, setSearch] = useState('')
  const [modal, setModal]   = useState({ open: false, taskId: null, prefill: null })

  const filtered = useMemo(() => selectTasks(tasks, filter, search), [tasks, filter, search])

  function openNew()    { setModal({ open: true, taskId: null, prefill: null }) }
  function openEdit(id) { setModal({ open: true, taskId: id,   prefill: null }) }
  function closeModal() { setModal({ open: false, taskId: null, prefill: null }) }

  // Called by CalendarView when the user clicks a day or drags across a date range.
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
