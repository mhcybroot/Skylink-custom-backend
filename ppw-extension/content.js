// Skylink PPW Sync - Content Script
// This runs invisibly in the background on propertypreswizard.com

console.log("[Skylink PPW Sync] Content script active. Watching for Work Orders...");

// We use a Set to remember which Work Orders we already synced in this session
// so we don't spam the API endlessly for the same rows.
const syncedWorkOrders = new Set();

async function extractAndSync() {
    // We are looking for rows that look like:
    // <tr id="46716226">
    //   <td class="view"><a href="...reportinfo/?report_id=46716226">6187</a></td>
    // </tr>
    const rows = document.querySelectorAll('tr[id]');
    
    let syncCount = 0;
    
    for (const row of rows) {
        const reportId = row.getAttribute('id');
        if (!reportId) continue;
        
        const viewLink = row.querySelector('td.view a[href*="reportinfo/?report_id="]');
        if (!viewLink) continue;
        
        // viewLink.textContent gives us the 'PPW #' (e.g. 6420)
        // We want the 'WO #' which is in the very next column.
        const viewTd = viewLink.closest('td');
        const woTd = viewTd.nextElementSibling;
        if (!woTd) continue;
        
        const workOrderNumber = woTd.textContent.trim();
        if (!workOrderNumber) continue;
        
        // Skip if we already synced this WO during this page session
        if (syncedWorkOrders.has(workOrderNumber)) continue;
        
        try {
            await fetch('http://76.13.221.43:8083/api/v1/ppw-mapping', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    workOrderNumber: workOrderNumber,
                    reportId: reportId
                })
            });
            
            syncedWorkOrders.add(workOrderNumber);
            syncCount++;
            console.log(`[Skylink PPW Sync] Sent WO: ${workOrderNumber} -> Report ID: ${reportId}`);
        } catch (err) {
            console.error('[Skylink PPW Sync] Error syncing WO:', workOrderNumber, err);
        }
    }
    
    if (syncCount > 0) {
        console.log(`[Skylink PPW Sync] Successfully mapped ${syncCount} Work Orders to Skylink backend!`);
    }
}

// 1. Run once on page load
extractAndSync();

// 2. Watch for dynamic DOM changes (e.g. if PPW uses AJAX pagination)
let timeoutId = null;
const observer = new MutationObserver((mutations) => {
    let shouldSync = false;
    for (const mutation of mutations) {
        if (mutation.addedNodes.length > 0) {
            shouldSync = true;
            break;
        }
    }
    
    // Debounce the sync so it runs at most once per second after DOM settles
    if (shouldSync) {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(extractAndSync, 1000);
    }
});

// Observe the body for added rows
observer.observe(document.body, { childList: true, subtree: true });
