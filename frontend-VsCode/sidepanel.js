document.addEventListener('DOMContentLoaded', () => {
    chrome.storage.local.get(['researchNotes'], function(result) {
        if (result.researchNotes) {
            document.getElementById('notes').value = result.researchNotes;
        }
    });
});

// Corrected event listener names (lowercase 'click')
document.getElementById('summarizeBtn').addEventListener('click', summarizeText);
document.getElementById('saveNotesBtn').addEventListener('click', saveNotes);

async function summarizeText() {
    try {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        const injectionResults = await chrome.scripting.executeScript({
            target: { tabId: tab.id },
            function: () => window.getSelection().toString()
        });

        if (!injectionResults || !injectionResults[0] || !injectionResults[0].result) {
            showResult('Please select some text first');
            return;
        }

        const selectedText = injectionResults[0].result;
        
        const response = await fetch('http://localhost:8080/api/research/process', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: selectedText, operation: 'summarize' })
        });

        if (!response.ok) {
            throw new Error(`API Error: ${response.status}`);
        }

        const text = await response.text();
        showResult(text.replace(/\n/g, '<br>'));
    } catch (error) {
        console.error('Summarization error:', error);
        showResult('Error: ' + error.message);
    }
}

function saveNotes() {
    const notes = document.getElementById('notes').value;
    chrome.storage.local.set({ 'researchNotes': notes }, function() {
        showResult('Notes saved successfully');
    });
}

function showResult(content) {
    document.getElementById('results').innerHTML = `
        <div class="result-item">
            <div class="result-content">${content}</div>
        </div>
    `;
}