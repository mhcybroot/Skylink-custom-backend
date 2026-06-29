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

        const cred = response.credentials[0]; // Pick first match
        if (!cred.loginId || !cred.password) return;

        // Try to find the username and password fields
        // Simple heuristic: password field is type="password"
        // Username field is usually type="text" or type="email" right before the password field
        const passwordFields = document.querySelectorAll('input[type="password"]');
        if (passwordFields.length > 0) {
            const passField = passwordFields[0];
            let userField = null;

            // Look for username field (type=text or email)
            const inputs = document.querySelectorAll('input[type="text"], input[type="email"], input:not([type])');
            for (const input of inputs) {
                // Ignore hidden inputs
                if (input.type === 'hidden' || input.style.display === 'none') continue;
                // Just grab the last text input before the password field
                if (input.compareDocumentPosition(passField) & Node.DOCUMENT_POSITION_FOLLOWING) {
                    userField = input;
                }
            }

            if (userField && passField) {
                userField.value = cred.loginId;
                passField.value = cred.password;
                console.log("[Skylink Sync] Autofilled credentials for", window.location.hostname);
                
                // Trigger events so React/Vue/Angular notice the change
                userField.dispatchEvent(new Event('input', { bubbles: true }));
                userField.dispatchEvent(new Event('change', { bubbles: true }));
                passField.dispatchEvent(new Event('input', { bubbles: true }));
                passField.dispatchEvent(new Event('change', { bubbles: true }));
            }
        }
    });
}

// Run autofill after slight delay to allow DOM to settle, especially for SPAs
setTimeout(attemptAutofill, 1000);

