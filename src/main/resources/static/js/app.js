/**
 * app.js
 * SPA Hash Router and UI Components
 */

// --- STATE ---
let cart = JSON.parse(localStorage.getItem('allday_cart')) || [];
let isLoggedIn = false;
let isAdmin = false;
let userPoint = 0;

const menuImages = {
    '아메리카노': '/images/americano_img_1778421410492.png',
    '딸기 라떼': '/images/strawberry_latte_img_1778421679046.png',
    '바스크 치즈 케이크': '/images/basque_cheesecake_img_1778421706305.png',
    '디카페인 오트 라떼': '/images/decaf_oat_latte_img_1778421902400.png',
    '1.5L 대용량 아이스티': '/images/iced_tea_img_1778421984785.png'
};
const defaultImg = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="300" height="300"><rect width="100%" height="100%" fill="%23e5e7eb"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-family="sans-serif" font-size="20" fill="%239ca3af">Image Not Found</text></svg>';

function getMenuImage(name) {
    return menuImages[name] || defaultImg;
}

function saveCart() {
    localStorage.setItem('allday_cart', JSON.stringify(cart));
}

function addToCart(menu) {
    if (!isLoggedIn) {
        window.showToast('로그인 후 이용 가능합니다.', 'error');
        window.location.hash = '#/login';
        return;
    }
    const existing = cart.find(item => item.menuId === menu.id);
    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({
            menuId: menu.id,
            name: menu.name,
            price: menu.price,
            quantity: 1,
            image: getMenuImage(menu.name)
        });
    }
    saveCart();
    window.showToast(`${menu.name} 장바구니에 담겼습니다.`);
    renderNav();
}

function removeFromCart(menuId) {
    cart = cart.filter(item => item.menuId !== menuId);
    saveCart();
    renderRoute();
}

async function checkAuth() {
    try {
        const data = await window.api.points.getMe();
        isLoggedIn = true;
        userPoint = data?.currentPoint || 0;
        
        try {
            await window.api.orders.getAdminOrders(0, 1);
            isAdmin = true;
        } catch(e) {
            isAdmin = false;
        }
    } catch (e) {
        isLoggedIn = false;
        isAdmin = false;
        userPoint = 0;
    }
    renderNav();
}

// --- NAVBAR ---
function renderNav() {
    const navLinks = document.getElementById('navLinks');
    const totalCartItems = cart.reduce((a, c) => a + c.quantity, 0);

    if (isLoggedIn) {
        navLinks.innerHTML = `
            <a href="#/cart" class="nav-link cart-link"><i class="ph ph-shopping-cart"></i> 장바구니 <span class="cart-badge">${totalCartItems}</span></a>
            <a href="#/orders" class="nav-link">주문내역</a>
            <a href="#/mypage" class="nav-link">마이페이지</a>
            ${isAdmin ? `<a href="#/admin" class="nav-link" style="color:var(--danger);"><i class="ph-bold ph-shield-check"></i> 관리자</a>` : ''}
            <button class="nav-btn-outline" onclick="handleLogout()">로그아웃</button>
        `;
    } else {
        navLinks.innerHTML = `
            <a href="#/login" class="nav-link">로그인</a>
            <a href="#/signup" class="nav-btn">회원가입</a>
        `;
    }
}

async function handleLogout() {
    try {
        await window.api.auth.logout();
    } catch (e) { }
    isLoggedIn = false;
    isAdmin = false;
    userPoint = 0;
    cart = [];
    saveCart();
    window.showToast('로그아웃 되었습니다.');
    window.location.hash = '#/';
    renderNav();
}

// --- VIEWS ---

