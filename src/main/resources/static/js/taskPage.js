const token = localStorage.getItem('token');
const urlParts = window.location.pathname.split('/');
const taskId = urlParts[urlParts.length - 1];
const taskCategory = document.querySelector('.task-category').textContent;

// Дані про 参数 任务
const taskParametersTable = document.getElementById('task-parameters-table');
const taskParameters = Array.from(taskParametersTable.querySelectorAll('tbody tr')).map(row => ({
    name: row.children[0].innerText.trim(),
    weight: parseFloat(row.children[1].innerText.trim()),
    unit: row.children[2].innerText.trim()
}));

fetch('http://localhost:8080/api/v1/auth/user', {
    method: 'GET',
    headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    }
})
    .then(response => response.json())
    .then(user => {
        const userId = user.id;
        const userRole = user.roles[0].name;
        console.log(userRole);
        console.log(taskCategory);
        const isAnalyst = userRole === 'ANALYST';
        const roleMatchesCategory =
            (taskCategory === 'Енергетика' && userRole === 'POWER_ENGINEER') ||
            (taskCategory === 'Економіка' && userRole === 'ECONOMIST') ||
            (taskCategory === 'Екологія' && userRole === 'ECOLOGIST');

        if (roleMatchesCategory) {
            const createButton = document.createElement('a');
            createButton.href = `/decisions/create?taskId=${taskId}`;
            createButton.className = "inline-block bg-green-500 hover:bg-green-600 text-white font-bold py-2 px-4 rounded mb-4";
            createButton.textContent = "提交方案";

            document.getElementById('create-decision-container').appendChild(createButton);
        }

        if (isAnalyst) {
            const ahpButton = document.createElement('button');
            ahpButton.className = "inline-block bg-indigo-500 hover:bg-indigo-600 text-white font-bold py-2 px-4 rounded mb-4 ml-4";
            ahpButton.textContent = "选择最佳方案 (AHP)";
            ahpButton.onclick = openAHPModal;

            document.getElementById('create-decision-container').appendChild(ahpButton);
        }

        if (isAnalyst) {
            const topsisButton = document.createElement('button');
            topsisButton.className = "inline-block bg-purple-500 hover:bg-purple-600 text-white font-bold py-2 px-4 rounded mb-4 ml-4";
            topsisButton.textContent = "选择最佳方案 (TOPSIS)";
            topsisButton.onclick = calculateTopsis;

            document.getElementById('create-decision-container').appendChild(topsisButton);
        }
        if (isAnalyst) {
            const electreButton = document.createElement('button');
            electreButton.className = "inline-block bg-emerald-500 hover:bg-emerald-600 text-white font-bold py-2 px-4 rounded mb-4 ml-4";
            electreButton.textContent = "选择最佳方案 (ELECTRE)";
            electreButton.onclick = () => {
                document.getElementById('electre-modal').classList.remove('hidden');
            };
            document.getElementById('create-decision-container').appendChild(electreButton);
        }
        if (isAnalyst) {
            const recommendButton = document.createElement('button');
            recommendButton.className = "inline-block bg-cyan-600 hover:bg-cyan-700 text-white font-bold py-2 px-4 rounded mb-4 ml-4";
            recommendButton.textContent = "查看推荐方法";
            recommendButton.onclick = recommendMethod;

            document.getElementById('create-decision-container').appendChild(recommendButton);
        }
        // Створення кнопки "评分" і передача ID
        if (userRole === 'ECOLOGIST' || userRole === 'ECONOMIST' || userRole === 'POWER_ENGINEER' || userRole === 'LAWEYR') {
            document.querySelectorAll('[data-decision-id]').forEach(decisionCard => {
                const decisionId = decisionCard.getAttribute('data-decision-id');

                const rateButton = document.createElement('button');
                rateButton.className = "inline-block bg-blue-500 hover:bg-blue-600 text-white font-semibold py-1 px-3 rounded ml-2 mt-2";
                rateButton.textContent = "评分";

                // ЦЕ ГОЛОВНЕ: передаємо ID у функцію
                rateButton.onclick = () => openRateModal(decisionId);

                decisionCard.appendChild(rateButton);
            });
        }


        document.querySelectorAll('[data-decision-id]').forEach(decisionCard => {
            const decisionUserId = parseInt(decisionCard.getAttribute('data-decision-user-id'));
            if (decisionUserId === userId) {
                const decisionId = decisionCard.getAttribute('data-decision-id');

                const editButton = document.createElement('a');
                editButton.href = `/decisions/${decisionId}/edit`;
                editButton.className = "inline-block bg-yellow-500 hover:bg-yellow-600 text-white font-semibold py-1 px-3 rounded ml-2 mt-2";
                editButton.textContent = "编辑 方案";

                decisionCard.appendChild(editButton);
            }
        });
    })
    .catch(error => {
        alert("您需要先登录。")
        window.location.href = "/login";
        console.error('错误 获取 用户:', error);
    });

