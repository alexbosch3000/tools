// Gestionnaire de tâches — logique principale
// Stockage : localStorage, aucune dépendance externe

'use strict';

/* ── Stockage ────────────────────────────────────────── */

const STORAGE_KEY = 'tasks_v1';

function loadTasks() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) || []; }
  catch { return []; }
}

function saveTasks(tasks) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks));
}

/* ── Helpers dates ───────────────────────────────────── */

// Retourne la date du jour au format YYYY-MM-DD
function today() {
  return new Date().toISOString().slice(0, 10);
}

// Formate une date ISO en dd/mm/yyyy pour l'affichage
function fmtDate(iso) {
  if (!iso) return null;
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

function isPast(iso) { return iso && iso < today(); }
function isToday(iso) { return iso === today(); }

/* ── Génération d'ID unique ──────────────────────────── */
function uid() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2);
}

/* ── État global ─────────────────────────────────────── */

let tasks = loadTasks();
let currentFilter = 'all';
let searchQuery = '';
let editingId = null;

/* ── Filtrage ────────────────────────────────────────── */

function applyFilters(list) {
  const td = today();

  return list.filter(t => {
    // Filtre texte
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      if (!t.title.toLowerCase().includes(q) &&
          !(t.desc || '').toLowerCase().includes(q)) return false;
    }

    // Filtre statut
    switch (currentFilter) {
      case 'done':     return t.done;
      case 'active':   return !t.done && (!t.end || t.end >= td);
      case 'upcoming': return !t.done && t.start && t.start > td;
      case 'overdue':  return !t.done && t.end && t.end < td;
      default:         return true;
    }
  });
}

/* ── Rendu ───────────────────────────────────────────── */

function renderTasks() {
  const list = document.getElementById('task-list');
  const empty = document.getElementById('empty-state');
  const filtered = applyFilters(tasks);

  list.innerHTML = '';

  if (filtered.length === 0) {
    empty.hidden = false;
    return;
  }
  empty.hidden = true;

  // Tri : tâches non terminées d'abord, puis par date de fin
  const sorted = [...filtered].sort((a, b) => {
    if (a.done !== b.done) return a.done ? 1 : -1;
    if (a.end && b.end) return a.end.localeCompare(b.end);
    if (a.end) return -1;
    if (b.end) return 1;
    return b.createdAt.localeCompare(a.createdAt);
  });

  sorted.forEach(t => list.appendChild(buildCard(t)));
}

function buildCard(t) {
  const card = document.createElement('article');
  card.className = 'task-card' + (t.done ? ' done' : '');
  card.dataset.priority = t.priority || 'medium';

  // ── Checkbox ──
  const check = document.createElement('div');
  check.className = 'task-check';
  check.setAttribute('role', 'checkbox');
  check.setAttribute('aria-checked', String(t.done));
  check.setAttribute('tabindex', '0');
  check.innerHTML = `<svg width="11" height="9" viewBox="0 0 11 9" fill="none">
    <path d="M1 4L4 7.5L10 1" stroke="white" stroke-width="2" stroke-linecap="round"/>
  </svg>`;
  check.addEventListener('click', () => toggleDone(t.id));
  check.addEventListener('keydown', e => { if (e.key === ' ' || e.key === 'Enter') toggleDone(t.id); });

  // ── Corps ──
  const body = document.createElement('div');
  body.className = 'task-body';

  const title = document.createElement('div');
  title.className = 'task-title';
  title.textContent = t.title;
  body.appendChild(title);

  if (t.desc) {
    const desc = document.createElement('div');
    desc.className = 'task-desc';
    desc.textContent = t.desc;
    body.appendChild(desc);
  }

  // Tags meta
  const meta = document.createElement('div');
  meta.className = 'task-meta';

  // Priorité
  const pLabels = { low: 'Basse', medium: 'Moyenne', high: 'Haute' };
  const pTag = document.createElement('span');
  pTag.className = `tag tag-priority-${t.priority || 'medium'}`;
  pTag.textContent = pLabels[t.priority] || 'Moyenne';
  meta.appendChild(pTag);

  // Date début
  if (t.start) {
    const startTag = document.createElement('span');
    startTag.className = 'tag tag-date';
    startTag.textContent = `▶ ${fmtDate(t.start)}`;
    meta.appendChild(startTag);
  }

  // Date fin
  if (t.end) {
    const endTag = document.createElement('span');
    const past = isPast(t.end) && !t.done;
    const tod  = isToday(t.end) && !t.done;
    endTag.className = 'tag ' + (past ? 'tag-overdue' : tod ? 'tag-today' : 'tag-date');
    endTag.textContent = (past ? '⚠ ' : '⏰ ') + fmtDate(t.end);
    meta.appendChild(endTag);
  }

  body.appendChild(meta);

  // ── Actions ──
  const actions = document.createElement('div');
  actions.className = 'task-actions';

  const editBtn = document.createElement('button');
  editBtn.className = 'btn-icon';
  editBtn.setAttribute('aria-label', 'Modifier');
  editBtn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
  </svg>`;
  editBtn.addEventListener('click', () => openModal(t.id));

  const delBtn = document.createElement('button');
  delBtn.className = 'btn-icon danger';
  delBtn.setAttribute('aria-label', 'Supprimer');
  delBtn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <polyline points="3 6 5 6 21 6"/>
    <path d="M19 6l-1 14H6L5 6"/>
    <path d="M10 11v6M14 11v6"/>
    <path d="M9 6V4h6v2"/>
  </svg>`;
  delBtn.addEventListener('click', () => deleteTask(t.id));

  actions.appendChild(editBtn);
  actions.appendChild(delBtn);

  card.appendChild(check);
  card.appendChild(body);
  card.appendChild(actions);

  return card;
}

