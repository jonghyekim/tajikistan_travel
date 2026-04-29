(function() {
    function hasSkeletonLayout() {
        return !!document.getElementById('loading-skeleton') && !!document.getElementById('main-content');
    }

    function footerSkeletonMarkup() {
        return `
            <div class="sk-footer">
                <div class="sk-footer-top">
                    <div class="skeleton-box sk-f-logo"></div>
                    <div class="skeleton-box sk-f-nav"></div>
                    <div class="skeleton-box sk-f-lang"></div>
                </div>
                <div class="skeleton-box sk-f-line"></div>
            </div>
        `;
    }

    function filterSkeletonMarkup() {
        return `
            <div class="sk-header">
                <div class="skeleton-box sk-logo"></div>
                <div class="skeleton-box sk-nav"></div>
                <div class="skeleton-box sk-lang-group"></div>
            </div>

            <div class="sk-list-body">
                <div class="sk-container" style="margin-top:120px;">
                    <div class="skeleton-box sk-title-sm"></div>

                    <div class="skeleton-box" style="height:100px; border-radius:28px; margin-top:18px;"></div>

                    <div class="skeleton-box" style="width:220px; height:26px; border-radius:8px; margin:22px 0 16px;"></div>

                    <div class="card-grid">
                        <div style="border-radius:28px; overflow:hidden; background:#fff;">
                            <div class="skeleton-box" style="aspect-ratio:16/9;"></div>
                            <div style="padding:18px;">
                                <div class="skeleton-box" style="height:20px; margin-bottom:10px;"></div>
                                <div class="skeleton-box" style="height:14px; margin-bottom:8px;"></div>
                                <div class="skeleton-box" style="height:14px; width:70%;"></div>
                            </div>
                        </div>
                        <div style="border-radius:28px; overflow:hidden; background:#fff;">
                            <div class="skeleton-box" style="aspect-ratio:16/9;"></div>
                            <div style="padding:18px;">
                                <div class="skeleton-box" style="height:20px; margin-bottom:10px;"></div>
                                <div class="skeleton-box" style="height:14px; margin-bottom:8px;"></div>
                                <div class="skeleton-box" style="height:14px; width:70%;"></div>
                            </div>
                        </div>
                        <div style="border-radius:28px; overflow:hidden; background:#fff;">
                            <div class="skeleton-box" style="aspect-ratio:16/9;"></div>
                            <div style="padding:18px;">
                                <div class="skeleton-box" style="height:20px; margin-bottom:10px;"></div>
                                <div class="skeleton-box" style="height:14px; margin-bottom:8px;"></div>
                                <div class="skeleton-box" style="height:14px; width:70%;"></div>
                            </div>
                        </div>
                        <div style="border-radius:28px; overflow:hidden; background:#fff;">
                            <div class="skeleton-box" style="aspect-ratio:16/9;"></div>
                            <div style="padding:18px;">
                                <div class="skeleton-box" style="height:20px; margin-bottom:10px;"></div>
                                <div class="skeleton-box" style="height:14px; margin-bottom:8px;"></div>
                                <div class="skeleton-box" style="height:14px; width:70%;"></div>
                            </div>
                        </div>
                        <div style="border-radius:28px; overflow:hidden; background:#fff;">
                            <div class="skeleton-box" style="aspect-ratio:16/9;"></div>
                            <div style="padding:18px;">
                                <div class="skeleton-box" style="height:20px; margin-bottom:10px;"></div>
                                <div class="skeleton-box" style="height:14px; margin-bottom:8px;"></div>
                                <div class="skeleton-box" style="height:14px; width:70%;"></div>
                            </div>
                        </div>
                        <div style="border-radius:28px; overflow:hidden; background:#fff;">
                            <div class="skeleton-box" style="aspect-ratio:16/9;"></div>
                            <div style="padding:18px;">
                                <div class="skeleton-box" style="height:20px; margin-bottom:10px;"></div>
                                <div class="skeleton-box" style="height:14px; margin-bottom:8px;"></div>
                                <div class="skeleton-box" style="height:14px; width:70%;"></div>
                            </div>
                        </div>
                        <div style="border-radius:28px; overflow:hidden; background:#fff;">
                            <div class="skeleton-box" style="aspect-ratio:16/9;"></div>
                            <div style="padding:18px;">
                                <div class="skeleton-box" style="height:20px; margin-bottom:10px;"></div>
                                <div class="skeleton-box" style="height:14px; margin-bottom:8px;"></div>
                                <div class="skeleton-box" style="height:14px; width:70%;"></div>
                            </div>
                        </div>
                        <div style="border-radius:28px; overflow:hidden; background:#fff;">
                            <div class="skeleton-box" style="aspect-ratio:16/9;"></div>
                            <div style="padding:18px;">
                                <div class="skeleton-box" style="height:20px; margin-bottom:10px;"></div>
                                <div class="skeleton-box" style="height:14px; margin-bottom:8px;"></div>
                                <div class="skeleton-box" style="height:14px; width:70%;"></div>
                            </div>
                        </div>
                        <div style="border-radius:28px; overflow:hidden; background:#fff;">
                            <div class="skeleton-box" style="aspect-ratio:16/9;"></div>
                            <div style="padding:18px;">
                                <div class="skeleton-box" style="height:20px; margin-bottom:10px;"></div>
                                <div class="skeleton-box" style="height:14px; margin-bottom:8px;"></div>
                                <div class="skeleton-box" style="height:14px; width:70%;"></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            ${footerSkeletonMarkup()}
        `;
    }

    function detailSkeletonMarkup() {
        return `
            <div class="sk-header">
                <div class="skeleton-box sk-logo"></div>
                <div class="skeleton-box sk-nav"></div>
                <div class="skeleton-box sk-lang-group"></div>
            </div>

            <div class="sk-detail-container">
                <div class="sk-detail-meta-row">
                    <div class="skeleton-box sk-chip sk-chip-long"></div>
                    <div class="skeleton-box sk-chip"></div>
                </div>

                <div class="sk-detail-title-row">
                    <div class="skeleton-box sk-back-btn"></div>
                    <div class="skeleton-box sk-detail-title"></div>
                </div>
            </div>

            <div class="sk-detail-hero-shell">
                <div class="skeleton-box sk-detail-hero"></div>
            </div>

            <div class="sk-detail-container">
                <div class="sk-detail-content-wrap">
                    <div class="sk-text-block">
                        <div class="skeleton-box sk-text-line is-wide"></div>
                        <div class="skeleton-box sk-text-line is-wide"></div>
                        <div class="skeleton-box sk-text-line is-mid"></div>
                        <div class="skeleton-box sk-text-line is-short"></div>
                    </div>
                </div>
            </div>

            ${footerSkeletonMarkup()}
        `;
    }

    function emergencySkeletonMarkup() {
        return `
            <div class="sk-header">
                <div class="skeleton-box sk-logo"></div>
                <div class="skeleton-box sk-nav"></div>
                <div class="skeleton-box sk-lang-group"></div>
            </div>

            <div class="sk-container" style="margin-top:120px;">
                <div class="skeleton-box sk-title-sm"></div>
                <div class="sk-contact-list">
                    <div class="sk-contact-card">
                        <div class="sk-contact-main">
                            <div class="skeleton-box sk-contact-title"></div>
                            <div class="skeleton-box sk-contact-desc"></div>
                        </div>
                        <div class="skeleton-box sk-contact-badge"></div>
                        <div class="skeleton-box sk-contact-action"></div>
                    </div>
                    <div class="sk-contact-card">
                        <div class="sk-contact-main">
                            <div class="skeleton-box sk-contact-title"></div>
                            <div class="skeleton-box sk-contact-desc"></div>
                        </div>
                        <div class="skeleton-box sk-contact-badge"></div>
                        <div class="skeleton-box sk-contact-action"></div>
                    </div>
                    <div class="sk-contact-card">
                        <div class="sk-contact-main">
                            <div class="skeleton-box sk-contact-title"></div>
                            <div class="skeleton-box sk-contact-desc"></div>
                        </div>
                        <div class="skeleton-box sk-contact-badge"></div>
                        <div class="skeleton-box sk-contact-action"></div>
                    </div>
                    <div class="sk-contact-card">
                        <div class="sk-contact-main">
                            <div class="skeleton-box sk-contact-title"></div>
                            <div class="skeleton-box sk-contact-desc"></div>
                        </div>
                        <div class="skeleton-box sk-contact-badge"></div>
                        <div class="skeleton-box sk-contact-action"></div>
                    </div>
                </div>
            </div>

            ${footerSkeletonMarkup()}
        `;
    }

    function getCurrentKind() {
        return document.body ? document.body.dataset.skeletonKind || null : null;
    }

    function resolveKind(url) {
        if (!url) {
            return null;
        }

        const pathname = url.pathname.replace(/\/+$/, '') || '/';
        if (pathname === '/filter') {
            return 'filter';
        }
        if (pathname.startsWith('/detail/')) {
            return 'detail';
        }
        
        if (pathname === '/') {
            return 'home';
        }
        return null;
    }

    function applyKind(kind) {
        const skeleton = document.getElementById('loading-skeleton');
        if (!skeleton || !kind || skeleton.dataset.kind === kind) {
            return;
        }

        if (kind === 'filter') {
            skeleton.innerHTML = filterSkeletonMarkup();
        } else if (kind === 'detail') {
            skeleton.innerHTML = detailSkeletonMarkup();
        } 

        skeleton.dataset.kind = kind;
    }

    function showSkeleton(kind) {
        if (!hasSkeletonLayout() || !document.body) {
            return;
        }

        applyKind(kind || getCurrentKind());

        document.body.classList.add('skeleton-pending');
        document.body.classList.remove('skeleton-ready');
    }

    function revealContent() {
        const skeleton = document.getElementById('loading-skeleton');
        const mainContent = document.getElementById('main-content');

        if (!skeleton || !mainContent || !document.body) {
            return;
        }

        document.body.classList.remove('skeleton-pending');
        document.body.classList.add('skeleton-ready');
    }

    function navigateWithSkeleton(url, kind) {
        showSkeleton(kind);
        window.requestAnimationFrame(function() {
            window.location.href = url.href;
        });
    }

    function bindNavigationSkeleton() {
        if (!hasSkeletonLayout()) {
            return;
        }

        document.addEventListener('click', function(event) {
            if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
                return;
            }

            const link = event.target.closest('a[href]');
            if (!link || link.target === '_blank' || link.hasAttribute('download')) {
                return;
            }

            const rawHref = link.getAttribute('href');
            if (!rawHref || rawHref.startsWith('#')) {
                return;
            }

            if (rawHref.startsWith('javascript:history.back()')) {
                event.preventDefault();
                showSkeleton(resolveKind(document.referrer ? new URL(document.referrer) : null));
                window.requestAnimationFrame(function() {
                    window.history.back();
                });
                return;
            }

            const url = new URL(link.href, window.location.href);
            if (url.origin !== window.location.origin || !/^https?:$/.test(url.protocol)) {
                return;
            }

			const nextKind = resolveKind(url);

			if (!nextKind) {
			    return;
			}

			event.preventDefault();
			navigateWithSkeleton(url, nextKind);
			
        }, true);

        document.addEventListener('submit', function(event) {
            if (event.defaultPrevented) {
                return;
            }

            const form = event.target;
            if (!(form instanceof HTMLFormElement) || form.target === '_blank') {
                return;
            }

            const method = (form.getAttribute('method') || 'get').toLowerCase();
            if (method !== 'get') {
                return;
            }

            const action = form.getAttribute('action') || window.location.href;
            const url = new URL(action, window.location.href);
            if (url.origin !== window.location.origin) {
                return;
            }

            const formData = new FormData(form);
            url.search = new URLSearchParams(formData).toString();

            event.preventDefault();
            showSkeleton(resolveKind(url));
            window.requestAnimationFrame(function() {
                form.submit();
            });
        }, true);
    }

    window.PageSkeleton = {
        show: showSkeleton,
        reveal: revealContent,
    };

    bindNavigationSkeleton();

    if (document.readyState === 'complete') {
        revealContent();
        return;
    }

    window.addEventListener('load', revealContent, { once: true });
})();
