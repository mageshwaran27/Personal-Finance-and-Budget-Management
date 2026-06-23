let questions = [];
let answers = {};

fetch('/api/questions')
  .then(res => res.json())
  .then(data => {
    questions = data;
    renderQuiz();
  });

function renderQuiz() {
  const box = document.getElementById("quiz-box");
  questions.forEach((q, index) => {
    const div = document.createElement("div");
    div.innerHTML = `<h3>${q.question}</h3>
      ${['A','B','C','D'].map((opt, i) => `
        <label>
          <input type="radio" name="q${index}" value="${opt}" />
          ${q[`option${i+1}`]}
        </label><br>`).join('')}
    `;
    box.appendChild(div);
  });
}

document.getElementById("submit-btn").onclick = () => {
  questions.forEach((_, i) => {
    const selected = document.querySelector(`input[name="q${i}"]:checked`);
    if (selected) answers[i] = selected.value;
  });

  fetch('/api/submit', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(answers)
  })
  .then(res => res.json())
  .then(data => showResults(data));
};

function showResults(data) {
  document.getElementById("quiz-box").classList.add("hidden");
  document.getElementById("submit-btn").classList.add("hidden");
  document.getElementById("result-box").classList.remove("hidden");
  document.getElementById("score").textContent = `Correct: ${data.correct}, Wrong: ${data.wrong}, Percentage: ${data.percentage.toFixed(2)}%`;
  document.getElementById("performance").textContent = `Performance: ${data.performance}`;
}
