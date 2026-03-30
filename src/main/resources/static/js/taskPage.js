const token = localStorage.getItem('token');
const urlParts = window.location.pathname.split('/');
const taskId = urlParts[urlParts.length - 1];
const taskCategory = document.querySelector('.task-category').textContent;

const taskParametersTable = document.getElementById('task-parameters-table');
const taskParameters = Array.from(taskParametersTable.querySelectorAll('tbody tr')).map(row => ({
    parent: row.children[0].innerText.trim() || '默认一级指标',
    name: row.children[1].innerText.trim(),
    weight: parseFloat(row.children[2].innerText.trim())
}));

const firstLevelCriteria = [...new Set(taskParameters.map(p => p.parent))];
const secondLevelByParent = taskParameters.reduce((acc, p) => {
    if (!acc[p.parent]) acc[p.parent] = [];
    acc[p.parent].push(p.name);
    return acc;
}, {});

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
        const isAnalyst = userRole === 'ANALYST';
        const roleMatchesCategory =
            (taskCategory === '能源' && userRole === 'POWER_ENGINEER') ||
            (taskCategory === '经济' && userRole === 'ECONOMIST') ||
            (taskCategory === '生态' && userRole === 'ECOLOGIST');

        if (roleMatchesCategory) {
            const createButton = document.createElement('a');
            createButton.href = `/decisions/create?taskId=${taskId}`;
            createButton.className = 'inline-block bg-green-500 hover:bg-green-600 text-white font-bold py-2 px-4 rounded mb-4';
            createButton.textContent = '提交候选前沿技术';
            document.getElementById('create-decision-container').appendChild(createButton);
        }

        if (isAnalyst) {
            const ahpButton = document.createElement('button');
            ahpButton.className = 'inline-block bg-indigo-500 hover:bg-indigo-600 text-white font-bold py-2 px-4 rounded mb-4 ml-4';
            ahpButton.textContent = '执行AHP分析与评价';
            ahpButton.onclick = openAHPModal;
            document.getElementById('create-decision-container').appendChild(ahpButton);
        }

        if (userRole === 'ECOLOGIST' || userRole === 'ECONOMIST' || userRole === 'POWER_ENGINEER' || userRole === 'LAWEYR') {
            document.querySelectorAll('[data-decision-id]').forEach(decisionCard => {
                const decisionId = decisionCard.getAttribute('data-decision-id');
                const rateButton = document.createElement('button');
                rateButton.className = 'inline-block bg-blue-500 hover:bg-blue-600 text-white font-semibold py-1 px-3 rounded ml-2 mt-2';
                rateButton.textContent = '评分';
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
                editButton.className = 'inline-block bg-yellow-500 hover:bg-yellow-600 text-white font-semibold py-1 px-3 rounded ml-2 mt-2';
                editButton.textContent = '编辑候选技术';
                decisionCard.appendChild(editButton);
            }
        });
    })
    .catch(error => {
        alert('您需要先登录。');
        window.location.href = '/login';
        console.error('错误 获取 用户:', error);
    });

function openAHPModal() {
    const modal = document.getElementById('ahp-modal');
    const tableContainer = document.getElementById('ahp-table-container');
    tableContainer.innerHTML = '';

    tableContainer.appendChild(buildMatrixCard('一级指标判断矩阵', 'primary', firstLevelCriteria));
    firstLevelCriteria.forEach(parent => {
        const children = secondLevelByParent[parent] || [];
        if (children.length > 1) {
            tableContainer.appendChild(buildMatrixCard(`二级指标判断矩阵 - ${parent}`, `secondary_${parent}`, children));
        }
    });

    modal.classList.remove('hidden');
}

function buildMatrixCard(title, key, labels) {
    const wrapper = document.createElement('div');
    wrapper.className = 'mb-6 border rounded p-4';

    let html = `<h3 class="font-semibold mb-3">${title}</h3><table class="table-auto border-collapse w-full">`;
    html += '<thead><tr><th class="border p-2 bg-gray-100"></th>';
    labels.forEach(label => html += `<th class="border p-2 bg-gray-100">${label}</th>`);
    html += '</tr></thead><tbody>';

    labels.forEach((rowLabel, i) => {
        html += `<tr><th class="border p-2 bg-gray-100">${rowLabel}</th>`;
        labels.forEach((_, j) => {
            if (i === j) {
                html += '<td class="border p-2 text-center"><input type="number" value="1" readonly class="w-20 text-center bg-gray-200 border-gray-300 rounded"></td>';
            } else {
                html += `<td class="border p-2 text-center"><input type="number" step="0.01" min="0.11" value="1" class="w-20 text-center border-gray-300 rounded" data-matrix="${key}" data-row="${i}" data-col="${j}"></td>`;
            }
        });
        html += '</tr>';
    });

    html += '</tbody></table>';
    wrapper.innerHTML = html;
    return wrapper;
}

