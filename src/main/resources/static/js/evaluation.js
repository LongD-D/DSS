let selectedDecisionId = null;

function openRateModal(decisionId) {
    selectedDecisionId = decisionId;
    const modal = document.getElementById('rate-modal');
    modal.classList.remove('hidden');
    modal.classList.add('flex');

    document.getElementById('rate-score').value = '';
    document.getElementById('rate-comment').value = '';

    fetch(`/api/v1/decisions/${decisionId}/evaluation`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (response.status === 204) return null;
            return response.json();
        })
        .then(data => {
            if (data) {
                document.getElementById('rate-score').value = data.score;
                document.getElementById('rate-comment').value = data.comment || '';
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

    if (!selectedDecisionId) {
        alert('未指定方案 ID');
        return;
    }

    if (isNaN(score) || score < 0 || score > 10) {
        alert('请输入 0 到 10 之间的评分。');
        return;
    }

    fetch(`/api/v1/decisions/${selectedDecisionId}/rate`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ score, comment, decisionId: selectedDecisionId })
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