// Login form handling
document.getElementById('loginForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const remember = document.getElementById('remember').checked;

    // Clear previous alerts
    clearAlerts();

    // Show loading state
    setLoading(true);

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username: username,
                password: password
            })
        });

        const data = await response.json();

        if (response.ok && data.status === 'success') {
            showSuccess('Login successful! Redirecting...');
            
            // Store token if provided
            if (data.access_token) {
                localStorage.setItem('access_token', data.access_token);
            }
            
            // Store remember me preference
            if (remember) {
                localStorage.setItem('remember_me', 'true');
                localStorage.setItem('remembered_username', username);
            }

            // Create form to submit user data to session
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = '/login-success';
            
            // Add user data to form
            const userData = {
                username: username,
                email: data.email || username + '@example.com',
                firstName: data.firstName || 'User',
                lastName: data.lastName || 'Name',
                role: data.role || 'user'
            };
            
            Object.keys(userData).forEach(key => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = key;
                input.value = userData[key];
                form.appendChild(input);
            });
            
            document.body.appendChild(form);
            form.submit();
            
        } else {
            showError(data.message || 'Invalid username or password.');
        }
    } catch (error) {
        console.error('Login error:', error);
        showError('An error occurred. Please try again.');
    } finally {
        setLoading(false);
    }
});

// Google OAuth2 login
function googleLogin() {
    window.location.href = '/oauth2/google';
}

// GitHub OAuth2 login (if you want to add GitHub OAuth2)
function githubLogin() {
    // You can implement GitHub OAuth2 here
    showError('GitHub login is not configured yet.');
}

// Utility functions
function showError(message) {
    const alert = document.createElement('div');
    alert.className = 'alert alert-danger';
    alert.innerHTML = `<i class="fas fa-exclamation-triangle"></i> ${message}`;
    document.getElementById('loginForm').prepend(alert);
}

function showSuccess(message) {
    const alert = document.createElement('div');
    alert.className = 'alert alert-success';
    alert.innerHTML = `<i class="fas fa-check-circle"></i> ${message}`;
    document.getElementById('loginForm').prepend(alert);
}

function clearAlerts() {
    document.querySelectorAll('.alert').forEach(alert => alert.remove());
}

function setLoading(loading) {
    const form = document.getElementById('loginForm');
    const submitBtn = form.querySelector('.signin-btn');
    
    if (loading) {
        form.classList.add('loading');
        submitBtn.textContent = 'Signing In...';
    } else {
        form.classList.remove('loading');
        submitBtn.textContent = 'Sign In';
    }
}

// Auto-fill username if remembered
document.addEventListener('DOMContentLoaded', function() {
    const rememberedUsername = localStorage.getItem('remembered_username');
    if (rememberedUsername) {
        document.getElementById('username').value = rememberedUsername;
        document.getElementById('remember').checked = true;
    }
}); 