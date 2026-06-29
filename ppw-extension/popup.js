document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const loginFormState = document.getElementById('loginFormState');
    const loggedInState = document.getElementById('loggedInState');
    const messageEl = document.getElementById('message');
    const submitBtn = document.getElementById('submitBtn');
    const btnText = document.getElementById('btnText');
    const loader = document.getElementById('loader');

    const BASE_URL = ENV.BASE_URL;
    let timerInterval = null;

    // Check login state on load
    chrome.storage.local.get(['isLoggedIn'], (result) => {
        if (result.isLoggedIn) {
            showLoggedIn();
        } else {
            showLoginForm();
        }
    });

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        const base64Creds = btoa(username + ':' + password);
        
        // Show loading state
        submitBtn.disabled = true;
        btnText.style.display = 'none';
        loader.style.display = 'block';
        messageEl.textContent = '';
        
        try {
            const response = await fetch(`${BASE_URL}/api/v1/extension/credentials`, {
                method: 'GET',
                headers: {
                    'Authorization': 'Basic ' + base64Creds
                }
            });
            
            if (!response.ok) {
                throw new Error('Invalid username or password');
            }
            
            // If successful, save credentials
            chrome.storage.local.set({ isLoggedIn: true, authHeader: 'Basic ' + base64Creds }, () => {
                showLoggedIn();
            });
            
        } catch (error) {
            messageEl.textContent = error.message || 'Connection failed. Check network.';
            messageEl.className = 'message error';
        } finally {
            submitBtn.disabled = false;
            btnText.style.display = 'block';
            loader.style.display = 'none';
        }
    });




    async function showLoggedIn() {
        loginFormState.style.display = 'none';
        loggedInState.style.display = 'block';

        // Immediately verify session with server (catches admin force-logout)
        try {
            const storageData = await new Promise((resolve) => {
                chrome.storage.local.get(['authHeader'], resolve);
            });
            if (storageData.authHeader) {
                const statusRes = await fetch(`${BASE_URL}/api/v1/extension/session-status`, {
                    headers: { 'Authorization': storageData.authHeader }
                });
                const status = await statusRes.json();
                if (status.active === false) {
                    console.log("[Skylink Popup] Admin force-logout detected!");
                    chrome.storage.local.set({ isLoggedIn: false, authHeader: null }, () => {
                        showLoginForm();
                    });
                    return;
                }
            }
        } catch (e) {
            // Server unreachable — just show logged in state as usual
            console.log("[Skylink Popup] Could not verify session:", e.message);
        }

        await Promise.all([fetchResources(), fetchDashboardStatus()]);
    }

    async function fetchDashboardStatus() {
        try {
            const data = await new Promise((resolve) => chrome.storage.local.get(['authHeader'], resolve));
            if (!data.authHeader) return;

            const res = await fetch(`${BASE_URL}/api/v1/extension/dashboard-status`, {
                headers: { 'Authorization': data.authHeader }
            });
            if (!res.ok) throw new Error("Status fetch failed");
            
            const statusData = await res.json();
            renderDashboardStatus(statusData);
        } catch (e) {
            document.getElementById('countdownSub').textContent = "Unable to load status.";
        }
    }

    function renderDashboardStatus(data) {
        const statusBadge = document.getElementById('statusBadge');
        const countdownTimer = document.getElementById('countdownTimer');
        const countdownSub = document.getElementById('countdownSub');
        const progressFill = document.getElementById('progressFill');
        const serverTimeDisplay = document.getElementById('serverTimeDisplay');

        // Update badge
        statusBadge.textContent = data.status.replace(/_/g, ' ');
        statusBadge.className = 'status-badge';
        if (data.status === 'WORKING' || data.status === 'ENTERED_OFFICE') statusBadge.classList.add('working');
        else if (data.status === 'ON_BREAK') statusBadge.classList.add('on-break');
        else if (data.status === 'ENDED_WORK' || data.status === 'LEFT_WITHOUT_PUNCH' || data.status === 'COMPLETED_DAY') statusBadge.classList.add('ended');
        else statusBadge.classList.add('not-entered');

        // Calculate offset between server clock and local PC clock
        const serverTime = new Date(data.serverTimeISO).getTime();
        const localTime = Date.now();
        const clockOffset = serverTime - localTime;

        let workStartMs = data.workStartISO ? new Date(data.workStartISO).getTime() : serverTime;
        let breakStartMs = data.breakStartISO ? new Date(data.breakStartISO).getTime() : null;
        let totalBreakSecs = data.totalBreakSeconds || 0;
        let shiftDurationSecs = data.shiftDurationSeconds || (8 * 3600);

        function updateTimer() {
            // Apply timezone compensation
            const nowServerSynced = Date.now() + clockOffset;
            
            // Format Server Time (e.g. 10:30:15 AM)
            const serverDateObj = new Date(nowServerSynced);
            let th = serverDateObj.getHours();
            const tm = serverDateObj.getMinutes();
            const ts = serverDateObj.getSeconds();
            const ampm = th >= 12 ? 'PM' : 'AM';
            th = th % 12;
            th = th ? th : 12; 
            serverTimeDisplay.textContent = 'Server Time: ' + 
                (th < 10 ? '0'+th : th) + ':' + 
                (tm < 10 ? '0'+tm : tm) + ':' + 
                (ts < 10 ? '0'+ts : ts) + ' ' + ampm;

            if (data.status === 'NOT_ENTERED' || data.status === 'ENTERED_OFFICE' || data.status === 'LOGGED_IN') {
                countdownTimer.textContent = "--:--:--";
                countdownSub.textContent = "Start work to begin your shift.";
                progressFill.style.width = '0%';
                return;
            }

            if (data.status === 'ENDED_WORK' || data.status === 'LEFT_WITHOUT_PUNCH' || data.status === 'COMPLETED_DAY') {
                countdownTimer.textContent = "00:00:00";
                countdownSub.textContent = "Shift ended.";
                progressFill.style.width = '100%';
                return;
            }

            let activeBreakSecs = 0;

            if (data.status === 'ON_BREAK' && breakStartMs) {
                activeBreakSecs = Math.floor((nowServerSynced - breakStartMs) / 1000);
            }

            const elapsedTotalSecs = Math.floor((nowServerSynced - workStartMs) / 1000);
            const elapsedWorkSecs = Math.max(0, elapsedTotalSecs - totalBreakSecs - activeBreakSecs);
            const remainingSecs = Math.max(0, shiftDurationSecs - elapsedWorkSecs);

            // Format time
            const h = Math.floor(remainingSecs / 3600);
            const m = Math.floor((remainingSecs % 3600) / 60);
            const s = remainingSecs % 60;
            countdownTimer.textContent = 
                (h < 10 ? '0'+h : h) + ':' + 
                (m < 10 ? '0'+m : m) + ':' + 
                (s < 10 ? '0'+s : s);

            if (data.status === 'ON_BREAK') {
                countdownSub.textContent = "Timer paused while on break.";
            } else {
                countdownSub.textContent = "Time remaining in shift.";
            }

            const progress = Math.min(100, (elapsedWorkSecs * 100) / shiftDurationSecs);
            progressFill.style.width = progress + '%';
        }

        if (timerInterval) clearInterval(timerInterval);
        updateTimer();
        timerInterval = setInterval(updateTimer, 1000);
    }

    async function fetchResources() {
        const resourcesList = document.getElementById('resourcesList');
        const rLoader = document.getElementById('resourcesLoader');
        
        resourcesList.innerHTML = '';
        rLoader.style.display = 'block';

        try {
            const data = await new Promise((resolve) => {
                chrome.storage.local.get(['authHeader'], resolve);
            });
            
            if (!data.authHeader) {
                chrome.storage.local.set({ isLoggedIn: false }, () => {
                    showLoginForm();
                });
                return;
            }

            const response = await fetch(`${BASE_URL}/api/v1/extension/credentials`, {
                method: 'GET',
                headers: {
                    'Authorization': data.authHeader
                }
            });

            if (response.status === 401) {
                // Password changed or invalid, force logout
                chrome.storage.local.set({ isLoggedIn: false, authHeader: null }, () => {
                    showLoginForm();
                });
                return;
            }

            if (!response.ok) throw new Error("Failed to load resources");
            
            const resources = await response.json();
            
            if (resources.length === 0) {
                resourcesList.innerHTML = '<div class="empty-state">No shared resources assigned.</div>';
            } else {
                resources.forEach(res => {
                    const div = document.createElement('div');
                    div.className = 'resource-item';
                    
                    const name = document.createElement('p');
                    name.className = 'resource-name';
                    name.textContent = res.resourceName;
                    
                    const login = document.createElement('p');
                    login.className = 'resource-login';
                    login.innerHTML = `<strong>User:</strong> ${res.loginId || 'N/A'}`;
                    
                    div.appendChild(name);
                    div.appendChild(login);
                    resourcesList.appendChild(div);
                });
            }
        } catch (e) {
            resourcesList.innerHTML = '<div class="empty-state">Error loading resources.</div>';
        } finally {
            rLoader.style.display = 'none';
        }
    }

    function showLoginForm() {
        loginFormState.style.display = 'block';
        loggedInState.style.display = 'none';
    }
});
