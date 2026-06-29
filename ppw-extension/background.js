// Skylink PPW Sync - Background Service Worker
// This runs in the extension's background context, which bypasses the Mixed Content (HTTP/HTTPS) restrictions.

importScripts('env.js');

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === "sync_ppw") {
        fetch(`${ENV.BASE_URL}/api/v1/ppw-mapping`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                workOrderNumber: request.workOrderNumber,
                reportId: request.reportId
            }),
            credentials: 'include'
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
