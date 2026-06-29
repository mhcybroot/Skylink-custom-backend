// Skylink PPW Sync - Background Service Worker
// This runs in the extension's background context, which bypasses the Mixed Content (HTTP/HTTPS) restrictions.

importScripts('env.js');

let syncIntervalMinutes = 1; // Default (chrome.alarms minimum is 1 minute)
const MAX_QUEUE_SIZE = 10000;

// Fetch settings once on startup
chrome.storage.local.get(['authHeader'], (data) => {
    if (data.authHeader) {
        fetch(`${ENV.BASE_URL}/api/v1/extension/settings`, {
            headers: { 'Authorization': data.authHeader }
        })
        .then(res => {
            if (!res.ok) throw new Error("Settings fetch failed");
            const contentType = res.headers.get("content-type");
            if (!contentType || !contentType.includes("application/json")) throw new Error("Settings returned non-JSON");
            return res.json();
        })
        .then(settings => {
            if (settings.syncInterval) {
                syncIntervalMinutes = Math.max(1, Math.ceil(settings.syncInterval / 60));
                console.log("[Skylink PPW] Sync interval set to", syncIntervalMinutes, "minute(s)");
            }
        })
        .catch(err => console.error("Failed to fetch settings:", err));
    }
});

// Use chrome.alarms for reliable periodic sync (works even when service worker is suspended)
chrome.alarms.create('skylink-sync', { periodInMinutes: 1 });

chrome.alarms.onAlarm.addListener((alarm) => {
    if (alarm.name !== 'skylink-sync') return;

    chrome.storage.local.get(['authHeader', 'offlineHistoryQueue'], (data) => {
        const queue = data.offlineHistoryQueue || [];

        // 1. Sync offline queue
        if (queue.length > 0) {
            console.log(`[Skylink Sync] Found ${queue.length} items in offline queue. Attempting sync...`);

            if (data.authHeader) {
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
                        const contentType = res.headers.get("content-type");
                        if (contentType && contentType.includes("application/json")) {
                            return res.json().then(data => {
                                if (data.success) {
                                    console.log(`[Skylink Sync] Successfully synced ${payload.length} items!`);
                                    chrome.storage.local.get(['offlineHistoryQueue'], (latestData) => {
                                        const latestQueue = latestData.offlineHistoryQueue || [];
                                        const remainingQueue = latestQueue.slice(payload.length);
                                        chrome.storage.local.set({ offlineHistoryQueue: remainingQueue });
                                    });
                                } else {
                                    console.log(`[Skylink Sync] Sync failed from server logic. Retrying later.`);
                                }
                            });
                        } else {
                            console.log(`[Skylink Sync] Server returned non-JSON. Retrying later.`);
                        }
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

        // 2. Heartbeat: Check if admin has force-logged out this employee
        if (data.authHeader) {
            fetch(`${ENV.BASE_URL}/api/v1/extension/session-status`, {
                headers: { 'Authorization': data.authHeader }
            })
            .then(res => {
                if (!res.ok) throw new Error("Status fetch failed");
                const contentType = res.headers.get("content-type");
                if (!contentType || !contentType.includes("application/json")) throw new Error("Status returned non-JSON");
                return res.json();
            })
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
});

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
            .then(response => {
                if (!response.ok) throw new Error("Sync request failed");
                const contentType = response.headers.get("content-type");
                if (!contentType || !contentType.includes("application/json")) throw new Error("Sync returned non-JSON");
                return response.json();
            })
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
                const contentType = response.headers.get("content-type");
                if (!contentType || !contentType.includes("application/json")) throw new Error("Server returned non-JSON credentials response");
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
