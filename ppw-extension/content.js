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
                    if (document.getElementById('skylink-cred-wrapper')) return;
                    
                    const wrapper = document.createElement('div');
                    wrapper.id = 'skylink-cred-wrapper';
                    wrapper.style.cssText = 'position: relative; display: inline-block; margin-left: 8px; vertical-align: middle; z-index: 99999; font-family: system-ui, -apple-system, sans-serif;';
                    
                    const triggerBtn = document.createElement('button');
                    triggerBtn.type = 'button';
                    triggerBtn.style.cssText = 'background: #594af2; color: white; border: none; border-radius: 20px; padding: 5px 12px; font-size: 13px; font-weight: 500; cursor: pointer; box-shadow: 0 2px 4px rgba(0,0,0,0.1); display: flex; align-items: center; gap: 6px; transition: background 0.2s; line-height: 1.5; outline: none;';
                    triggerBtn.innerHTML = `
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"></path></svg>
                        Skylink Accounts
                    `;
                    
                    triggerBtn.addEventListener('mouseover', () => triggerBtn.style.background = '#4537d1');
                    triggerBtn.addEventListener('mouseout', () => triggerBtn.style.background = triggerBtn.dataset.filled ? '#10b981' : '#594af2');
                    
                    const menu = document.createElement('div');
                    menu.style.cssText = 'position: absolute; top: 100%; left: 0; margin-top: 6px; background: white; border: 1px solid #eaeaea; border-radius: 8px; box-shadow: 0 10px 25px rgba(0,0,0,0.15); display: none; flex-direction: column; min-width: 200px; overflow: hidden;';
                    
                    // Header
                    const header = document.createElement('div');
                    header.style.cssText = 'padding: 8px 16px; background: #f8f9fa; font-size: 11px; font-weight: 600; color: #6c757d; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid #eaeaea;';
                    header.textContent = 'Select an account';
                    menu.appendChild(header);

                    response.credentials.forEach((c) => {
                        const item = document.createElement('div');
                        item.style.cssText = 'padding: 12px 16px; font-size: 13px; font-weight: 500; color: #333; cursor: pointer; transition: background 0.2s; border-bottom: 1px solid #f5f5f5; text-align: left; display: flex; align-items: center; gap: 8px;';
                        
                        item.innerHTML = `
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6c757d" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                            ${c.loginId}
                        `;
                        
                        item.addEventListener('mouseover', () => {
                            item.style.background = '#f4f2ff';
                            item.style.color = '#594af2';
                            item.querySelector('svg').style.stroke = '#594af2';
                        });
                        item.addEventListener('mouseout', () => {
                            item.style.background = 'white';
                            item.style.color = '#333';
                            item.querySelector('svg').style.stroke = '#6c757d';
                        });
                        
                        item.addEventListener('click', () => {
                            userField.value = c.loginId;
                            passField.value = c.password;
                            userField.dispatchEvent(new Event('input', { bubbles: true }));
                            userField.dispatchEvent(new Event('change', { bubbles: true }));
                            passField.dispatchEvent(new Event('input', { bubbles: true }));
                            passField.dispatchEvent(new Event('change', { bubbles: true }));
                            
                            menu.style.display = 'none';
                            triggerBtn.dataset.filled = 'true';
                            triggerBtn.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg> Autofilled`;
                            triggerBtn.style.background = '#10b981';
                        });
                        
                        menu.appendChild(item);
                    });
                    
                    if(menu.lastChild) menu.lastChild.style.borderBottom = 'none';
                    
                    triggerBtn.addEventListener('click', (e) => {
                        e.preventDefault();
                        menu.style.display = menu.style.display === 'none' ? 'flex' : 'none';
                    });
                    
                    document.addEventListener('click', (e) => {
                        if (!wrapper.contains(e.target)) {
                            menu.style.display = 'none';
                        }
                    });
                    
                    wrapper.appendChild(triggerBtn);
                    wrapper.appendChild(menu);
                    
                    userField.parentNode.insertBefore(wrapper, userField.nextSibling);
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