// Відкрити модалку для введення матриці AHP
function openAHPModal() {
    const modal = document.getElementById('ahp-modal');
    const tableContainer = document.getElementById('ahp-table-container');

    // Генеруємо HTML таблицю для вводу парних порівнянь
    let html = '<table class="table-auto border-collapse w-full">';
    html += '<thead><tr><th class="border p-2 bg-gray-100"></th>';

    taskParameters.forEach(param => {
        html += `<th class="border p-2 bg-gray-100">${param.name}</th>`;
    });
    html += '</tr></thead><tbody>';

    taskParameters.forEach((rowParam, i) => {
        html += `<tr><th class="border p-2 bg-gray-100">${rowParam.name}</th>`;
        taskParameters.forEach((colParam, j) => {
            if (i === j) {
                html += `<td class="border p-2 text-center">
                            <input type="number" value="1" readonly class="w-20 text-center bg-gray-200 border-gray-300 rounded">
                         </td>`;
            } else {
                html += `<td class="border p-2 text-center">
                            <input type="number" step="0.01" min="0" class="w-20 text-center border-gray-300 rounded" data-row="${i}" data-col="${j}">
                         </td>`;
            }
        });
        html += '</tr>';
    });

    html += '</tbody></table>';
    tableContainer.innerHTML = html;

    modal.classList.remove('hidden');
}

// 关闭 модалку без обчислення
document.getElementById('cancel-ahp').addEventListener('click', () => {
    document.getElementById('ahp-modal').classList.add('hidden');
});

// Обробка кнопки "计算" в модалці
document.getElementById('submit-ahp').addEventListener('click', () => {
    const size = taskParameters.length;
    const matrix = [];

    for (let i = 0; i < size; i++) {
        matrix[i] = [];
        for (let j = 0; j < size; j++) {
            const input = document.querySelector(`input[data-row="${i}"][data-col="${j}"]`);
            matrix[i][j] = (i === j) ? 1 : (input ? parseFloat(input.value) || 1 : 1);
        }
    }

    console.log('两两比较矩阵：', matrix);

    fetch(`http://localhost:8080/api/v1/tasks/${taskId}/ahp`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(matrix)
    })
        .then(response => response.json())
        .then(results => {
            const resultModal = document.createElement('div');
            resultModal.className = "fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center";
            resultModal.innerHTML = `
            <div class="bg-white p-8 rounded-lg shadow-lg w-full max-w-2xl">
                <h2 class="text-2xl font-bold mb-6 text-center">选择结果 (AHP)</h2>
                <table class="w-full border text-sm mb-6">
                    <thead class="bg-gray-200">
                        <tr><th class="p-2 border">方案</th><th class="p-2 border">评分</th></tr>
                    </thead>
                    <tbody>
                        ${
                Object.entries(results)
                    .sort((a, b) => b[1] - a[1]) // сортуємо за спаданням оцінок
                    .map(([description, score], index) => `
                                    <tr class="${index === 0 ? 'bg-green-100' : ''}">
                                        <td class="p-2 border">${description}</td>
                                        <td class="p-2 border">${score.toFixed(2)}</td>
                                    </tr>
                                `).join('')
            }
                    </tbody>
                </table>
                <div class="flex justify-center">
                    <button class="bg-red-500 hover:bg-red-600 text-white font-bold py-2 px-6 rounded" id="close-result-modal">
                        关闭
                    </button>
                </div>
            </div>
        `;
            document.body.appendChild(resultModal);

            document.getElementById('close-result-modal').addEventListener('click', () => {
                resultModal.remove();
            });

            document.getElementById('ahp-modal').classList.add('hidden');
        })
        .catch(error => {
            console.error('错误 при обчисленні AHP:', error);
            alert('无法确定选择结果。');
            document.getElementById('ahp-modal').classList.add('hidden');
        });
});

