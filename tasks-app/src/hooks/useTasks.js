// Hook personnalisé : CRUD des tâches avec persistance localStorage
import { useState } from 'react'

const STORAGE_KEY = 'tasks_v1'

function load() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) || [] }
  catch { return [] }
}

function save(tasks) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks))
}

function uid() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2)
}

export function useTasks() {
  const [tasks, setTasks] = useState(load)

  function update(next) {
    save(next)
    setTasks(next)
  }

  return {
    tasks,
    addTask:    (data) => update([...tasks, { id: uid(), done: false, createdAt: new Date().toISOString(), ...data }]),
    updateTask: (id, data) => update(tasks.map(t => t.id === id ? { ...t, ...data } : t)),
    deleteTask: (id) => update(tasks.filter(t => t.id !== id)),
    toggleDone: (id) => update(tasks.map(t => t.id === id ? { ...t, done: !t.done } : t)),
  }
}