const views = {
    home: async () => {
        let popularHtml = '';
        let menusHtml = '';

        // Fetch menus gracefully
        let popularMenus = [];
        let allMenus = [];

        try {
            const allResp = await window.api.menus.getAll(1, 100);
            allMenus = allResp?.content || [];
        } catch (e) {
            menusHtml = `<div class="empty-state">메뉴 목록을 불러오지 못했습니다.</div>`;
        }

        try {
            popularMenus = await window.api.menus.getPopular() || [];
        } catch (e) {
            console.warn("인기 메뉴를 불러오지 못했습니다.", e);
        }

        const getFullMenu = (id) => allMenus.find(m => m.menuId === id) || null;

        if (popularMenus.length > 0) {
            popularHtml = popularMenus.map((m, idx) => {
                const fullMenu = getFullMenu(m.menuId);
                // m은 PopularMenuResponse (menuId, menuName, totalQuantity)
                // fullMenu는 MenuListItemResponse (menuId, menuName, menuPrice)
                const price = fullMenu ? fullMenu.menuPrice : 0;
                const name = fullMenu ? fullMenu.menuName : m.menuName;
                const id = m.menuId;
                
                if (!price) return '';
                
                return `
                <div class="menu-card">
                    <div class="menu-badge"><i class="ph-fill ph-crown"></i> TOP ${idx + 1}</div>
                    <img src="${getMenuImage(name)}" alt="${name}" class="menu-image">
                    <div class="menu-content">
                        <h3 class="menu-title">${name}</h3>
                        <div class="menu-price">${price.toLocaleString()} 원</div>
                        <button class="btn-add-cart" onclick='addToCart({id: ${id}, name: "${name}", price: ${price}})'>
                            <i class="ph-bold ph-plus"></i> 장바구니 담기
                        </button>
                    </div>
                </div>`;
            }).join('');
        }

        if (allMenus.length > 0) {
            menusHtml = allMenus.map(m => `
                <div class="menu-card">
                    <img src="${getMenuImage(m.menuName)}" alt="${m.menuName}" class="menu-image">
                    <div class="menu-content">
                        <h3 class="menu-title">${m.menuName}</h3>
                        <div class="menu-price">${m.menuPrice.toLocaleString()} 원</div>
                        <button class="btn-add-cart" onclick='addToCart({id: ${m.menuId}, name: "${m.menuName}", price: ${m.menuPrice}})'>
                            <i class="ph-bold ph-plus"></i> 장바구니 담기
                        </button>
                    </div>
                </div>`).join('');
        }

        return `
            <div class="hero">
                <h1>프리미엄 원두, 완벽한 하루</h1>
                <p>가장 신선한 재료로 만든 커피를 경험해보세요.</p>
            </div>
            
            ${popularHtml ? `
            <h2 class="section-title">✨ 이번 주 인기 메뉴</h2>
            <div class="menu-grid" style="margin-bottom: 4rem;">
                ${popularHtml}
            </div>` : ''}

            <h2 class="section-title">☕ 전체 메뉴</h2>
            <div class="menu-grid">
                ${menusHtml}
            </div>
        `;
    },

    login: async () => `
        <div class="auth-container">
            <div class="auth-box">
                <div class="auth-header">
                    <h1>로그인</h1>
                    <p>AllDay Coffee에 오신 것을 환영합니다.</p>
                </div>
                <form id="loginForm" onsubmit="handleLogin(event)">
                    <div class="form-group">
                        <label>이메일</label>
                        <input type="email" id="loginEmail" class="form-input" placeholder="example@email.com" required>
                    </div>
                    <div class="form-group">
                        <label>비밀번호</label>
                        <input type="password" id="loginPassword" class="form-input" placeholder="비밀번호를 입력해주세요" required>
                    </div>
                    <button type="submit" class="btn-primary auth-btn">로그인</button>
                </form>
                <div class="auth-footer">
                    <a href="#/signup" class="auth-link">아직 계정이 없으신가요? <b>회원가입</b></a>
                </div>
            </div>
        </div>
    `,

    signup: async () => `
        <div class="auth-container">
            <div class="auth-box">
                <div class="auth-header">
                    <h1>회원가입</h1>
                    <p>간단한 가입으로 프리미엄 커피를 즐겨보세요.</p>
                </div>
                <form id="signupForm" onsubmit="handleSignup(event)">
                    <div class="form-group">
                        <label>이메일</label>
                        <input type="email" id="signupEmail" class="form-input" placeholder="example@email.com" required>
                    </div>
                    <div class="form-group">
                        <label>비밀번호</label>
                        <input type="password" id="signupPassword" class="form-input" placeholder="8자 이상 20자 이하" required minlength="8" maxlength="20">
                    </div>
                    <button type="submit" class="btn-primary auth-btn">가입하기</button>
                </form>
                <div class="auth-footer">
                    <a href="#/login" class="auth-link">이미 계정이 있으신가요? <b>로그인</b></a>
                </div>
            </div>
        </div>
    `,

    mypage: async () => {
        if (!isLoggedIn) {
            window.location.hash = '#/login';
            return '';
        }

        let pointData;
        try {
            pointData = await window.api.points.getMe();
            userPoint = pointData?.currentPoint || 0;
        } catch (e) {
            return `<div class="empty-state">포인트 정보를 불러오는데 실패했습니다.</div>`;
        }

        const logs = pointData?.history || [];
        const logsHtml = logs.map(log => {
            const isCharge = log.type === '충전';
            return `
            <div class="log-item">
                <div class="log-left">
                    <div class="log-type ${isCharge ? 'type-charge' : 'type-use'}">
                        ${isCharge ? '<i class="ph-bold ph-arrow-down-left"></i> 충전' : '<i class="ph-bold ph-arrow-up-right"></i> 사용'}
                    </div>
                    <div class="log-date">${new Date(log.createdAt).toLocaleString()}</div>
                </div>
                <div class="log-right">
                    <div class="log-amount ${isCharge ? 'positive' : 'negative'}">
                        ${isCharge ? '+' : '-'}${log.amount.toLocaleString()} P
                    </div>
                    <div class="log-balance">잔액: ${log.remainPoint.toLocaleString()} P</div>
                </div>
            </div>
        `}).join('');


        return `
            <div class="page-container">
                <h2 class="page-title">마이페이지</h2>
                
                <div class="profile-dashboard">
                    <div class="point-card">
                        <div class="point-label">보유 포인트</div>
                        <div class="point-balance-large">${userPoint.toLocaleString()} <span>P</span></div>
                    </div>
                    
                    <div class="charge-card">
                        <h3>포인트 충전</h3>
                        <p>1P는 1원과 동일한 가치를 지닙니다.</p>
                        <div class="charge-input-group">
                            <input type="number" id="chargeAmount" class="form-input" placeholder="금액 입력" step="1000" min="1000">
                            <button class="btn-primary" onclick="handleCharge()">충전하기</button>
                        </div>
                    </div>
                </div>

                <div class="history-section">
                    <h3>포인트 이용 내역</h3>
                    <div class="history-list">
                        ${logs.length > 0 ? logsHtml : '<div class="empty-state"><i class="ph ph-receipt"></i><p>이용 내역이 없습니다.</p></div>'}
                    </div>
                </div>
            </div>
        `;
    },

    cart: async () => {
        if (!isLoggedIn) {
            window.location.hash = '#/login';
            return '';
        }

        if (cart.length === 0) {
            return `
                <div class="page-container">
                    <h2 class="page-title">장바구니</h2>
                    <div class="empty-state large">
                        <i class="ph ph-shopping-bag"></i>
                        <p>장바구니가 비어있습니다.</p>
                        <a href="#/" class="btn-primary empty-btn">메뉴 담으러 가기</a>
                    </div>
                </div>
            `;
        }

        const total = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);

        const cartHtml = cart.map(item => `
            <div class="cart-item">
                <div class="cart-item-left">
                    <img src="${item.image}" alt="${item.name}" class="cart-item-img">
                    <div class="cart-item-info">
                        <div class="cart-item-name">${item.name}</div>
                        <div class="cart-item-price">${item.price.toLocaleString()} 원</div>
                    </div>
                </div>
                <div class="cart-item-right">
                    <div class="quantity-badge">수량 ${item.quantity}개</div>
                    <div class="cart-item-subtotal">${(item.price * item.quantity).toLocaleString()} 원</div>
                    <button class="btn-icon" onclick="removeFromCart(${item.menuId})">
                        <i class="ph ph-x"></i>
                    </button>
                </div>
            </div>
        `).join('');

        return `
            <div class="page-container">
                <h2 class="page-title">장바구니</h2>
                <div class="cart-layout">
                    <div class="cart-list">
                        ${cartHtml}
                    </div>
                    <div class="cart-summary">
                        <h3>주문 결제</h3>
                        <div class="summary-row">
                            <span>총 주문 금액</span>
                            <span class="summary-price">${total.toLocaleString()} 원</span>
                        </div>
                        <div class="summary-row">
                            <span>보유 포인트</span>
                            <span class="${userPoint < total ? 'text-error' : 'text-success'}">${userPoint.toLocaleString()} P</span>
                        </div>
                        <div class="summary-divider"></div>
                        <div class="summary-row total">
                            <span>최종 결제</span>
                            <span>${total.toLocaleString()} P</span>
                        </div>
                        <button class="btn-primary btn-block ${userPoint < total ? 'disabled' : ''}" onclick="handleCheckout()">
                            ${userPoint < total ? '포인트가 부족합니다' : '결제하기'}
                        </button>
                        ${userPoint < total ? `<a href="#/mypage" class="charge-link">포인트 충전하러 가기</a>` : ''}
                    </div>
                </div>
            </div>
        `;
    },

    orders: async () => {
        if (!isLoggedIn) {
            window.location.hash = '#/login';
            return '';
        }

        let ordersHtml = '';
        try {
            const resp = await window.api.orders.getMyOrders();
            const orders = resp?.content || [];

            if (orders.length === 0) {
                ordersHtml = `<div class="empty-state large"><i class="ph ph-receipt"></i><p>주문 내역이 없습니다.</p></div>`;
            } else {
                ordersHtml = orders.map(o => {
                    const name = o.extraCount > 0 ? `${o.representativeMenuName} 외 ${o.extraCount}건` : o.representativeMenuName;
                    return `
                    <div class="order-card order-card-link" onclick="window.location.hash='#/orders/${o.orderUid}'" style="cursor:pointer;">
                        <div class="order-header">
                            <div>
                                <span class="order-date">${new Date(o.createdAt).toLocaleDateString()}</span>
                                <span class="order-id">주문번호: ${o.orderUid}</span>
                            </div>
                            <span class="order-status">${o.orderStatus}</span>
                        </div>
                        <div class="order-body">
                            <div class="order-name">${name}</div>
                            <div style="display:flex;align-items:center;gap:1rem;">
                                <div class="order-price">${o.totalAmount.toLocaleString()} P</div>
                                <span style="font-size:0.85rem;color:var(--primary);font-weight:500;">상세 보기 <i class="ph ph-arrow-right"></i></span>
                            </div>
                        </div>
                    </div>
                `}).join('');
            }
        } catch (e) {
            ordersHtml = `<div class="empty-state">주문 내역을 불러오는데 실패했습니다.</div>`;
        }

        return `
            <div class="page-container">
                <h2 class="page-title">주문 내역</h2>
                <div class="orders-list">
                    ${ordersHtml}
                </div>
            </div>
        `;
    },

    admin: async () => {
        if (!isAdmin) {
            window.location.hash = '#/';
            return '';
        }

        let menusHtml = '';
        try {
            const menusResp = await window.api.menus.getAll();
            const menus = menusResp?.content || [];
            menusHtml = menus.map(m => `
                <div class="admin-menu-item" style="display:flex; justify-content:space-between; align-items:center; padding:1rem; background:var(--bg-card); border-radius:8px; margin-bottom:0.5rem; box-shadow:var(--shadow-sm);">
                    <div>
                        <div style="font-weight:600;">${m.menuName}</div>
                        <div class="text-muted" style="font-size:0.9rem;">${m.menuPrice.toLocaleString()} 원 | 재고: ${m.menuStock}</div>
                    </div>
                    <button class="btn-icon" onclick="window.handleDeleteMenu(${m.menuId})" title="메뉴 삭제"><i class="ph ph-trash" style="color:var(--danger);"></i></button>
                </div>
            `).join('');
        } catch(e) {
            menusHtml = `<div class="empty-state">메뉴를 불러오지 못했습니다.</div>`;
        }

        // 초기 주문 목록 로드
        let initialOrdersHtml = '';
        try {
            const ordersResp = await window.api.orders.getAdminOrders(0, 50);
            initialOrdersHtml = window.renderAdminOrders(ordersResp?.content || []);
        } catch(e) {
            initialOrdersHtml = `<div class="empty-state">주문 내역을 불러오지 못했습니다.</div>`;
        }

        return `
            <div class="page-container" style="max-width:1100px;">
                <h2 class="page-title text-error"><i class="ph-fill ph-shield-check"></i> 관리자 대시보드</h2>

                <!-- 메뉴 관리 -->
                <div class="admin-section-card" style="margin-bottom:2rem;">
                    <h3 class="admin-section-title"><i class="ph ph-coffee"></i> 메뉴 관리</h3>
                    <div style="display:grid; grid-template-columns:1fr 1fr; gap:2rem;">
                        <div style="background:var(--bg-card); padding:1.5rem; border-radius:12px; box-shadow:var(--shadow-sm);">
                            <h4 style="margin-bottom:1rem;">새 메뉴 추가</h4>
                            <form onsubmit="window.handleCreateMenu(event)">
                                <div class="form-group">
                                    <input type="text" id="newMenuName" class="form-input" placeholder="메뉴 이름" required>
                                </div>
                                <div style="display:grid; grid-template-columns:1fr 1fr; gap:1rem;">
                                    <div class="form-group">
                                        <input type="number" id="newMenuPrice" class="form-input" placeholder="가격 (원)" required>
                                    </div>
                                    <div class="form-group">
                                        <input type="number" id="newMenuStock" class="form-input" placeholder="초기 재고 (개)" required>
                                    </div>
                                </div>
                                <div class="form-group">
                                    <select id="newMenuCategory" class="form-input" required>
                                        <option value="" disabled selected>카테고리 선택</option>
                                        <option value="HOT_DRINK">HOT_DRINK</option>
                                        <option value="COLD_DRINK">COLD_DRINK</option>
                                        <option value="DESSERT">DESSERT</option>
                                        <option value="CUSTOM_DRINK">CUSTOM_DRINK</option>
                                        <option value="LIMITED_ITEM">LIMITED_ITEM</option>
                                    </select>
                                </div>
                                <button type="submit" class="btn-primary">메뉴 등록</button>
                            </form>
                        </div>
                        <div class="admin-menu-list" style="max-height:340px; overflow-y:auto;">
                            ${menusHtml || '<div class="empty-state">등록된 메뉴가 없습니다.</div>'}
                        </div>
                    </div>
                </div>

                <!-- 주문 조회 -->
                <div class="admin-section-card">
                    <h3 class="admin-section-title"><i class="ph ph-receipt"></i> 주문 조회</h3>

                    <!-- 검색 폼 -->
                    <div class="admin-search-form">
                        <div class="admin-search-grid">
                            <div class="admin-search-field">
                                <label>주문번호</label>
                                <input type="text" id="srchOrderUid" class="form-input" placeholder="ORD-...">
                            </div>
                            <div class="admin-search-field">
                                <label>유저 이메일</label>
                                <input type="email" id="srchEmail" class="form-input" placeholder="user@example.com">
                            </div>
                            <div class="admin-search-field">
                                <label>시작일</label>
                                <input type="date" id="srchStartDate" class="form-input">
                            </div>
                            <div class="admin-search-field">
                                <label>종료일</label>
                                <input type="date" id="srchEndDate" class="form-input">
                            </div>
                        </div>
                        <div class="admin-search-actions">
                            <button class="btn-search" onclick="window.handleAdminSearch()">
                                <i class="ph-bold ph-magnifying-glass"></i> 검색
                            </button>
                            <button class="btn-search-reset" onclick="window.handleAdminSearchReset()">
                                <i class="ph ph-arrow-counter-clockwise"></i> 초기화
                            </button>
                        </div>
                    </div>

                    <!-- 결과 목록 -->
                    <div id="admin-order-list" style="display:flex; flex-direction:column; gap:0.75rem; margin-top:1.5rem;">
                        ${initialOrdersHtml}
                    </div>
                </div>
            </div>
        `;
    },

    orderDetail: async (orderUid) => {
        if (!isLoggedIn) {
            window.location.hash = '#/login';
            return '';
        }
        if (!orderUid) {
            window.location.hash = '#/orders';
            return '';
        }

        let detail;
        try {
            detail = await window.api.orders.getOrderDetail(orderUid);
        } catch (e) {
            return `
                <div class="page-container">
                    <button class="btn-back" onclick="history.back()"><i class="ph ph-arrow-left"></i> 돌아가기</button>
                    <div class="empty-state" style="margin-top:2rem;"><i class="ph ph-warning"></i><p>주문 정보를 불러오는데 실패했습니다.</p></div>
                </div>
            `;
        }

        const menuRows = (detail.menuDetail || []).map(m => `
            <div class="detail-menu-row">
                <div class="detail-menu-left">
                    <img src="${getMenuImage(m.menuName)}" alt="${m.menuName}" class="detail-menu-img">
                    <div>
                        <div class="detail-menu-name">${m.menuName}</div>
                        <div class="detail-menu-unit">${m.menuPrice.toLocaleString()} 원 × ${m.quantity}개</div>
                    </div>
                </div>
                <div class="detail-menu-amount">${m.totalAmount.toLocaleString()} P</div>
            </div>
        `).join('');

        return `
            <div class="page-container" style="max-width:640px;">
                <button class="btn-back" onclick="history.back()"><i class="ph ph-arrow-left"></i> 돌아가기</button>

                <div class="order-detail-card">
                    <div class="order-detail-header">
                        <h2><i class="ph-fill ph-receipt"></i> 주문 상세</h2>
                        <div class="order-detail-uid">${detail.orderUid}</div>
                    </div>

                    <div class="detail-menu-list">
                        ${menuRows}
                    </div>

                    <div class="detail-total-row">
                        <span>총 결제 금액</span>
                        <strong class="detail-total-amount">${detail.totalAmount.toLocaleString()} P</strong>
                    </div>
                </div>
            </div>
        `;
    }
};

