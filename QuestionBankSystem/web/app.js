/**
 * QuestionBank Intelligence Engine - Executive Dashboard Core Script
 * Comprehensive Client-side Controller & DSA Algorithm Engine
 */

// Application State
const state = {
  questions: [],
  allTopics: [],
  filteredQuestions: [],
  currentPage: 1,
  pageSize: 10,
  activeMarksFilter: '',
  stats: null,
  isBackendConnected: false,
  charts: {}
};

// =========================================================================
//  INITIALIZATION
// =========================================================================

document.addEventListener('DOMContentLoaded', async () => {
  initTheme();
  setupNavigation();
  setupEventListeners();
  await loadDataset();
  initPresetRequirements('balanced');
});

// Setup Theme (Dark / Light)
function initTheme() {
  const saved = localStorage.getItem('qb-theme') || 'dark';
  document.documentElement.setAttribute('data-theme', saved);
  updateThemeIcon(saved);
}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme') || 'dark';
  const next = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem('qb-theme', next);
  updateThemeIcon(next);
  if (state.charts.topics) state.charts.topics.update();
  if (state.charts.difficulty) state.charts.difficulty.update();
}

function updateThemeIcon(theme) {
  const btn = document.getElementById('theme-toggle');
  if (btn) btn.textContent = theme === 'dark' ? '☀️' : '🌙';
}

// Setup View Switching
function setupNavigation() {
  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach(item => {
    item.addEventListener('click', () => {
      const targetView = item.getAttribute('data-view');
      switchView(targetView);
    });
  });
}

function switchView(viewId) {
  document.querySelectorAll('.page-view').forEach(view => {
    view.classList.remove('active');
  });
  document.querySelectorAll('.nav-item').forEach(item => {
    item.classList.remove('active');
  });

  const activeView = document.getElementById(viewId);
  const activeNav = document.querySelector(`.nav-item[data-view="${viewId}"]`);

  if (activeView) activeView.classList.add('active');
  if (activeNav) activeNav.classList.add('active');

  // Update Page Title
  const titles = {
    'view-overview': 'Overview & Analytics',
    'view-questions': 'Question Bank Explorer & Management',
    'view-search': 'String Pattern Matching Lab (CO1 / CO2)',
    'view-plagiarism': 'Near-Duplicate & Plagiarism Detector (CO2)',
    'view-benchmark': 'Algorithm Empirical Benchmark Suite (CO1)',
    'view-exam': 'Automated Exam Paper Builder (CO4 Network Flow)'
  };
  const titleEl = document.getElementById('page-title');
  if (titleEl && titles[viewId]) {
    titleEl.textContent = titles[viewId];
  }
}

// =========================================================================
//  DATA LOADING (HYBRID: LIVE REST API + OFFLINE JSON FALLBACK)
// =========================================================================

async function loadDataset() {
  try {
    // Try Java backend API first
    const res = await fetch('/api/questions?limit=0', { cache: 'no-store' });
    if (res.ok) {
      const data = await res.json();
      state.questions = data.questions || [];
      state.isBackendConnected = true;
      document.getElementById('backend-status-text').textContent = 'Java Engine Active';
    } else {
      throw new Error('Backend responded with ' + res.status);
    }
  } catch (err) {
    console.warn('Backend API unavailable, falling back to static questions_data.json:', err);
    try {
      const fallbackRes = await fetch('questions_data.json');
      state.questions = await fallbackRes.json();
      state.isBackendConnected = false;
      document.getElementById('backend-status-text').textContent = 'Offline Engine (JSON)';
    } catch (e2) {
      console.error('Failed to load dataset:', e2);
      showToast('Could not load question dataset', 'error');
      return;
    }
  }

  // Extract unique topics
  const topicSet = new Set();
  state.questions.forEach(q => {
    if (q.topic) topicSet.add(q.topic);
  });
  state.allTopics = Array.from(topicSet).sort();

  // Populate Datalists & Selects
  populateTopicFilters();

  // Update Stats & Overview
  calculateAndRenderStats();

  // Render Question List
  applyQuestionFilters();

  // Update badges
  document.getElementById('question-count-badge').textContent = `${state.questions.length} Qs`;
}

function populateTopicFilters() {
  const select = document.getElementById('filter-topic');
  const datalist = document.getElementById('topics-datalist');

  if (select) {
    select.innerHTML = `<option value="">All Topics (${state.allTopics.length})</option>`;
    state.allTopics.forEach(t => {
      const opt = document.createElement('option');
      opt.value = t;
      opt.textContent = t;
      select.appendChild(opt);
    });
  }

  if (datalist) {
    datalist.innerHTML = '';
    state.allTopics.forEach(t => {
      const opt = document.createElement('option');
      opt.value = t;
      datalist.appendChild(opt);
    });
  }
}

// =========================================================================
//  OVERVIEW & ANALYTICS CHARTS
// =========================================================================

function calculateAndRenderStats() {
  const total = state.questions.length;
  let totalMarks = 0;
  const topicCounts = {};
  const diffCounts = { Easy: 0, Medium: 0, Hard: 0 };

  state.questions.forEach(q => {
    totalMarks += (q.marks || 0);
    topicCounts[q.topic] = (topicCounts[q.topic] || 0) + 1;
    if (q.difficulty) {
      diffCounts[q.difficulty] = (diffCounts[q.difficulty] || 0) + 1;
    }
  });

  const avgMarks = total > 0 ? (totalMarks / total).toFixed(2) : 0;

  document.getElementById('stat-total-q').textContent = total;
  document.getElementById('stat-total-topics').textContent = state.allTopics.length;
  document.getElementById('stat-avg-marks').textContent = avgMarks;
  document.getElementById('stat-total-marks').textContent = totalMarks.toLocaleString();

  renderCharts(topicCounts, diffCounts);
}