function calculateTopsis() {
    fetch(`http://localhost:8080/api/v1/tasks/${taskId}/topsis`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => response.json())
        .then(result => {
            const sorted = Object.entries(result)
                .sort(([, a], [, b]) => b - a);

            const modal = document.createElement('div');
            modal.className = "fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50";

            modal.innerHTML = `
            <div class="bg-white p-8 rounded-lg shadow-lg max-w-2xl w-full">
                <h2 class="text-2xl font-bold mb-6 text-center">选择结果 (TOPSIS)</h2>
                <table class="w-full border text-sm mb-6">
                    <thead class="bg-gray-200">
                        <tr><th class="p-2 border">方案</th><th class="p-2 border">评分</th></tr>
                    </thead>
                    <tbody>
                        ${sorted.map(([desc, score], i) => `
                            <tr class="${i === 0 ? 'bg-green-100' : ''}">
                                <td class="p-2 border">${desc}</td>
                                <td class="p-2 border">${score.toFixed(4)}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
                <div class="flex justify-center">
                    <button class="bg-red-500 hover:bg-red-600 text-white font-bold py-2 px-6 rounded" onclick="this.closest('.fixed').remove()">关闭</button>
                </div>
            </div>
        `;

            document.body.appendChild(modal);
        })
        .catch(error => {
            console.error('TOPSIS 错误：', error);
            alert('无法确定最佳方案 (TOPSIS)。');
        });
}

function calculateElectre() {
    fetch(`http://localhost:8080/api/v1/tasks/${taskId}/electre`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => response.json())
        .then(result => {
            const sorted = Object.entries(result)
                .sort(([, a], [, b]) => b - a);

            const modal = document.createElement('div');
            modal.className = "fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50";

            modal.innerHTML = `
                <div class="bg-white p-8 rounded-lg shadow-lg max-w-2xl w-full">
                    <h2 class="text-2xl font-bold mb-6 text-center">选择结果 (ELECTRE)</h2>
                    <table class="w-full border text-sm mb-6">
                        <thead class="bg-gray-200">
                            <tr><th class="p-2 border">方案</th><th class="p-2 border">评分</th></tr>
                        </thead>
                        <tbody>
                            ${sorted.map(([desc, score], i) => `
                                <tr class="${i === 0 ? 'bg-green-100' : ''}">
                                    <td class="p-2 border">${desc}</td>
                                    <td class="p-2 border">${score.toFixed(4)}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                    <div class="flex justify-center">
                        <button class="bg-red-500 hover:bg-red-600 text-white font-bold py-2 px-6 rounded" onclick="this.closest('.fixed').remove()">关闭</button>
                    </div>
                </div>
            `;

            document.body.appendChild(modal);
        })
        .catch(error => {
            console.error('ELECTRE 错误：', error);
            alert('无法确定最佳方案 (ELECTRE)。');
        });
}

function openElectreModal() {
    const modal = document.createElement('div');
    modal.className = "fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50";
    modal.innerHTML = `
        <div class="bg-white p-6 rounded-lg shadow-lg max-w-md w-full">
            <h2 class="text-xl font-bold mb-4 text-center">请输入 ELECTRE 阈值</h2>
            <label class="block mb-2">一致性阈值 (C)：</label>
            <input id="c-threshold" type="number" step="0.01" min="0" max="1" value="0.5"
                   class="w-full p-2 border rounded mb-4" />
            <label class="block mb-2">不一致性阈值 (D)：</label>
            <input id="d-threshold" type="number" step="0.01" min="0" max="1" value="0.5"
                   class="w-full p-2 border rounded mb-6" />
            <div class="flex justify-end space-x-2">
                <button class="bg-gray-300 px-4 py-2 rounded" onclick="this.closest('.fixed').remove()">取消</button>
                <button class="bg-green-500 text-white px-4 py-2 rounded" onclick="submitElectre()">计算</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
}
function openElectreModal() {
    document.getElementById('electre-modal').classList.remove('hidden');
}

function closeElectreModal() {
    document.getElementById('electre-modal').classList.add('hidden');
}

function submitElectre() {
    const cThreshold = parseFloat(document.getElementById('c-threshold').value) || 0.5;
    const dThreshold = parseFloat(document.getElementById('d-threshold').value) || 0.5;

    const payload = {
        cThreshold: cThreshold,
        dThreshold: dThreshold
    };

    fetch(`http://localhost:8080/api/v1/tasks/${taskId}/electre`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
        .then(res => res.json())
        .then(result => {
            const sorted = Object.entries(result).sort(([, a], [, b]) => b - a);

            const modal = document.createElement('div');
            modal.className = "fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50";
            modal.innerHTML = `
                <div class="bg-white p-6 rounded shadow-lg max-w-2xl w-full">
                    <h2 class="text-xl font-bold mb-4 text-center">选择结果 (ELECTRE)</h2>
                    <table class="w-full border text-sm mb-4">
                        <thead class="bg-gray-200">
                            <tr><th class="p-2 border">方案</th><th class="p-2 border">评分</th></tr>
                        </thead>
                        <tbody>
                            ${sorted.map(([desc, score]) => `
                                <tr class="${score === 1 ? 'bg-green-100' : ''}">
                                    <td class="p-2 border">${desc}</td>
                                    <td class="p-2 border">${score.toFixed(4)}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                    <div class="flex justify-center">
                        <button onclick="this.closest('.fixed').remove()" class="bg-red-500 hover:bg-red-600 text-white py-2 px-4 rounded">关闭</button>
                    </div>
                </div>`;
            document.body.appendChild(modal);
            closeElectreModal();
        })
        .catch(error => {
            console.error('ELECTRE 错误：', error);
            alert('无法计算 ELECTRE。');
            closeElectreModal();
        });
}
// Сортуємо 方案 по категорії
decisions.sort((a, b) => {
    const order = ['APPROVED', 'PROPOSED', 'REJECTED'];
    return order.indexOf(a.decisionStatus) - order.indexOf(b.decisionStatus);
});

// Рендеримо 方案
const container = document.getElementById('decisions-container');
container.innerHTML = '';

decisions.forEach(decision => {
    const html = `
        <a href="/decisions/${decision.id}" class="bg-white p-4 rounded-lg shadow block" data-decision-id="${decision.id}" data-decision-user-id="${decision.user.id}">
            <h3 class="text-xl font-bold mb-2">${decision.title}</h3>
            <p class="text-gray-600 mb-2">
                <strong>描述：</strong> ${decision.description} |
                <strong>类型：</strong> ${decision.decisionCategory} |
                <strong>状态：</strong> ${decision.decisionStatus}
            </p>
            <p class="text-gray-700 mb-4">
                <strong>作者：</strong> ${decision.user.name} (${decision.user.roles[0].name})
            </p>
            <p class="text-gray-600 mb-2">
                <strong>评分：</strong> ${decision.rate.toFixed(2)}/10<br>
                <strong>评分数量：</strong> ${decision.expertEvaluations.length}
            </p>
        </a>
    `;
    container.insertAdjacentHTML('beforeend', html);
});
function recommendMethod() {
    fetch(`http://localhost:8080/api/v1/tasks/${taskId}/recommend-method`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })
        .then(res => res.text())
        .then(method => {
            const modal = document.createElement('div');
            modal.className = "fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50";
            modal.innerHTML = `
                <div class="bg-white p-8 rounded-lg shadow-lg w-full max-w-md text-center">
                    <h2 class="text-2xl font-bold mb-4">推荐方法</h2>
                    <p class="text-lg mb-6">基于任务特征，推荐使用以下方法：</p>
                    <p class="text-3xl font-extrabold text-blue-600">${method}</p>
                    <div class="mt-6">
                        <button class="bg-red-500 hover:bg-red-600 text-white font-bold py-2 px-6 rounded" onclick="this.closest('.fixed').remove()">关闭</button>
                    </div>
                </div>
            `;
            document.body.appendChild(modal);
        })
        .catch(error => {
            console.error('错误 рекомендації методу:', error);
            alert('无法获取推荐。');
        });
}