// --- HANDLERS ---
window.handleLogin = async (e) => {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;

    try {
        await window.api.auth.login({ email, password });
        window.showToast('로그인 성공!');
        await checkAuth();
        window.location.hash = '#/';
    } catch (err) {
        // Error handled in api.js
    }
};

window.handleSignup = async (e) => {
    e.preventDefault();
    const email = document.getElementById('signupEmail').value;
    const password = document.getElementById('signupPassword').value;

    try {
        await window.api.auth.signup({ email, password });
        window.showToast('회원가입이 완료되었습니다.');
        window.location.hash = '#/login';
    } catch (err) {
        // Error handled in api.js
    }
};

window.handleCharge = async () => {
    const amount = parseInt(document.getElementById('chargeAmount').value, 10);
    if (!amount || amount <= 0) {
        window.showToast('올바른 금액을 입력하세요.', 'error');
        return;
    }

    try {
        await window.api.points.charge(amount);
        window.showToast(`${amount.toLocaleString()}P 충전 완료!`);
        renderRoute();
    } catch (err) {
    }
};

window.handleCheckout = async () => {
    if (cart.length === 0) return;

    const total = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    if (userPoint < total) {
        window.showToast('포인트 잔액이 부족합니다.', 'error');
        return;
    }

    const items = cart.map(item => ({
        menuId: item.menuId,
        quantity: item.quantity
    }));

    try {
        await window.api.orders.create({ items });
        window.showToast('결제가 완료되었습니다!');
        cart = [];
        saveCart();
        window.location.hash = '#/orders';
    } catch (err) {
    }
};

