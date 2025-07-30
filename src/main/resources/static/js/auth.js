// Add this to your dashboard.html, admin.html, profile.html pages
document.addEventListener('DOMContentLoaded', function() {
    // Get JWT token from localStorage
    const token = localStorage.getItem('access_token');
    
    if (token) {
        // Add token to all fetch requests
        const originalFetch = window.fetch;
        window.fetch = function(url, options = {}) {
            if (!options.headers) {
                options.headers = {};
            }
            options.headers['Authorization'] = `Bearer ${token}`;
            return originalFetch(url, options);
        };
        
        // Add token to current page request
        const currentUrl = window.location.pathname;
        if (currentUrl === '/dashboard' || currentUrl === '/admin' || currentUrl === '/profile') {
            // You can make an API call here to verify the token
            fetch('/api/auth/validate-token', {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            }).then(response => {
                if (!response.ok) {
                    // Token is invalid, redirect to login
                    localStorage.removeItem('access_token');
                    window.location.href = '/login';
                }
            }).catch(() => {
                // Error occurred, redirect to login
                localStorage.removeItem('access_token');
                window.location.href = '/login';
            });
        }
    } else {
        // No token found, redirect to login
        if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
            window.location.href = '/login';
        }
    }
});

// Logout function
function logout() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('remember_me');
    localStorage.removeItem('remembered_username');
    window.location.href = '/login';
}