/* ── Actions CRUD ────────────────────────────────────── */

function toggleDone(id) {
  tasks = tasks.map(t => t.id === id ? { ...t, done: !t.done } : t);
  saveTasks(tasks);
  renderTasks();
}

function deleteTask(id) {
  if (!confirm('Supprimer cette tâche ?')) return;
  tasks = tasks.filter(t => t.id !== id);
  saveTasks(tasks);
  renderTasks();
}

function saveTask(data) {
  if (editingId) {
    tasks = tasks.map(t => t.id === editingId ? { ...t, ...data } : t);
  } else {
    tasks.push({ id: uid(), done: false, createdAt: new Date().toISOString(), ...data });
  }
  saveTasks(tasks);
  renderTasks();
}

/* ── Modal ───────────────────────────────────────────── */

function openModal(id = null) {
  editingId = id;
  const overlay = document.getElementById('modal-overlay');
  const title   = document.getElementById('modal-title');
  const form    = document.getElementById('task-form');
  const error   = document.getElementById('form-error');

  error.hidden = true;
  form.reset();

  if (id) {
    const t = tasks.find(x => x.id === id);
    if (!t) return;
    title.textContent = 'Modifier la tâche';
    document.getElementById('task-id').value       = t.id;
    document.getElementById('task-title').value    = t.title;
    document.getElementById('task-desc').value     = t.desc || '';
    document.getElementById('task-start').value    = t.start || '';
    document.getElementById('task-end').value      = t.end || '';
    document.getElementById('task-priority').value = t.priority || 'medium';
  } else {
    title.textContent = 'Nouvelle tâche';
  }

  overlay.hidden = false;
  document.getElementById('task-title').focus();
}

function closeModal() {
  document.getElementById('modal-overlay').hidden = true;
  editingId = null;
}

/* ── Soumission du formulaire ────────────────────────── */

document.getElementById('task-form').addEventListener('submit', e => {
  e.preventDefault();
  const error  = document.getElementById('form-error');
  const titleV = document.getElementById('task-title').value.trim();
  const start  = document.getElementById('task-start').value;
  const end    = document.getElementById('task-end').value;

  // Validation
  if (!titleV) {
    error.textContent = 'Le titre est obligatoire.';
    error.hidden = false;
    document.getElementById('task-title').focus();
    return;
  }
  if (start && end && end < start) {
    error.textContent = 'La date de fin ne peut pas être antérieure à la date de début.';
    error.hidden = false;
    return;
  }

  error.hidden = true;
  saveTask({
    title:    titleV,
    desc:     document.getElementById('task-desc').value.trim(),
    start:    start || null,
    end:      end   || null,
    priority: document.getElementById('task-priority').value,
  });
  closeModal();
});

/* ── Événements UI ───────────────────────────────────── */

document.getElementById('btn-new-task').addEventListener('click', () => openModal());
document.getElementById('btn-cancel').addEventListener('click', closeModal);
document.getElementById('modal-close').addEventListener('click', closeModal);

// Fermer en cliquant sur l'overlay
document.getElementById('modal-overlay').addEventListener('click', e => {
  if (e.target === e.currentTarget) closeModal();
});

// Fermer avec Échap
document.addEventListener('keydown', e => {
  if (e.key === 'Escape' && !document.getElementById('modal-overlay').hidden) closeModal();
});

// Filtres
document.querySelectorAll('.filter-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    currentFilter = btn.dataset.filter;
    renderTasks();
  });
});

// Recherche (debounce 200 ms)
let searchTimer;
document.getElementById('search').addEventListener('input', e => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    searchQuery = e.target.value.trim();
    renderTasks();
  }, 200);
});

/* ── Démarrage ───────────────────────────────────────── */
renderTasks();
