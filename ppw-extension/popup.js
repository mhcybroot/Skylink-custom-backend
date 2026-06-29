document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const loginFormState = document.getElementById('loginFormState');
    const loggedInState = document.getElementById('loggedInState');
    const messageEl = document.getElementById('message');
    const submitBtn = document.getElementById('submitBtn');
    const btnText = document.getElementById('btnText');
    const loader = document.getElementById('loader');
    const logoutBtn = document.getElementById('logoutBtn');

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
        
        // Show loading state
        submitBtn.disabled = true;
        btnText.style.display = 'none';
        loader.style.display = 'block';
        messageEl.textContent = '';
        
        try {
            // Spring Security form login expects application/x-www-form-urlencoded
            const params = new URLSearchParams();
            params.append('username', username);
            params.append('password', password);
            
            const response = await fetch(`${BASE_URL}/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: params,
                // include credentials so browser saves the session cookie for this domain
                credentials: 'include' 
            });
            
            // Spring Security typically returns 302 on success, which fetch follows automatically.
            // If it follows and gets 200 on a page like '/' or '/dashboard' (or stays on /login?error), we check URL.
            if (response.url.includes('error')) {
                throw new Error('Invalid username or password');
            }
            
            // If successful
            chrome.storage.local.set({ isLoggedIn: true }, () => {
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

    logoutBtn.addEventListener('click', async () => {
        try {
            // Tell backend to logout
            await fetch(`${BASE_URL}/logout`, { 
                method: 'GET',
                credentials: 'include'
            });
        } catch (e) {
            console.error("Logout fetch failed", e);
        }
        
        // Clear state locally
        chrome.storage.local.set({ isLoggedIn: false }, () => {
            showLoginForm();
            document.getElementById('username').value = '';
            document.getElementById('password').value = '';
            messageEl.textContent = '';
        });
    });

    function showLoggedIn() {
        loginFormState.style.display = 'none';
        loggedInState.style.display = 'block';
    }

    function showLoginForm() {
        loginFormState.style.display = 'block';
        loggedInState.style.display = 'none';
    }
});