document.getElementById('cancel-ahp').addEventListener('click', () => {
    document.getElementById('ahp-modal').classList.add('hidden');
});

document.getElementById('submit-ahp').addEventListener('click', () => {
    const primaryMatrix = collectMatrix('primary', firstLevelCriteria.length);
    const secondaryMatrices = {};

    firstLevelCriteria.forEach(parent => {
        const children = secondLevelByParent[parent] || [];
        if (children.length > 1) {
            secondaryMatrices[parent] = collectMatrix(`secondary_${parent}`, children.length);
        }
    });

    fetch(`http://localhost:8080/api/v1/tasks/${taskId}/ahp-analysis`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ primaryMatrix, secondaryMatrices })
    })
        .then(response => response.json())
        .then(renderAnalysisResult)
        .catch(error => {
            console.error('错误： 计算 AHP:', error);
            alert('无法执行分析与评价。');
        })
        .finally(() => {
            document.getElementById('ahp-modal').classList.add('hidden');
        });
});

function collectMatrix(key, size) {
    const matrix = [];
    for (let i = 0; i < size; i++) {
        matrix[i] = [];
        for (let j = 0; j < size; j++) {
            const input = document.querySelector(`input[data-matrix="${key}"][data-row="${i}"][data-col="${j}"]`);
            matrix[i][j] = (i === j) ? 1 : (input ? parseFloat(input.value) || 1 : 1);
        }
    }
    return matrix;
}

function renderAnalysisResult(result) {
    const container = document.getElementById('analysis-result-container');
    container.classList.remove('hidden');

    const consistencyRows = Object.entries(result.consistencyByLevel || {})
        .map(([level, val]) => `<tr><td class="p-2 border">${level}</td><td class="p-2 border">${val.ci.toFixed(4)}</td><td class="p-2 border">${val.cr.toFixed(4)}</td><td class="p-2 border ${val.consistent ? 'text-green-600' : 'text-red-600'}">${val.consistent ? '通过' : '未通过'}</td></tr>`)
        .join('');

    const weightRows = Object.entries(result.criteriaWeights || {})
        .map(([name, w]) => `<tr><td class="p-2 border">${name}</td><td class="p-2 border">${w.toFixed(4)}</td></tr>`)
        .join('');

    const rankingRows = (result.ranking || [])
        .map((item, i) => `<tr class="${i === 0 ? 'bg-green-100' : ''}"><td class="p-2 border">${item.rank}</td><td class="p-2 border">${item.decisionTitle}</td><td class="p-2 border">${item.ahpScore.toFixed(4)}</td><td class="p-2 border">${item.expertScore.toFixed(4)}</td><td class="p-2 border">${item.totalScore.toFixed(4)}</td></tr>`)
        .join('');

    const questionnaireRows = Object.entries(result.questionnaireDimensionScores || {})
        .map(([d, score]) => `<li>${d}: ${score.toFixed(2)}</li>`)
        .join('');

    container.innerHTML = `
        <h3 class="text-xl font-bold mb-4">分析结果</h3>
        <div class="grid md:grid-cols-2 gap-6">
            <div>
                <h4 class="font-semibold mb-2">一致性检验 (CI/CR)</h4>
                <table class="w-full border text-sm"><thead class="bg-gray-100"><tr><th class="p-2 border">层级</th><th class="p-2 border">CI</th><th class="p-2 border">CR</th><th class="p-2 border">结论</th></tr></thead><tbody>${consistencyRows}</tbody></table>
            </div>
            <div>
                <h4 class="font-semibold mb-2">二级指标综合权重</h4>
                <table class="w-full border text-sm"><thead class="bg-gray-100"><tr><th class="p-2 border">指标</th><th class="p-2 border">权重</th></tr></thead><tbody>${weightRows}</tbody></table>
            </div>
        </div>
        <div class="mt-4">
            <h4 class="font-semibold mb-2">候选技术综合得分与排名</h4>
            <table class="w-full border text-sm"><thead class="bg-gray-100"><tr><th class="p-2 border">排名</th><th class="p-2 border">候选技术</th><th class="p-2 border">AHP得分</th><th class="p-2 border">专家得分(归一)</th><th class="p-2 border">综合得分</th></tr></thead><tbody>${rankingRows}</tbody></table>
        </div>
        <div class="mt-4 text-sm text-gray-700">
            <strong>问卷维度汇总（最近一次提交）:</strong>
            <ul class="list-disc pl-6">${questionnaireRows || '<li>暂无问卷提交数据</li>'}</ul>
        </div>
    `;
}