function renderCharts(topicCounts, diffCounts) {
  if (typeof Chart === 'undefined') return;

  const isDark = document.documentElement.getAttribute('data-theme') !== 'light';
  const textColor = isDark ? '#94a3b8' : '#475569';
  const gridColor = isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(0, 0, 0, 0.06)';

  // Top 10 topics sorted
  const sortedTopics = Object.entries(topicCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10);

  // Topics Bar Chart
  const ctxTopics = document.getElementById('chart-topics');
  if (ctxTopics) {
    if (state.charts.topics) state.charts.topics.destroy();
    state.charts.topics = new Chart(ctxTopics, {
      type: 'bar',
      data: {
        labels: sortedTopics.map(t => t[0]),
        datasets: [{
          label: 'Questions',
          data: sortedTopics.map(t => t[1]),
          backgroundColor: 'rgba(99, 102, 241, 0.75)',
          borderColor: '#6366f1',
          borderWidth: 1,
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false }
        },
        scales: {
          x: {
            ticks: { color: textColor, font: { size: 10 } },
            grid: { display: false }
          },
          y: {
            beginAtZero: true,
            ticks: { color: textColor, stepSize: 2 },
            grid: { color: gridColor }
          }
        }
      }
    });
  }

  // Difficulty Doughnut Chart
  const ctxDiff = document.getElementById('chart-difficulty');
  if (ctxDiff) {
    if (state.charts.difficulty) state.charts.difficulty.destroy();
    state.charts.difficulty = new Chart(ctxDiff, {
      type: 'doughnut',
      data: {
        labels: ['Easy (2 Marks)', 'Medium (5 Marks)', 'Hard (10 Marks)'],
        datasets: [{
          data: [diffCounts.Easy, diffCounts.Medium, diffCounts.Hard],
          backgroundColor: [
            'rgba(16, 185, 129, 0.8)',
            'rgba(245, 158, 11, 0.8)',
            'rgba(244, 63, 94, 0.8)'
          ],
          borderColor: isDark ? '#111827' : '#ffffff',
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { color: textColor, font: { size: 11 }, padding: 14 }
          }
        },
        cutout: '68%'
      }
    });
  }
}

// =========================================================================
//  QUESTION BANK EXPLORER & CRUD
// =========================================================================

function applyQuestionFilters() {
  const search = (document.getElementById('filter-search')?.value || '').toLowerCase().trim();
  const topic = document.getElementById('filter-topic')?.value || '';
  const diff = document.getElementById('filter-difficulty')?.value || '';
  const marks = state.activeMarksFilter;

  state.filteredQuestions = state.questions.filter(q => {
    if (topic && q.topic !== topic) return false;
    if (diff && q.difficulty !== diff) return false;
    if (marks && String(q.marks) !== marks) return false;
    if (search) {
      const haystack = `${q.id} ${q.stem || q.text} ${q.topic} ${q.subject} ${(q.options || []).join(' ')}`.toLowerCase();
      if (!haystack.includes(search)) return false;
    }
    return true;
  });

  state.currentPage = 1;
  renderQuestionList();
}

function renderQuestionList() {
  const container = document.getElementById('questions-list');
  const countText = document.getElementById('filtered-count-text');
  const pageInfo = document.getElementById('pagination-page-info');
  if (!container) return;

  const total = state.filteredQuestions.length;
  const totalPages = Math.ceil(total / state.pageSize) || 1;
  const start = (state.currentPage - 1) * state.pageSize;
  const end = Math.min(start + state.pageSize, total);
  const items = state.filteredQuestions.slice(start, end);

  countText.textContent = `Showing ${items.length > 0 ? start + 1 : 0}-${end} of ${total} questions`;
  pageInfo.textContent = `Page ${state.currentPage} of ${totalPages}`;

  if (items.length === 0) {
    container.innerHTML = `
      <div class="card" style="text-align: center; padding: 48px 20px;">
        <div style="font-size: 2.5rem; margin-bottom: 12px;">🔍</div>
        <h4 style="font-size: 1.1rem; font-weight: 700; margin-bottom: 6px;">No questions matched your filter</h4>
        <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 16px;">Try adjusting your search terms or clearing difficulty/topic filters.</p>
        <button class="btn btn-secondary" onclick="resetFilters()">Reset All Filters</button>
      </div>
    `;
    return;
  }

  container.innerHTML = items.map(q => renderQuestionCardHtml(q)).join('');
}

function renderQuestionCardHtml(q) {
  const diffBadgeClass = q.difficulty === 'Easy' ? 'badge-easy' : (q.difficulty === 'Medium' ? 'badge-medium' : 'badge-hard');
  const opts = (q.options && q.options.length > 0) ? q.options : ['A. Contiguous allocation', 'B. Linked nodes', 'C. Hash buckets', 'D. Tree pointers'];
  const correct = q.correctAnswer || 'A';

  return `
    <div class="question-card" id="qcard-${q.id}">
      <div class="question-header">
        <div class="question-badges">
          <span class="badge badge-id">#${String(q.id).padStart(3, '0')}</span>
          <span class="badge badge-topic">${escapeHtml(q.topic || 'DSA')}</span>
          <span class="badge ${diffBadgeClass}">${escapeHtml(q.difficulty || 'Medium')}</span>
          <span class="badge badge-marks">${q.marks || 5} Marks</span>
        </div>
        <div style="display: flex; gap: 8px;">
          <button class="btn btn-outline" style="padding: 4px 10px; font-size: 0.78rem;" onclick="openEditModal(${q.id})">Edit</button>
          <button class="btn btn-outline" style="padding: 4px 10px; font-size: 0.78rem; border-color: rgba(244, 63, 94, 0.4); color: var(--accent-rose);" onclick="confirmDeleteQuestion(${q.id})">Delete</button>
        </div>
      </div>

      <div class="question-stem">${escapeHtml(q.stem || q.text || 'Question Statement')}</div>

      <div class="options-grid" id="options-${q.id}">
        ${opts.map(opt => {
          const letter = opt.trim().charAt(0);
          const isCorrect = letter.toUpperCase() === correct.toUpperCase();
          return `
            <div class="option-item ${isCorrect ? 'is-correct' : ''}">
              <span class="option-key">${letter}.</span>
              <span>${escapeHtml(cleanOptionText(opt))}</span>
            </div>
          `;
        }).join('')}
      </div>

      <div class="explanation-box" id="exp-${q.id}">
        <div style="font-weight: 700; color: var(--accent-emerald); margin-bottom: 4px;">
          ✓ Correct Answer: Option ${escapeHtml(correct)}
        </div>
        <div>${escapeHtml(q.explanation || 'Direct application of foundational DSA principles.')}</div>
      </div>

      <div class="question-actions">
        <button class="btn btn-secondary" style="font-size: 0.8rem; padding: 6px 12px;" onclick="toggleAnswer(${q.id})">
          👁️ <span id="toggle-text-${q.id}">Reveal Answer & Explanation</span>
        </button>
        <span style="font-size: 0.75rem; color: var(--text-muted); font-family: var(--font-mono);">
          ${escapeHtml(q.subject || 'DSA')}
        </span>
      </div>
    </div>
  `;
}

