const API = '/api/students';

const rollBody = document.getElementById('roll-body');
const emptyState = document.getElementById('empty-state');
const searchInput = document.getElementById('search');
const form = document.getElementById('student-form');
const idField = document.getElementById('student-id');
const nameField = document.getElementById('name');
const emailField = document.getElementById('email');
const courseField = document.getElementById('course');
const marksField = document.getElementById('marks');
const formHeading = document.getElementById('form-heading');
const submitBtn = document.getElementById('submit-btn');
const cancelEditBtn = document.getElementById('cancel-edit');
const formMessage = document.getElementById('form-message');
const statCount = document.getElementById('stat-count');
const statAvg = document.getElementById('stat-avg');

let students = [];

async function loadStudents() {
  try {
    const res = await fetch(API);
    if (!res.ok) throw new Error('Failed to load students');
    students = await res.json();
    render();
  } catch (err) {
    formMessage.textContent = 'Could not reach the server. Is StudentManagementSystem running?';
    formMessage.className = 'form-message error';
  }
}

function render() {
  const query = searchInput.value.trim().toLowerCase();
  const filtered = students.filter(s =>
    s.name.toLowerCase().includes(query) ||
    s.email.toLowerCase().includes(query) ||
    s.course.toLowerCase().includes(query)
  );

  rollBody.innerHTML = '';
  emptyState.hidden = filtered.length !== 0;

  filtered.forEach(s => {
    const tr = document.createElement('tr');

    const marksClass = s.marks < 40 ? 'marks-pill low' : 'marks-pill';

    tr.innerHTML = `
      <td>${s.id}</td>
      <td class="student-name">${escapeHtml(s.name)}</td>
      <td class="student-email">${escapeHtml(s.email)}</td>
      <td class="student-course">${escapeHtml(s.course)}</td>
      <td><span class="${marksClass}">${s.marks.toFixed(1)}</span></td>
      <td class="row-actions">
        <button data-action="edit" data-id="${s.id}">Edit</button>
        <button data-action="delete" data-id="${s.id}" class="danger">Remove</button>
      </td>
    `;
    rollBody.appendChild(tr);
  });

  statCount.textContent = students.length;
  statAvg.textContent = students.length
    ? (students.reduce((sum, s) => sum + s.marks, 0) / students.length).toFixed(1)
    : '—';
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

rollBody.addEventListener('click', (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = Number(btn.dataset.id);
  if (btn.dataset.action === 'edit') startEdit(id);
  if (btn.dataset.action === 'delete') deleteStudent(id);
});

function startEdit(id) {
  const s = students.find(x => x.id === id);
  if (!s) return;
  idField.value = s.id;
  nameField.value = s.name;
  emailField.value = s.email;
  courseField.value = s.course;
  marksField.value = s.marks;

  formHeading.textContent = `Editing ${s.name}`;
  submitBtn.textContent = 'Update student';
  cancelEditBtn.hidden = false;
  formMessage.textContent = '';
  document.getElementById('add-panel').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function resetForm() {
  form.reset();
  idField.value = '';
  formHeading.textContent = 'Add a student';
  submitBtn.textContent = 'Save student';
  cancelEditBtn.hidden = true;
}

cancelEditBtn.addEventListener('click', resetForm);

form.addEventListener('submit', async (e) => {
  e.preventDefault();

  const payload = {
    name: nameField.value.trim(),
    email: emailField.value.trim(),
    course: courseField.value.trim(),
    marks: marksField.value
  };

  const id = idField.value;
  const isEdit = Boolean(id);
  const url = isEdit ? `${API}/${id}` : API;
  const method = isEdit ? 'PUT' : 'POST';

  try {
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();

    if (!res.ok) {
      formMessage.textContent = data.error || 'Something went wrong.';
      formMessage.className = 'form-message error';
      return;
    }

    formMessage.textContent = isEdit ? 'Student updated.' : 'Student added.';
    formMessage.className = 'form-message success';
    resetForm();
    await loadStudents();
  } catch (err) {
    formMessage.textContent = 'Could not reach the server.';
    formMessage.className = 'form-message error';
  }
});

async function deleteStudent(id) {
  const s = students.find(x => x.id === id);
  if (!s) return;
  if (!confirm(`Remove ${s.name} from the roll?`)) return;

  try {
    const res = await fetch(`${API}/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error('Delete failed');
    await loadStudents();
  } catch (err) {
    formMessage.textContent = 'Could not delete that student.';
    formMessage.className = 'form-message error';
  }
}

searchInput.addEventListener('input', render);

document.querySelectorAll('.rail-nav-item[data-scroll]').forEach(item => {
  item.addEventListener('click', () => {
    document.getElementById(item.dataset.scroll).scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
});

loadStudents();
