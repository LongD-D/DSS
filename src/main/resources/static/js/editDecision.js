const token = localStorage.getItem('token');
const decisionId = document.getElementById('decisionId').value;

// 弹窗
const modal = document.getElementById('confirm-modal');
const modalTitle = document.getElementById('modal-title');
const modalMessage = document.getElementById('modal-message');
const modalConfirm = document.getElementById('modal-confirm');
const modalCancel = document.getElementById('modal-cancel');

// 显示弹窗
function showModal(title, message, confirmAction) {
    modalTitle.innerText = title;
    modalMessage.innerText = message;
    modal.classList.remove('hidden');

    modalConfirm.onclick = () => {
        confirmAction();
        modal.classList.add('hidden');
    };

    modalCancel.onclick = () => {
        modal.classList.add('hidden');
    };
}

// 添加新场景
function addScenario() {
    const container = document.getElementById('scenarios-container');
    const scenarioBlock = document.createElement('div');
    scenarioBlock.className = 'bg-gray-50 p-4 rounded-lg shadow-sm flex flex-wrap items-center gap-3 scenario-block mt-2';

    scenarioBlock.innerHTML = `
        <input type="text" class="scenario-title w-1/3 border rounded px-2 py-1" placeholder="情景名称">
        <input type="text" class="scenario-description w-1/3 border rounded px-2 py-1" placeholder="描述">
        <input type="number" step="0.01" min="0" max="1" class="scenario-possibility w-28 border rounded px-2 py-1" placeholder="概率">
        <button type="button" class="remove-scenario bg-red-500 hover:bg-red-600 text-white py-1 px-3 rounded">✕</button>
    `;

    container.appendChild(scenarioBlock);
    attachRemoveScenarioListeners();
}

// 绑定删除场景处理器
function attachRemoveScenarioListeners() {
    document.querySelectorAll('.remove-scenario').forEach(button => {
        button.onclick = function () {
            this.closest('.scenario-block').remove();
        };
    });
}

// 保存 方案
function saveDecision() {
    const title = document.getElementById('title').value;
    const description = document.getElementById('description').value;
    const decisionCategory = document.getElementById('decisionCategory').value;

    const parameters = Array.from(document.querySelectorAll('.parameter-block')).map(block => ({
        taskParameterId: parseInt(block.querySelector('.task-parameter-id').value),
        value: parseFloat(block.querySelector('.param-value').value),
        comment: block.querySelector('.param-comment').value
    }));

    const scenarios = Array.from(document.querySelectorAll('.scenario-block')).map(block => ({
        title: block.querySelector('.scenario-title').value.trim(),
        description: block.querySelector('.scenario-description').value.trim(),
        possibility: parseFloat(block.querySelector('.scenario-possibility').value)
    })).filter(scenario => scenario.title && scenario.description && !isNaN(scenario.possibility));

    const decisionData = {
        title,
        description,
        decisionCategory,
        decisionStatus: "PROPOSED",
        decisionParameterDtoList: parameters,
        scenariosDto: scenarios
    };

    console.log(JSON.stringify(decisionData, null, 2));

    fetch(`http://localhost:8080/api/v1/decisions/${decisionId}`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(decisionData)
    })
        .then(response => {
            if (response.ok) {
                window.history.back();
            } else {
                alert('错误： 更新时 方案!');
            }
        })
        .catch(error => console.error('错误： 更新时:', error));
}

// 删除 方案
function deleteDecision() {
    fetch(`http://localhost:8080/api/v1/decisions/${decisionId}`, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (response.ok) {
                window.location.href = '/tasks';
            } else {
                alert('错误： 删除时 方案!');
            }
        })
        .catch(error => console.error('错误： 删除时:', error));
}

// 按钮处理器
document.getElementById('add-scenario').addEventListener('click', addScenario);
document.getElementById('save-decision').addEventListener('click', () => {
    showModal("确认 保存", "您确定要保存更改吗？", saveDecision);
});
document.getElementById('delete-decision').addEventListener('click', () => {
    showModal("确认 删除", "您确定要删除该方案吗？", deleteDecision);
});

// 加载时为现有场景绑定处理器
attachRemoveScenarioListeners();