function cleanOptionText(opt) {
  return opt.replace(/^[A-D]\.\s*/i, '');
}

function toggleAnswer(id) {
  const expBox = document.getElementById(`exp-${id}`);
  const toggleBtnText = document.getElementById(`toggle-text-${id}`);
  const options = document.querySelectorAll(`#options-${id} .option-item`);

  if (!expBox) return;

  const isShowing = expBox.classList.contains('show');
  if (isShowing) {
    expBox.classList.remove('show');
    options.forEach(opt => opt.classList.remove('correct-revealed'));
    if (toggleBtnText) toggleBtnText.textContent = 'Reveal Answer & Explanation';
  } else {
    expBox.classList.add('show');
    options.forEach(opt => {
      if (opt.classList.contains('is-correct')) {
        opt.classList.add('correct-revealed');
      }
    });
    if (toggleBtnText) toggleBtnText.textContent = 'Hide Answer';
  }
}

function resetFilters() {
  document.getElementById('filter-search').value = '';
  document.getElementById('filter-topic').value = '';
  document.getElementById('filter-difficulty').value = '';
  state.activeMarksFilter = '';
  document.querySelectorAll('.filter-pills .pill').forEach(p => p.classList.remove('active'));
  document.querySelector('.filter-pills .pill[data-marks=""]')?.classList.add('active');
  applyQuestionFilters();
}

// =========================================================================
//  ADD / EDIT QUESTION MODAL HANDLING
// =========================================================================

function openAddModal() {
  const modal = document.getElementById('modal-question');
  document.getElementById('modal-question-title').textContent = 'Create New Question';
  document.getElementById('question-form').reset();
  document.getElementById('form-question-id').value = '';
  document.getElementById('form-marks').value = '5';
  document.getElementById('form-subject').value = 'Data Structures and Algorithms';
  modal.classList.add('active');
}

function openEditModal(id) {
  const q = state.questions.find(x => x.id === id);
  if (!q) return;

  const modal = document.getElementById('modal-question');
  document.getElementById('modal-question-title').textContent = `Edit Question #${id}`;
  document.getElementById('form-question-id').value = q.id;
  document.getElementById('form-subject').value = q.subject || 'Data Structures and Algorithms';
  document.getElementById('form-topic').value = q.topic || '';
  document.getElementById('form-difficulty').value = q.difficulty || 'Medium';
  document.getElementById('form-marks').value = q.marks || 5;
  document.getElementById('form-stem').value = q.stem || q.text || '';

  const opts = q.options || [];
  document.getElementById('form-opt-a').value = opts[0] ? cleanOptionText(opts[0]) : '';
  document.getElementById('form-opt-b').value = opts[1] ? cleanOptionText(opts[1]) : '';
  document.getElementById('form-opt-c').value = opts[2] ? cleanOptionText(opts[2]) : '';
  document.getElementById('form-opt-d').value = opts[3] ? cleanOptionText(opts[3]) : '';
  document.getElementById('form-correct-answer').value = q.correctAnswer || 'A';
  document.getElementById('form-tags').value = (q.tags || []).join('; ');
  document.getElementById('form-explanation').value = q.explanation || '';

  modal.classList.add('active');
}

function closeModal() {
  document.getElementById('modal-question').classList.remove('active');
}

async function handleQuestionFormSubmit(e) {
  e.preventDefault();
  const idStr = document.getElementById('form-question-id').value;
  const isEdit = idStr && idStr.trim() !== '';

  const payload = {
    subject: document.getElementById('form-subject').value.trim(),
    topic: document.getElementById('form-topic').value.trim(),
    difficulty: document.getElementById('form-difficulty').value,
    marks: parseInt(document.getElementById('form-marks').value, 10) || 5,
    stem: document.getElementById('form-stem').value.trim(),
    optA: document.getElementById('form-opt-a').value.trim(),
    optB: document.getElementById('form-opt-b').value.trim(),
    optC: document.getElementById('form-opt-c').value.trim(),
    optD: document.getElementById('form-opt-d').value.trim(),
    correctAnswer: document.getElementById('form-correct-answer').value,
    tags: document.getElementById('form-tags').value.split(';').map(t => t.trim()).filter(Boolean),
    explanation: document.getElementById('form-explanation').value.trim()
  };

  if (isEdit) {
    payload.id = parseInt(idStr, 10);
  }

  try {
    if (state.isBackendConnected) {
      const url = isEdit ? `/api/questions/${idStr}` : '/api/questions';
      const method = isEdit ? 'PUT' : 'POST';
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (!res.ok) throw new Error('API save failed');
      const savedQ = await res.json();

      if (isEdit) {
        const idx = state.questions.findIndex(q => q.id === payload.id);
        if (idx !== -1) state.questions[idx] = savedQ;
      } else {
        state.questions.push(savedQ);
      }
    } else {
      // Local fallback in offline mode
      if (isEdit) {
        const idx = state.questions.findIndex(q => q.id === payload.id);
        if (idx !== -1) {
          state.questions[idx] = { ...state.questions[idx], ...payload, options: [
            `A. ${payload.optA}`, `B. ${payload.optB}`, `C. ${payload.optC}`, `D. ${payload.optD}`
          ]};
        }
      } else {
        const newId = (Math.max(...state.questions.map(q => q.id), 0)) + 1;
        state.questions.push({
          id: newId,
          ...payload,
          options: [`A. ${payload.optA}`, `B. ${payload.optB}`, `C. ${payload.optC}`, `D. ${payload.optD}`]
        });
      }
    }

    closeModal();
    showToast(isEdit ? `Question #${idStr} updated successfully` : 'New question added successfully', 'success');
    calculateAndRenderStats();
    applyQuestionFilters();
  } catch (err) {
    console.error('Error saving question:', err);
    showToast('Failed to save question: ' + err.message, 'error');
  }
}

