document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const loginFormState = document.getElementById('loginFormState');
    const loggedInState = document.getElementById('loggedInState');
    const messageEl = document.getElementById('message');
    const submitBtn = document.getElementById('submitBtn');
    const btnText = document.getElementById('btnText');
    const loader = document.getElementById('loader');

    const BASE_URL = ENV.BASE_URL;

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

        await fetchResources();
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
