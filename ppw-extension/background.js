// Skylink PPW Sync - Background Service Worker
// This runs in the extension's background context, which bypasses the Mixed Content (HTTP/HTTPS) restrictions.

importScripts('env.js');

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === "sync_ppw") {
        chrome.storage.local.get(['authHeader'], (data) => {
            if (!data.authHeader) {
                sendResponse({ success: false, error: "Not logged in" });
                return;
            }
            fetch(`${ENV.BASE_URL}/api/v1/ppw-mapping`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': data.authHeader
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
        });
        return true; // Indicates we will respond asynchronously
    } else if (request.action === "get_credentials") {
        chrome.storage.local.get(['authHeader'], (data) => {
            if (!data.authHeader) {
                sendResponse({ success: false, error: "Not logged in" });
                return;
            }
            fetch(`${ENV.BASE_URL}/api/v1/extension/credentials`, {
                method: 'GET',
                headers: {
                    'Authorization': data.authHeader
                }
            })
            .then(response => {
                if (!response.ok) throw new Error("Not logged in or failed to fetch credentials");
                return response.json();
            })
            .then(resources => {
                // Filter resources that match the requested hostname
                const matches = resources.filter(res => {
                    if (!res.resourceLink) return false;
                    try {
                        const resUrl = new URL(res.resourceLink);
                        return resUrl.hostname === request.hostname || 
                               resUrl.hostname.includes(request.hostname) ||
                               request.hostname.includes(resUrl.hostname);
                    } catch(e) {
                        return false;
                    }
                });
                sendResponse({ success: true, credentials: matches });
            })
            .catch(err => {
                console.error("[Skylink PPW Background] Credentials fetch failed:", err);
                sendResponse({ success: false, error: err.message });
            });
        });
        return true;
    }
});
