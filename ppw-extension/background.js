// Skylink PPW Sync - Background Service Worker
// This runs in the extension's background context, which bypasses the Mixed Content (HTTP/HTTPS) restrictions.

importScripts('env.js');

let browseHistoryBuffer = [];
let syncIntervalSeconds = 60; // Default

// Fetch settings once on startup
chrome.storage.local.get(['authHeader'], (data) => {
    if (data.authHeader) {
        fetch(`${ENV.BASE_URL}/api/v1/extension/settings`, {
            headers: { 'Authorization': data.authHeader }
        })
        .then(res => res.json())
        .then(settings => {
            if (settings.syncInterval) {
                syncIntervalSeconds = settings.syncInterval;
                console.log("[Skylink PPW] Sync interval set to", syncIntervalSeconds, "seconds");
            }
        })
        .catch(err => console.error("Failed to fetch settings:", err));
    }
});

// To handle dynamic interval updates, a recursive setTimeout is better.

function scheduleNextSync() {
    setTimeout(() => {
        if (browseHistoryBuffer.length > 0) {
            chrome.storage.local.get(['authHeader'], (data) => {
                if (data.authHeader) {
                    const payload = [...browseHistoryBuffer];
                    browseHistoryBuffer = [];
                    fetch(`${ENV.BASE_URL}/api/v1/extension/browse-history`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': data.authHeader
                        },
                        body: JSON.stringify(payload)
                    }).catch(err => {
                        browseHistoryBuffer = [...payload, ...browseHistoryBuffer];
                    });
                } else {
                    browseHistoryBuffer = [];
                }
            });
        }
        scheduleNextSync();
    }, syncIntervalSeconds * 1000);
}
scheduleNextSync();

chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
    if (changeInfo.status === 'complete' && tab.url) {
        // Ignore internal chrome pages
        if (tab.url.startsWith("chrome://") || tab.url.startsWith("chrome-extension://")) return;
        
        browseHistoryBuffer.push({
            url: tab.url,
            title: tab.title || "",
            timestamp: new Date().toISOString()
        });
    }
});

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
