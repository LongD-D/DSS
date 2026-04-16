let selectedDecisionId = null;

const CA_LEVELS = [
    { label: '证据充分（Ca=1.0）', value: 1.0 },
    { label: '证据较充分（Ca=0.8）', value: 0.8 },
    { label: '证据一般（Ca=0.6）', value: 0.6 },
    { label: '证据偏弱（Ca=0.4）', value: 0.4 },
    { label: '证据很弱（Ca=0.2）', value: 0.2 }
];

const CS_LEVELS = [
    { label: '非常熟悉（Cs=1.0）', value: 1.0 },
    { label: '比较熟悉（Cs=0.8）', value: 0.8 },
    { label: '一般熟悉（Cs=0.6）', value: 0.6 },
    { label: '不太熟悉（Cs=0.4）', value: 0.4 },
    { label: '不熟悉（Cs=0.2）', value: 0.2 }
];

function ensureWeightSelectorsInitialized() {
    const caSelect = document.getElementById('rate-ca');
    const csSelect = document.getElementById('rate-cs');
    if (!caSelect || !csSelect) return;

    if (!caSelect.options.length) {
        CA_LEVELS.forEach(item => {
            const option = document.createElement('option');
            option.value = String(item.value);
            option.textContent = item.label;
            caSelect.appendChild(option);
        });
    }

    if (!csSelect.options.length) {
        CS_LEVELS.forEach(item => {
            const option = document.createElement('option');
            option.value = String(item.value);
            option.textContent = item.label;
            csSelect.appendChild(option);
        });
    }
}

function selectNearestOption(selectElement, value, fallback) {
    if (!selectElement) return;
    const parsed = Number(value);
    const target = Number.isFinite(parsed) ? parsed : fallback;
    const options = Array.from(selectElement.options);
    const nearest = options.reduce((best, current) => {
        return Math.abs(Number(current.value) - target) < Math.abs(Number(best.value) - target)
            ? current
            : best;
    }, options[0]);
    selectElement.value = nearest?.value ?? String(fallback);
}

function openRateModal(decisionId) {
    selectedDecisionId = decisionId;
    const modal = document.getElementById('rate-modal');
    modal.classList.remove('hidden');
    modal.classList.add('flex');

    ensureWeightSelectorsInitialized();
    document.getElementById('rate-score').value = '';
    document.getElementById('rate-comment').value = '';
    selectNearestOption(document.getElementById('rate-ca'), null, 0.8);
    selectNearestOption(document.getElementById('rate-cs'), null, 0.8);

    fetch(`/api/v1/decisions/${decisionId}/evaluation`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (response.status === 204 || response.status === 404) return null;
            return response.json();
        })
        .then(data => {
            if (data) {
                document.getElementById('rate-score').value = data.score;
                document.getElementById('rate-comment').value = data.comment || '';
                selectNearestOption(document.getElementById('rate-ca'), data.ca, 0.8);
                selectNearestOption(document.getElementById('rate-cs'), data.cs, 0.8);
            }
        })
        .catch(error => {
            console.error('错误 加载历史评分时:', error);
        });
}

function closeRateModal() {
    selectedDecisionId = null;
    const modal = document.getElementById('rate-modal');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

function submitRate() {
    const score = parseFloat(document.getElementById('rate-score').value);
    const comment = document.getElementById('rate-comment').value;
    const ca = parseFloat(document.getElementById('rate-ca').value);
    const cs = parseFloat(document.getElementById('rate-cs').value);

    if (!selectedDecisionId) {
        alert('未指定方案 ID');
        return;
    }

    if (isNaN(score) || score < 0 || score > 10) {
        alert('请输入 0 到 10 之间的评分。');
        return;
    }

    if (isNaN(ca) || isNaN(cs)) {
        alert('请选择 Ca / Cs 权重等级。');
        return;
    }

    fetch(`/api/v1/decisions/${selectedDecisionId}/rate`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ score, comment, ca, cs, decisionId: selectedDecisionId })
    })
        .then(response => {
            if (!response.ok) throw new Error('错误 评分时');
            return response.json();
        })
        .then(() => {
            alert('评分已保存！');
            closeRateModal();
            location.reload();
        })
        .catch(error => {
            console.error('错误： 保存时 评分:', error);
            alert('无法保存评分');
        });
}