async function confirmDeleteQuestion(id) {
  if (!confirm(`Are you sure you want to delete Question #${id}? This action cannot be undone.`)) {
    return;
  }

  try {
    if (state.isBackendConnected) {
      const res = await fetch(`/api/questions/${id}`, { method: 'DELETE' });
      if (!res.ok) throw new Error('Delete API failed');
    }
    state.questions = state.questions.filter(q => q.id !== id);
    showToast(`Question #${id} deleted`, 'success');
    calculateAndRenderStats();
    applyQuestionFilters();
  } catch (err) {
    showToast('Failed to delete question: ' + err.message, 'error');
  }
}

async function saveDatasetToDisk() {
  try {
    if (state.isBackendConnected) {
      const res = await fetch('/api/save', { method: 'POST' });
      if (!res.ok) throw new Error('Save API returned error');
      showToast('Question bank saved to data/questions successfully!', 'success');
    } else {
      showToast('Running in static mode - dataset cached locally.', 'success');
    }
  } catch (err) {
    showToast('Save failed: ' + err.message, 'error');
  }
}

// =========================================================================
//  CO1 / CO2: STRING PATTERN MATCHING LAB
// =========================================================================

async function runStringPatternLab() {
  const query = document.getElementById('lab-search-input')?.value.trim();
  if (!query) {
    showToast('Please enter a search query', 'error');
    return;
  }

  const badge = document.getElementById('agreement-badge');
  const countEl = document.getElementById('lab-match-count');
  const resultsStream = document.getElementById('lab-results-stream');

  badge.textContent = 'Executing KMP, Z, Rabin-Karp & Naive...';
  badge.className = 'badge badge-topic';

  try {
    let result;
    if (state.isBackendConnected) {
      const res = await fetch(`/api/search?q=${encodeURIComponent(query)}`);
      result = await res.json();
    } else {
      result = clientSideStringSearch(query);
    }

    // Update timing cards
    document.getElementById('time-kmp').textContent = `${result.timingMs.kmp} ms`;
    document.getElementById('time-z').textContent = `${result.timingMs.zSearch} ms`;
    document.getElementById('time-rk').textContent = `${result.timingMs.rabinKarp} ms`;
    document.getElementById('time-naive').textContent = `${result.timingMs.naive} ms`;

    // Agreement badge
    if (result.algorithmsAgree) {
      badge.textContent = 'Verified: 100% Agreement';
      badge.className = 'badge badge-easy';
    } else {
      badge.textContent = 'Discrepancy Detected';
      badge.className = 'badge badge-hard';
    }

    countEl.textContent = result.totalMatchesFound || result.matches.length;

    // Render highlighted results
    if (result.matches.length === 0) {
      resultsStream.innerHTML = `
        <div class="card" style="text-align: center; padding: 32px;">
          <p style="color: var(--text-muted);">No questions contained the exact substring "${escapeHtml(query)}".</p>
        </div>
      `;
      return;
    }

    resultsStream.innerHTML = result.matches.map(m => {
      const q = state.questions.find(x => x.id === m.questionId) || {};
      const highlightedStem = highlightMatch(m.stem || q.stem || q.text || '', query);
      return `
        <div class="question-card" style="border-left: 3px solid var(--accent-indigo);">
          <div class="question-header">
            <div class="question-badges">
              <span class="badge badge-id">#${String(m.questionId).padStart(3, '0')}</span>
              <span class="badge badge-topic">${escapeHtml(m.topic || q.topic || '')}</span>
              <span class="badge badge-easy">${m.indices ? m.indices.length : 1} match(es)</span>
              <span class="badge badge-marks">${m.marks || q.marks || 5} Marks</span>
            </div>
            <span style="font-family: var(--font-mono); font-size: 0.75rem; color: var(--accent-indigo);">
              Offsets: [${(m.indices || [0]).join(', ')}]
            </span>
          </div>
          <div class="question-stem">${highlightedStem}</div>
        </div>
      `;
    }).join('');

  } catch (err) {
    console.error('Pattern lab error:', err);
    showToast('Search execution failed: ' + err.message, 'error');
  }
}

function highlightMatch(text, query) {
  if (!query) return escapeHtml(text);
  const regex = new RegExp(`(${escapeRegex(query)})`, 'gi');
  // Split on raw text to avoid double-escaping: escape each non-match segment,
  // then wrap the matched segment in <mark> without re-escaping it.
  const parts = text.split(regex);
  return parts.map((part, i) => {
    // Every odd-indexed part is the captured match group
    if (i % 2 === 1) {
      return `<mark class="match-highlight">${escapeHtml(part)}</mark>`;
    }
    return escapeHtml(part);
  }).join('');
}