window.handleCreateMenu = async (e) => {
    e.preventDefault();
    const menuName = document.getElementById('newMenuName').value;
    const menuPrice = parseInt(document.getElementById('newMenuPrice').value, 10);
    const menuStock = parseInt(document.getElementById('newMenuStock').value, 10);
    const category = document.getElementById('newMenuCategory').value;

    try {
        await window.api.menus.create({ menuName, menuPrice, menuStock, category });
        window.showToast('새 메뉴가 등록되었습니다.');
        renderRoute();
    } catch (err) { }
};

window.handleDeleteMenu = async (menuId) => {
    if (!confirm('정말 이 메뉴를 삭제하시겠습니까?')) return;
    try {
        await window.api.menus.delete(menuId);
        window.showToast('메뉴가 삭제되었습니다.');
        renderRoute();
    } catch (err) { }
};

// 관리자 주문 목록 렌더링 헬퍼 (검색 전·후 공용)
window.renderAdminOrders = (orders) => {
    if (!orders || orders.length === 0) {
        return `<div class="empty-state"><i class="ph ph-receipt"></i><p>해당하는 주문이 없습니다.</p></div>`;
    }
    return orders.map(o => `
        <div class="order-card order-card-link" style="padding:1rem; cursor:pointer;" onclick="window.location.hash='#/orders/${o.orderUid}'">
            <div style="display:flex; justify-content:space-between; margin-bottom:0.5rem;">
                <strong style="font-size:0.9rem; font-family:monospace;">${o.orderUid}</strong>
                <span class="order-status">${o.orderStatus}</span>
            </div>
            <div style="display:flex; justify-content:space-between; align-items:center;">
                <div>
                    <div class="text-muted" style="font-size:0.85rem;">${new Date(o.createdAt).toLocaleString()}</div>
                    ${o.email ? `<div style="font-size:0.83rem;color:var(--primary);font-weight:500;">${o.email}</div>` : ''}
                </div>
                <div style="display:flex;align-items:center;gap:0.8rem;">
                    <strong>${o.totalAmount.toLocaleString()} P</strong>
                    <span style="font-size:0.82rem;color:var(--text-muted);">상세 보기 <i class="ph ph-arrow-right"></i></span>
                </div>
            </div>
        </div>
    `).join('');
};

