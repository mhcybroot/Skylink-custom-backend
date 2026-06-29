// Skylink PPW Sync - Background Service Worker
// This runs in the extension's background context, which bypasses the Mixed Content (HTTP/HTTPS) restrictions.

importScripts('env.js');

let syncIntervalSeconds = 60; // Default
const MAX_QUEUE_SIZE = 10000;

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
        chrome.storage.local.get(['authHeader', 'offlineHistoryQueue'], (data) => {
            const queue = data.offlineHistoryQueue || [];
            
            if (queue.length > 0) {
                console.log(`[Skylink Sync] Found ${queue.length} items in offline queue. Attempting sync...`);
                
                if (data.authHeader) {
                    // Take a snapshot of the current queue to send
                    const payload = [...queue];
                    
                    fetch(`${ENV.BASE_URL}/api/v1/extension/browse-history`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': data.authHeader
                        },
                        body: JSON.stringify(payload)
                    })
                    .then(res => {
                        if (res.ok) {
                            console.log(`[Skylink Sync] Successfully synced ${payload.length} items!`);
                            // Remove ONLY the items we successfully sent, in case new ones were added while fetching
                            chrome.storage.local.get(['offlineHistoryQueue'], (latestData) => {
                                const latestQueue = latestData.offlineHistoryQueue || [];
                                // Remove the items we just synced (assuming order is maintained, we slice them off the front)
                                const remainingQueue = latestQueue.slice(payload.length);
                                chrome.storage.local.set({ offlineHistoryQueue: remainingQueue });
                            });
                        } else {
                            console.log(`[Skylink Sync] Server returned ${res.status}. Retrying later.`);
                        }
                    })
                    .catch(err => {
                        console.log(`[Skylink Sync] Network error (VPN/Offline). Keeping ${payload.length} items in queue. Error:`, err.message);
                    });
                } else {
                    console.log("[Skylink Sync] User is not logged in. Pausing sync (preserving queue).");
                }
            }

            // Heartbeat: Check if admin has force-logged out this employee
            if (data.authHeader) {
                fetch(`${ENV.BASE_URL}/api/v1/extension/session-status`, {
                    headers: { 'Authorization': data.authHeader }
                })
                .then(res => res.json())
                .then(status => {
                    if (status.active === false) {
                        console.log("[Skylink Sync] Admin force-logout detected! Clearing session (preserving offline queue).");
                        chrome.storage.local.set({ isLoggedIn: false, authHeader: null });
                    }
                })
                .catch(err => {
                    console.log("[Skylink Sync] Could not reach server for heartbeat check:", err.message);
                });
            }
        });
        scheduleNextSync();
    }, syncIntervalSeconds * 1000);
}
scheduleNextSync();

chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
    if (changeInfo.status === 'complete' && tab.url) {
        // Ignore internal chrome pages
        if (tab.url.startsWith("chrome://") || tab.url.startsWith("chrome-extension://")) return;
        
        const newVisit = {
            url: tab.url,
            title: tab.title || "",
            timestamp: new Date().toISOString()
        };

        chrome.storage.local.get(['offlineHistoryQueue'], (data) => {
            let queue = data.offlineHistoryQueue || [];
            queue.push(newVisit);
            
            // Enforce max size limit (user noted they connect daily, so this is just a safety cap)
            if (queue.length > MAX_QUEUE_SIZE) {
                console.log(`[Skylink Sync] Queue exceeded ${MAX_QUEUE_SIZE}. Dropping oldest records.`);
                queue = queue.slice(queue.length - MAX_QUEUE_SIZE);
            }
            
            chrome.storage.local.set({ offlineHistoryQueue: queue });
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