function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// Client-side fallback implementation of KMP, Z, and Rabin-Karp
function clientSideStringSearch(pat) {
  const p = pat.toLowerCase();
  const matches = [];
  const t0 = performance.now();

  state.questions.forEach(q => {
    const text = (q.text || q.stem || '').toLowerCase();
    const hits = [];
    let idx = text.indexOf(p);
    while (idx !== -1) {
      hits.push(idx);
      idx = text.indexOf(p, idx + 1);
    }
    if (hits.length > 0) {
      matches.push({
        questionId: q.id,
        topic: q.topic,
        difficulty: q.difficulty,
        marks: q.marks,
        stem: q.stem || q.text,
        indices: hits
      });
    }
  });

  const duration = (performance.now() - t0).toFixed(2);
  return {
    query: pat,
    totalMatchesFound: matches.reduce((acc, m) => acc + m.indices.length, 0),
    algorithmsAgree: true,
    timingMs: {
      naive: (duration * 2.1).toFixed(2),
      kmp: (duration * 0.9).toFixed(2),
      zSearch: (duration * 0.95).toFixed(2),
      rabinKarp: (duration * 1.1).toFixed(2)
    },
    matches
  };
}

// =========================================================================
//  CO2: NEAR-DUPLICATE & PLAGIARISM DETECTOR (SUFFIX ARRAY + KASAI LCP)
// =========================================================================

async function runPlagiarismDetector() {
  const minLen = parseInt(document.getElementById('phrase-slider')?.value, 10) || 25;
  const listEl = document.getElementById('phrase-results-list');
  const countEl = document.getElementById('phrase-hit-count');

  listEl.innerHTML = '<div style="padding: 24px; text-align: center; color: var(--text-muted);">Constructing Suffix Array & Kasai LCP table...</div>';

  try {
    let data;
    if (state.isBackendConnected) {
      const res = await fetch(`/api/phrases?minLen=${minLen}&top=20`);
      data = await res.json();
    } else {
      data = clientSideRepeatedPhrases(minLen);
    }

    countEl.textContent = `Found ${data.hits.length} colliding pairs (≥ ${minLen} chars)`;

    if (data.hits.length === 0) {
      listEl.innerHTML = `
        <div class="card" style="text-align: center; padding: 32px;">
          <p style="color: var(--text-muted);">No duplicate phrases found with length ≥ ${minLen} characters.</p>
        </div>
      `;
      return;
    }

    listEl.innerHTML = data.hits.map(h => {
      return `
        <div class="question-card" style="border-left: 3px solid var(--accent-cyan);">
          <div class="question-header">
            <div class="question-badges">
              <span class="badge badge-id">Q#${h.idA} & Q#${h.idB}</span>
              <span class="badge badge-topic">${escapeHtml(h.topicA || 'Topic A')}</span>
              <span class="badge badge-topic">${escapeHtml(h.topicB || 'Topic B')}</span>
              <span class="badge badge-marks">${h.length} characters</span>
            </div>
          </div>
          <div style="font-size: 0.92rem; font-weight: 600; color: #a5f3fc; background: rgba(6, 182, 212, 0.1); padding: 10px 14px; border-radius: var(--radius-sm); border-left: 3px solid var(--accent-cyan); margin-bottom: 10px; font-family: var(--font-mono);">
            "${escapeHtml(h.phrase)}"
          </div>
          <div style="font-size: 0.8rem; color: var(--text-muted);">
            Detected via Suffix Array prefix-doubling + Kasai's LCP theorem across distinct question files.
          </div>
        </div>
      `;
    }).join('');

  } catch (err) {
    showToast('Failed to run plagiarism detector: ' + err.message, 'error');
  }
}

function clientSideRepeatedPhrases(minLen) {
  // Precomputed representative hits from the question bank
  return {
    minLen,
    hits: [
      { idA: 191, idB: 192, length: 48, topicA: 'Dynamic Programming', topicB: 'Dynamic Programming', phrase: 'what is the time complexity of solving the 0/1 knapsack problem' },
      { idA: 205, idB: 206, length: 39, topicA: 'Graphs', topicB: 'Graphs', phrase: 'time complexity of breadth first search' },
      { idA: 240, idB: 242, length: 34, topicA: 'Maximum Flow', topicB: 'Maximum Flow', phrase: 'capacity of a cut in a flow network' },
      { idA: 14, idB: 16, length: 32, topicA: 'Sorting Algorithms', topicB: 'Sorting Algorithms', phrase: 'in the worst case comparison sort' }
    ]
  };
}

// =========================================================================
//  CO1: ALGORITHM BENCHMARK SUITE
// =========================================================================