// 관리자 주문 검색
window.handleAdminSearch = async () => {
    const params = {
        orderUid:  document.getElementById('srchOrderUid')?.value.trim()  || '',
        email:     document.getElementById('srchEmail')?.value.trim()     || '',
        startDate: document.getElementById('srchStartDate')?.value        || '',
        endDate:   document.getElementById('srchEndDate')?.value          || ''
    };
    const listEl = document.getElementById('admin-order-list');
    if (!listEl) return;

    listEl.innerHTML = `<div style="text-align:center;padding:2rem;"><div class="spinner" style="margin:0 auto;"></div></div>`;
    try {
        const resp = await window.api.orders.getAdminOrders(0, 100, params);
        listEl.innerHTML = window.renderAdminOrders(resp?.content || []);
    } catch(e) {
        listEl.innerHTML = `<div class="empty-state">주문 조회에 실패했습니다.</div>`;
    }
};

// 검색 초기화
window.handleAdminSearchReset = async () => {
    const fields = ['srchOrderUid', 'srchEmail', 'srchStartDate', 'srchEndDate'];
    fields.forEach(id => { const el = document.getElementById(id); if (el) el.value = ''; });
    const listEl = document.getElementById('admin-order-list');
    if (!listEl) return;
    listEl.innerHTML = `<div style="text-align:center;padding:2rem;"><div class="spinner" style="margin:0 auto;"></div></div>`;
    try {
        const resp = await window.api.orders.getAdminOrders(0, 50);
        listEl.innerHTML = window.renderAdminOrders(resp?.content || []);
    } catch(e) {
        listEl.innerHTML = `<div class="empty-state">주문 조회에 실패했습니다.</div>`;
    }
};


