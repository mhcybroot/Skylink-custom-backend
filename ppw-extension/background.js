// Skylink PPW Sync - Background Service Worker
// This runs in the extension's background context, which bypasses the Mixed Content (HTTP/HTTPS) restrictions.

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === "sync_ppw") {
        fetch('http://76.13.221.43:8083/api/v1/ppw-mapping', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                workOrderNumber: request.workOrderNumber,
                reportId: request.reportId
            })
        })
        .then(response => response.json())
        .then(data => {
            console.log("[Skylink PPW Background] Sync successful:", data);
            sendResponse({ success: true, data });
        })
        .catch(err => {
            console.error("[Skylink PPW Background] Sync failed:", err);
            sendResponse({ success: false, error: err.message });
        });

        return true; // Indicates we will respond asynchronously
    }
});
