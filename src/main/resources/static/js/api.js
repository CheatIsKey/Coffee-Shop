/**
 * api.js
 * Wrapper for API requests to handle common logic like error handling and credentials.
 */

const API_BASE = '/api';

// Toast Notification System
window.showToast = (message, type = 'success') => {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    const icon = type === 'success' ? 'check-circle' : 'warning-circle';
    toast.innerHTML = `<i class="ph-fill ph-${icon}"></i> <span>${message}</span>`;
    
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease forwards';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
};

// Loader System
window.showLoader = () => {
    let loader = document.getElementById('global-loader');
    if (!loader) {
        loader = document.createElement('div');
        loader.id = 'global-loader';
        loader.className = 'loader-overlay';
        loader.innerHTML = '<div class="spinner"></div>';
        document.body.appendChild(loader);
    }
    loader.classList.add('active');
};

window.hideLoader = () => {
    const loader = document.getElementById('global-loader');
    if (loader) loader.classList.remove('active');
};

async function apiFetch(endpoint, options = {}) {
    window.showLoader();
    try {
        const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint}`;
        
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
            },
            // Important for sending Cookies (Access & Refresh tokens)
            credentials: 'same-origin' 
        };

        const finalOptions = {
            ...defaultOptions,
            ...options,
            headers: { ...defaultOptions.headers, ...options.headers }
        };

        if (finalOptions.body && typeof finalOptions.body !== 'string') {
            finalOptions.body = JSON.stringify(finalOptions.body);
        }

        const response = await fetch(url, finalOptions);
        
        // Handle 429 Too Many Requests
        if (response.status === 429) {
            window.showToast('Too Many Requests - 짧은 시간 내 주문이 너무 많습니다. 잠시 후 다시 시도해주세요.', 'error');
            throw new Error('Too Many Requests');
        }

        // Global Exception Handling according to standard ApiResponse
        const data = await response.json().catch(() => null);

        if (!response.ok) {
            let errorMessage = '서버 에러가 발생했습니다.';
            if (data) {
                if (data.data && data.data.fieldErrors && data.data.fieldErrors.length > 0) {
                    errorMessage = data.data.fieldErrors[0].message;
                } else if (data.data && data.data.message) {
                    errorMessage = data.data.message;
                } else if (data.message) {
                    errorMessage = data.message;
                }
            }
            
            // Do not show toast for 401/403 if it's just checking auth state
            if (response.status === 401 && endpoint === '/points/me') {
                throw new Error('Unauthorized');
            }
            // Do not show toast for 401/403 when checking admin status
            if ((response.status === 401 || response.status === 403) && endpoint.startsWith('/orders/admin')) {
                throw new Error('Forbidden');
            }
            
            window.showToast(errorMessage, 'error');
            throw new Error(errorMessage);
        }

        return data.data; // Return the inner data from ApiResponse

    } catch (error) {
        throw error;
    } finally {
        window.hideLoader();
    }
}

const api = {
    auth: {
        login: (credentials) => apiFetch('/auth/login', { method: 'POST', body: credentials }),
        signup: (userData) => apiFetch('/auth/signup', { method: 'POST', body: userData }),
        logout: () => apiFetch('/auth/logout', { method: 'POST' }),
        reissue: () => apiFetch('/auth/reissue', { method: 'POST' })
    },
    menus: {
        getPopular: () => apiFetch('/menus/popular'),
        getAll: (page = 1, size = 100) => apiFetch(`/menus?page=${page}&size=${size}`),
        create: (menuData) => apiFetch('/menus', { method: 'POST', body: menuData }),
        update: (menuId, menuData) => apiFetch(`/menus/${menuId}`, { method: 'PUT', body: menuData }),
        delete: (menuId) => apiFetch(`/menus/${menuId}`, { method: 'DELETE' })
    },
    points: {
        getMe: (size = 50) => apiFetch(`/points/me?size=${size}`),
        charge: (amount) => apiFetch('/points/charge', { method: 'POST', body: { amount: amount, paymentType: 'CARD' } })
    },
    orders: {
        create: (orderData) => apiFetch('/orders', { method: 'POST', body: orderData }),
        getMyOrders: (cursorId = null, size = 50) => apiFetch(`/orders?size=${size}${cursorId ? '&cursorId=' + cursorId : ''}`),
        getAdminOrders: (page = 0, size = 100, params = {}) => {
            const qs = new URLSearchParams({ page, size });
            if (params.orderUid)  qs.append('orderUid',  params.orderUid);
            if (params.email)     qs.append('email',     params.email);
            if (params.startDate) qs.append('startDate', params.startDate);
            if (params.endDate)   qs.append('endDate',   params.endDate);
            return apiFetch(`/orders/admin?${qs.toString()}`);
        },
        getOrderDetail: (orderUid) => apiFetch(`/orders/${orderUid}`)
    }
};

window.api = api;