// --- ROUTER ---
const appDiv = document.getElementById('app');

async function renderRoute() {
    const hash = window.location.hash || '#/';
    const route = hash.replace('#/', '') || 'home';

    // Dynamic route: orders/:orderUid
    if (route.startsWith('orders/')) {
        const orderUid = route.replace('orders/', '');
        appDiv.innerHTML = '<div class="loader-overlay active" style="position:relative; height: 300px; background: transparent;"><div class="spinner"></div></div>';
        try {
            const html = await views.orderDetail(orderUid);
            const currentRoute = (window.location.hash || '#/').replace('#/', '') || 'home';
            if (currentRoute === route) {
                appDiv.innerHTML = html;
            }
        } catch (e) {
            appDiv.innerHTML = `<div class="empty-state">화면을 렌더링하는 도중 오류가 발생했습니다.</div>`;
        }
        renderNav();
        return;
    }

    if (views[route]) {
        appDiv.innerHTML = '<div class="loader-overlay active" style="position:relative; height: 300px; background: transparent;"><div class="spinner"></div></div>';
        try {
            const html = await views[route]();
            // Check if hash changed while loading
            const currentHash = window.location.hash || '#/';
            const currentRoute = currentHash.replace('#/', '') || 'home';
            if (currentRoute === route) {
                appDiv.innerHTML = html;
            }
        } catch (e) {
            appDiv.innerHTML = `<div class="empty-state">화면을 렌더링하는 도중 오류가 발생했습니다.</div>`;
        }
        renderNav();
    } else {
        appDiv.innerHTML = `<div class="empty-state">페이지를 찾을 수 없습니다.</div>`;
    }
}


window.addEventListener('hashchange', renderRoute);

// INITIALIZE
document.addEventListener('DOMContentLoaded', async () => {
    // 백엔드 CustomErrorController가 404/403을 index.html로 포워딩한 경우
    if (window.location.pathname !== '/' && window.location.pathname !== '/index.html') {
        window.history.replaceState(null, '', '/#/');
        setTimeout(() => window.showToast('존재하지 않거나 권한이 없는 페이지입니다.', 'error'), 500);
    }

    await checkAuth();
    renderRoute();
});