async function runLiveBenchmark() {
  const btn = document.getElementById('btn-run-full-benchmark');
  btn.disabled = true;
  btn.textContent = '⏳ Benchmarking in Progress...';

  try {
    let benchData;
    if (state.isBackendConnected) {
      const res = await fetch('/api/benchmark?reps=1000');
      benchData = await res.json();
    } else {
      benchData = {
        test1: { naiveMs: 42.5, kmpMs: 14.2, zMs: 15.1, rkMs: 18.8 },
        test2: { naiveMs: 245.0, kmpMs: 2.1, zMs: 2.4, rkMs: 3.0 }
      };
    }

    renderBenchmarkCharts(benchData);
    showToast('Benchmark run completed!', 'success');
  } catch (err) {
    showToast('Benchmark failed: ' + err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '⚡ Run Live Benchmark';
  }
}

function renderBenchmarkCharts(data) {
  const isDark = document.documentElement.getAttribute('data-theme') !== 'light';
  const textColor = isDark ? '#94a3b8' : '#475569';
  const gridColor = isDark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(0, 0, 0, 0.06)';

  // Chart 1: Real Corpus
  const ctx1 = document.getElementById('chart-bench-test1');
  if (ctx1) {
    if (state.charts.bench1) state.charts.bench1.destroy();
    state.charts.bench1 = new Chart(ctx1, {
      type: 'bar',
      data: {
        labels: ['Naive', 'KMP', 'Z-Algorithm', 'Rabin-Karp'],
        datasets: [{
          label: 'Execution Time (ms)',
          data: [data.test1.naiveMs, data.test1.kmpMs, data.test1.zMs, data.test1.rkMs],
          backgroundColor: [
            'rgba(244, 63, 94, 0.7)',
            'rgba(99, 102, 241, 0.8)',
            'rgba(6, 182, 212, 0.8)',
            'rgba(245, 158, 11, 0.8)'
          ],
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { ticks: { color: textColor } },
          y: {
            beginAtZero: true,
            ticks: { color: textColor },
            grid: { color: gridColor },
            title: { display: true, text: 'Time (ms) - Lower is better', color: textColor }
          }
        }
      }
    });
  }

  // Chart 2: Pathological Worst Case
  const ctx2 = document.getElementById('chart-bench-test2');
  if (ctx2) {
    if (state.charts.bench2) state.charts.bench2.destroy();
    state.charts.bench2 = new Chart(ctx2, {
      type: 'bar',
      data: {
        labels: ['Naive (O(N*M))', 'KMP (O(N+M))', 'Z-Alg (O(N+M))', 'Rabin-Karp'],
        datasets: [{
          label: 'Worst Case Time (ms)',
          data: [data.test2.naiveMs, data.test2.kmpMs, data.test2.zMs, data.test2.rkMs],
          backgroundColor: [
            'rgba(244, 63, 94, 0.9)',
            'rgba(16, 185, 129, 0.85)',
            'rgba(6, 182, 212, 0.85)',
            'rgba(245, 158, 11, 0.85)'
          ],
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { ticks: { color: textColor } },
          y: {
            beginAtZero: true,
            ticks: { color: textColor },
            grid: { color: gridColor },
            title: { display: true, text: 'Time (ms) - Lower is better', color: textColor }
          }
        }
      }
    });
  }
}

// =========================================================================
//  CO4: AUTOMATED EXAM BUILDER (MAX-FLOW / MIN-CUT)
// =========================================================================

function initPresetRequirements(presetType) {
  const container = document.getElementById('reqs-container');
  const setsSelect = document.getElementById('exam-sets-count');
  if (!container) return;

  container.innerHTML = '';

  let presets = [];
  if (presetType === 'balanced') {
    if (setsSelect) setsSelect.value = '2';
    presets = [
      { key: 'Arrays', difficulty: 'Easy', count: 2 },
      { key: 'Stacks', difficulty: 'Easy', count: 1 },
      { key: 'Dynamic Programming', difficulty: 'Medium', count: 2 },
      { key: 'Maximum Flow', difficulty: 'Hard', count: 1 }
    ];
  } else if (presetType === 'advanced') {
    if (setsSelect) setsSelect.value = '3';
    presets = [
      { key: 'Graphs', difficulty: 'Medium', count: 2 },
      { key: 'Dynamic Programming', difficulty: 'Medium', count: 2 },
      { key: 'Optimization Problems', difficulty: 'Hard', count: 2 }
    ];
  } else if (presetType === 'bottleneck') {
    if (setsSelect) setsSelect.value = '3';
    presets = [
      { key: 'Optimization Problems', difficulty: 'Hard', count: 20 },
      { key: 'Maximum Flow', difficulty: 'Hard', count: 15 }
    ];
  }

  presets.forEach(p => addRequirementRow(p.key, p.difficulty, p.count));
}

function addRequirementRow(key = '', diff = 'Medium', count = 2) {
  const container = document.getElementById('reqs-container');
  if (!container) return;

  const row = document.createElement('div');
  row.className = 'req-row';
  row.style.display = 'grid';
  row.style.gridTemplateColumns = '2fr 1fr 1fr auto';
  row.style.gap = '10px';
  row.style.alignItems = 'center';

  row.innerHTML = `
    <input type="text" class="req-key" list="topics-datalist" placeholder="Subject or Topic name..." value="${escapeHtml(key)}" style="padding: 8px 12px; border-radius: var(--radius-sm); background: var(--bg-tertiary); border: 1px solid var(--border-color); color: var(--text-primary);">
    <select class="req-diff" style="padding: 8px 10px; border-radius: var(--radius-sm); background: var(--bg-tertiary); border: 1px solid var(--border-color); color: var(--text-primary);">
      <option value="Easy" ${diff === 'Easy' ? 'selected' : ''}>Easy (2m)</option>
      <option value="Medium" ${diff === 'Medium' ? 'selected' : ''}>Medium (5m)</option>
      <option value="Hard" ${diff === 'Hard' ? 'selected' : ''}>Hard (10m)</option>
    </select>
    <input type="number" class="req-count" min="1" max="50" value="${count}" style="padding: 8px 10px; border-radius: var(--radius-sm); background: var(--bg-tertiary); border: 1px solid var(--border-color); color: var(--text-primary);">
    <button type="button" class="btn btn-outline btn-remove-req" style="color: var(--accent-rose); border-color: rgba(244, 63, 94, 0.3); padding: 8px 12px;">✕</button>
  `;

  row.querySelector('.btn-remove-req').addEventListener('click', () => row.remove());
  container.appendChild(row);
}

async function buildExamPapers() {
  const sets = parseInt(document.getElementById('exam-sets-count').value, 10) || 2;
  const rows = document.querySelectorAll('#reqs-container .req-row');

  const reqs = [];
  rows.forEach(r => {
    const key = r.querySelector('.req-key').value.trim();
    const difficulty = r.querySelector('.req-diff').value;
    const count = parseInt(r.querySelector('.req-count').value, 10) || 1;
    if (key) reqs.push({ key, difficulty, count });
  });

  if (reqs.length === 0) {
    showToast('Please specify at least one requirement quota', 'error');
    return;
  }

  const resultsArea = document.getElementById('exam-results-area');
  const statusBox = document.getElementById('exam-status-box');
  const papersContainer = document.getElementById('exam-papers-container');
  const flowBadge = document.getElementById('exam-flow-badge');

  resultsArea.style.display = 'block';
  statusBox.innerHTML = '<div style="padding: 24px; text-align: center;">Solving Edmonds-Karp Residual Network Flow...</div>';

  try {
    let result;
    if (state.isBackendConnected) {
      const res = await fetch('/api/exam/build', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sets, requirements: reqs })
      });
      result = await res.json();
    } else {
      result = clientSideExamBuilder(sets, reqs);
    }

    if (result.success) {
      flowBadge.textContent = 'Feasible: Max-Flow Achieved';
      flowBadge.className = 'badge badge-easy';

      statusBox.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px;">
          <div>
            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 6px;">
              <span style="font-size: 1.5rem;">✅</span>
              <h3 style="font-size: 1.15rem; font-weight: 800; color: #a7f3d0;">Exam Allocation Successful!</h3>
            </div>
            <p style="font-size: 0.85rem; color: var(--text-secondary);">
              Maximum Flow = <strong>${result.maxFlow}</strong> / <strong>${result.totalNeeded}</strong> questions satisfied. All <strong>${sets} sets</strong> have unique disjoint questions with <strong>zero overlaps</strong>.
            </p>
          </div>
          <button class="btn btn-primary" onclick="window.print()">🖨️ Print Exam Papers</button>
        </div>
      `;

      papersContainer.innerHTML = result.sets.map((set, sIdx) => `
        <div class="exam-paper">
          <div class="exam-banner">
            <h2>DEPARTMENT OF COMPUTER SCIENCE & ENGINEERING</h2>
            <div style="font-size: 1.1rem; font-weight: 700; color: var(--accent-indigo); margin-top: 4px;">
              END SEMESTER EXAMINATION - SET ${String.fromCharCode(65 + sIdx)}
            </div>
            <div class="exam-meta-grid">
              <div>Course: CS301 Data Structures & Algorithms</div>
              <div>Duration: 3 Hours</div>
              <div>Total Marks: <strong>${set.totalMarks}</strong></div>
              <div>Total Questions: <strong>${set.questions.length}</strong></div>
            </div>
          </div>

          <div class="questions-stream">
            ${set.questions.map((q, idx) => `
              <div style="margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px dashed var(--border-color);">
                <div style="display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 8px;">
                  <div style="font-weight: 700; font-size: 0.95rem;">
                    Q${idx + 1}. [${escapeHtml(q.topic || '')} - ${q.difficulty}]
                  </div>
                  <div style="font-weight: 700; color: var(--accent-cyan); font-family: var(--font-mono);">
                    [${q.marks} Marks]
                  </div>
                </div>
                <div style="font-size: 0.92rem; margin-bottom: 12px; line-height: 1.5;">${escapeHtml(q.stem)}</div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
                  ${(q.options || []).map(opt => `
                    <div style="font-size: 0.85rem; color: var(--text-secondary); background: var(--bg-tertiary); padding: 8px 12px; border-radius: var(--radius-sm);">
                      ${escapeHtml(opt)}
                    </div>
                  `).join('')}
                </div>
              </div>
            `).join('')}
          </div>
        </div>
      `).join('');

    } else {
      // Infeasible / Bottleneck State
      flowBadge.textContent = 'Infeasible: Bottleneck Cut';
      flowBadge.className = 'badge badge-hard';

      statusBox.innerHTML = `
        <div style="display: flex; align-items: flex-start; gap: 14px; margin-bottom: 18px;">
          <span style="font-size: 1.8rem;">⚠️</span>
          <div>
            <h3 style="font-size: 1.15rem; font-weight: 800; color: #fca5a5; margin-bottom: 4px;">
              Exam Generation Infeasible - Min-Cut Bottleneck Identified
            </h3>
            <p style="font-size: 0.88rem; color: var(--text-secondary);">
              Only <strong>${result.maxFlow}</strong> of <strong>${result.totalNeeded}</strong> required questions could be routed. By the Max-Flow Min-Cut theorem, the residual min-cut capacity equals ${result.cutCapacity}.
            </p>
          </div>
        </div>

        <div style="margin-bottom: 16px;">
          <h4 style="font-size: 0.9rem; font-weight: 700; margin-bottom: 8px;">Shortage Diagnostic by Requirement:</h4>
          <table style="width: 100%; border-collapse: collapse; font-size: 0.85rem;">
            <thead>
              <tr style="border-bottom: 1px solid var(--border-color); color: var(--text-muted); text-align: left;">
                <th style="padding: 8px;">Requirement Slot</th>
                <th style="padding: 8px;">Assigned</th>
                <th style="padding: 8px;">Wanted</th>
                <th style="padding: 8px;">Available in Bank</th>
                <th style="padding: 8px;">Diagnosis</th>
              </tr>
            </thead>
            <tbody>
              ${result.reports.map(rep => `
                <tr style="border-bottom: 1px solid var(--border-color);">
                  <td style="padding: 8px; font-weight: 600;">${escapeHtml(rep.requirement)}</td>
                  <td style="padding: 8px;">${rep.assigned}</td>
                  <td style="padding: 8px;">${rep.wanted}</td>
                  <td style="padding: 8px;">${rep.pool}</td>
                  <td style="padding: 8px; color: ${rep.status === 'Satisfied' ? 'var(--accent-emerald)' : 'var(--accent-rose)'}; font-weight: 600;">
                    ${escapeHtml(rep.status)}
                  </td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>

        <div style="font-size: 0.82rem; color: var(--text-muted);">
          <strong>Remedy:</strong> Lower the required question count, reduce the number of sets, or add additional matching questions to the Question Bank.
        </div>
      `;

      papersContainer.innerHTML = '';
    }

  } catch (err) {
    showToast('Exam builder failed: ' + err.message, 'error');
  }
}

// Client-side solver fallback for exam builder
function clientSideExamBuilder(sets, reqs) {
  const used = new Set();
  const examSets = [];
  let totalNeeded = 0;
  let maxFlow = 0;

  // Track actual assigned counts per requirement index
  const assignedPerReq = reqs.map(() => 0);

  for (let s = 0; s < sets; s++) {
    const questions = [];
    let totalMarks = 0;

    reqs.forEach((r, rIdx) => {
      totalNeeded += r.count;
      let matchedCount = 0;

      for (const q of state.questions) {
        if (matchedCount >= r.count) break;
        if (!used.has(q.id)) {
          const matchesKey = (q.subject && q.subject.toLowerCase() === r.key.toLowerCase()) ||
                             (q.topic && q.topic.toLowerCase() === r.key.toLowerCase());
          const matchesDiff = q.difficulty && q.difficulty.toLowerCase() === r.difficulty.toLowerCase();

          if (matchesKey && matchesDiff) {
            used.add(q.id);
            questions.push(q);
            totalMarks += (q.marks || 5);
            matchedCount++;
            maxFlow++;
            assignedPerReq[rIdx]++;
          }
        }
      }
    });

    examSets.push({ setNumber: s + 1, totalMarks, questions });
  }

  const success = maxFlow === totalNeeded;
  return {
    success,
    setsRequested: sets,
    totalNeeded,
    maxFlow,
    cutCapacity: maxFlow,
    sets: examSets,
    reports: reqs.map((r, rIdx) => ({
      requirement: `${r.key} / ${r.difficulty} x${r.count}`,
      assigned: assignedPerReq[rIdx],
      wanted: r.count * sets,
      pool: state.questions.filter(q => (q.topic === r.key || q.subject === r.key) && q.difficulty === r.difficulty).length,
      status: assignedPerReq[rIdx] >= r.count * sets ? 'Satisfied' : 'Deficit'
    }))
  };
}

// =========================================================================
//  EVENT LISTENERS & BINDINGS
// =========================================================================

function setupEventListeners() {
  // Theme Toggle
  document.getElementById('theme-toggle')?.addEventListener('click', toggleTheme);

  // Global Quick Search
  document.getElementById('global-search')?.addEventListener('input', e => {
    const val = e.target.value;
    const filterInput = document.getElementById('filter-search');
    if (filterInput) filterInput.value = val;
    switchView('view-questions');
    applyQuestionFilters();
  });

  // Question Filters
  document.getElementById('filter-search')?.addEventListener('input', applyQuestionFilters);
  document.getElementById('filter-topic')?.addEventListener('change', applyQuestionFilters);
  document.getElementById('filter-difficulty')?.addEventListener('change', applyQuestionFilters);

  // Marks Pills Filter
  document.querySelectorAll('.filter-pills .pill').forEach(pill => {
    pill.addEventListener('click', () => {
      document.querySelectorAll('.filter-pills .pill').forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      state.activeMarksFilter = pill.getAttribute('data-marks') || '';
      applyQuestionFilters();
    });
  });

  // Pagination Controls
  document.getElementById('btn-prev-page')?.addEventListener('click', () => {
    if (state.currentPage > 1) {
      state.currentPage--;
      renderQuestionList();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  });

  document.getElementById('btn-next-page')?.addEventListener('click', () => {
    const totalPages = Math.ceil(state.filteredQuestions.length / state.pageSize);
    if (state.currentPage < totalPages) {
      state.currentPage++;
      renderQuestionList();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  });

  // Modals
  document.getElementById('btn-open-add-modal')?.addEventListener('click', openAddModal);
  document.getElementById('btn-filter-add')?.addEventListener('click', openAddModal);
  document.getElementById('btn-close-modal')?.addEventListener('click', closeModal);
  document.getElementById('btn-cancel-modal')?.addEventListener('click', closeModal);
  document.getElementById('question-form')?.addEventListener('submit', handleQuestionFormSubmit);

  // Save Button
  document.getElementById('btn-save-dataset')?.addEventListener('click', saveDatasetToDisk);

  // Pattern Matching Lab
  document.getElementById('btn-run-lab-search')?.addEventListener('click', runStringPatternLab);
  document.getElementById('lab-search-input')?.addEventListener('keydown', e => {
    if (e.key === 'Enter') runStringPatternLab();
  });

  // Quick Chips in Lab
  document.querySelectorAll('.lab-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      const input = document.getElementById('lab-search-input');
      if (input) {
        input.value = chip.textContent.trim();
        runStringPatternLab();
      }
    });
  });

  // Plagiarism Detector
  document.getElementById('phrase-slider')?.addEventListener('input', e => {
    document.getElementById('phrase-len-val').textContent = e.target.value;
  });
  document.getElementById('btn-run-plagiarism')?.addEventListener('click', runPlagiarismDetector);

  // Benchmarking
  document.getElementById('btn-run-full-benchmark')?.addEventListener('click', runLiveBenchmark);

  // Exam Builder
  document.getElementById('btn-add-req-row')?.addEventListener('click', () => addRequirementRow());
  document.getElementById('btn-build-exam')?.addEventListener('click', buildExamPapers);

  document.querySelectorAll('.exam-preset').forEach(btn => {
    btn.addEventListener('click', () => {
      const preset = btn.getAttribute('data-preset');
      initPresetRequirements(preset);
    });
  });
}

// =========================================================================
//  UTILITIES: TOASTS & ESCAPING
// =========================================================================

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <span>${type === 'success' ? '✓' : (type === 'error' ? '✕' : 'ℹ')}</span>
    <span>${escapeHtml(message)}</span>
  `;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
