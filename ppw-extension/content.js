// Skylink PPW Sync - Content Script
// This runs invisibly in the background on propertypreswizard.com

// 1. PPW specific logic
if (window.location.hostname.includes('propertypreswizard.com')) {
    console.log("[Skylink Sync] PPW logic active.");
    const syncedWorkOrders = new Set();
    
    function extractAndSync() {
        const rows = document.querySelectorAll('tr[id]');
        let syncCount = 0;
        
        for (const row of rows) {
            const reportId = row.getAttribute('id');
            if (!reportId) continue;
            
            const viewLink = row.querySelector('td.view a[href*="reportinfo/?report_id="]');
            if (!viewLink) continue;
            
            const viewTd = viewLink.closest('td');
            const woTd = viewTd.nextElementSibling;
            if (!woTd) continue;
            
            const workOrderNumber = woTd.textContent.trim();
            if (!workOrderNumber) continue;
            
            if (syncedWorkOrders.has(workOrderNumber)) continue;
            
            try {
                chrome.runtime.sendMessage({
                    action: "sync_ppw",
                    workOrderNumber: workOrderNumber,
                    reportId: reportId
                }, (response) => {
                    if (chrome.runtime.lastError) {
                        // ignore
                    } else if (response && response.success) {
                        // ignore
                    }
                });
                
                syncedWorkOrders.add(workOrderNumber);
                syncCount++;
            } catch (err) {}
        }
    }
    
    extractAndSync();
    
    let timeoutId = null;
    const observer = new MutationObserver((mutations) => {
        let shouldSync = false;
        for (const mutation of mutations) {
            if (mutation.addedNodes.length > 0) {
                shouldSync = true;
                break;
            }
        }
        if (shouldSync) {
            clearTimeout(timeoutId);
            timeoutId = setTimeout(extractAndSync, 1000);
        }
    });
    observer.observe(document.body, { childList: true, subtree: true });
}

// 2. Autofill logic for all sites
function attemptAutofill() {
    chrome.runtime.sendMessage({
        action: "get_credentials",
        hostname: window.location.hostname
    }, (response) => {
        if (chrome.runtime.lastError || !response || !response.success || !response.credentials || response.credentials.length === 0) {
            return; // No credentials or not logged in
        }

        // Try to find the username and password fields
        const passwordFields = document.querySelectorAll('input[type="password"]');
        if (passwordFields.length > 0) {
            const passField = passwordFields[0];
            let userField = null;

            const inputs = document.querySelectorAll('input[type="text"], input[type="email"], input:not([type])');
            for (const input of inputs) {
                if (input.type === 'hidden' || input.style.display === 'none') continue;
                if (input.compareDocumentPosition(passField) & Node.DOCUMENT_POSITION_FOLLOWING) {
                    userField = input;
                }
            }

            if (userField && passField) {
                if (response.credentials.length > 1) {
                    if (document.getElementById('skylink-cred-selector')) return;
                    
                    const selector = document.createElement('select');
                    selector.id = 'skylink-cred-selector';
                    selector.style.cssText = 'margin-left: 5px; padding: 4px; font-size: 12px; border: 1px solid #ccc; border-radius: 4px; background: #fff; color: #333; max-width: 150px;';
                    
                    const defaultOpt = document.createElement('option');
                    defaultOpt.text = 'Skylink Accounts';
                    defaultOpt.value = '';
                    selector.appendChild(defaultOpt);
                    
                    response.credentials.forEach((c, idx) => {
                        const opt = document.createElement('option');
                        opt.text = c.loginId;
                        opt.value = idx;
                        selector.appendChild(opt);
                    });
                    
                    selector.addEventListener('change', (e) => {
                        const selectedIdx = e.target.value;
                        if (selectedIdx === '') return;
                        const cred = response.credentials[selectedIdx];
                        userField.value = cred.loginId;
                        passField.value = cred.password;
                        userField.dispatchEvent(new Event('input', { bubbles: true }));
                        userField.dispatchEvent(new Event('change', { bubbles: true }));
                        passField.dispatchEvent(new Event('input', { bubbles: true }));
                        passField.dispatchEvent(new Event('change', { bubbles: true }));
                    });
                    
                    userField.parentNode.insertBefore(selector, userField.nextSibling);
                } else {
                    const cred = response.credentials[0];
                    userField.value = cred.loginId;
                    passField.value = cred.password;
                    console.log("[Skylink Sync] Autofilled credentials for", window.location.hostname);
                    
                    userField.dispatchEvent(new Event('input', { bubbles: true }));
                    userField.dispatchEvent(new Event('change', { bubbles: true }));
                    passField.dispatchEvent(new Event('input', { bubbles: true }));
                    passField.dispatchEvent(new Event('change', { bubbles: true }));
                }
            }
        }
    });
}

// Run autofill after slight delay to allow DOM to settle, especially for SPAs
setTimeout(attemptAutofill, 1000);

