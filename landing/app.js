// StreamFolio Landing Page Interactive JS

// ----------------------------------------------------------------------
// WAITLIST GOOGLE SHEETS ENDPOINT CONFIGURATION
// Paste your Google Apps Script Web App URL below after completing setup
// ----------------------------------------------------------------------
const WAITLIST_ENDPOINT = 'https://script.google.com/macros/s/AKfycbzRdvpbEaRghnUtiW9-0j7L_BABCVRLvVFdQhILOqp6A_yG6DgEJai55NJCl4H971rJ/exec';

document.addEventListener('DOMContentLoaded', () => {
    // 1. Navbar Scroll Header Effect
    const navbar = document.getElementById('navbar');
    if (navbar) {
        window.addEventListener('scroll', () => {
            if (window.scrollY > 20) {
                navbar.classList.add('scrolled');
            } else {
                navbar.classList.remove('scrolled');
            }
        });
    }

    // 2. Mobile Slide-Out Drawer Navigation Toggle
    const hamburgerBtn = document.getElementById('hamburger-btn');
    const drawerCloseBtn = document.getElementById('drawer-close-btn');
    const mobileDrawer = document.getElementById('mobile-drawer');

    function openDrawer() {
        if (mobileDrawer) {
            mobileDrawer.classList.add('active');
            document.body.style.overflow = 'hidden';
        }
    }

    function closeDrawer() {
        if (mobileDrawer) {
            mobileDrawer.classList.remove('active');
            document.body.style.overflow = '';
        }
    }

    if (hamburgerBtn) {
        hamburgerBtn.addEventListener('click', openDrawer);
    }

    if (drawerCloseBtn) {
        drawerCloseBtn.addEventListener('click', closeDrawer);
    }

    if (mobileDrawer) {
        mobileDrawer.addEventListener('click', (e) => {
            if (e.target === mobileDrawer) {
                closeDrawer();
            }
        });

        // Close drawer when clicking any nav link
        const drawerLinks = mobileDrawer.querySelectorAll('.drawer-link');
        drawerLinks.forEach(link => {
            link.addEventListener('click', closeDrawer);
        });
    }

    // 3. Mobile Floating Sticky CTA Bar (Appears when scrolling past hero)
    const mobileStickyBar = document.getElementById('mobile-sticky-bar');
    const heroSection = document.querySelector('.hero');

    if (mobileStickyBar && heroSection) {
        window.addEventListener('scroll', () => {
            const heroBottom = heroSection.offsetTop + heroSection.offsetHeight - 200;
            if (window.scrollY > heroBottom && window.innerWidth <= 768) {
                mobileStickyBar.classList.add('visible');
            } else {
                mobileStickyBar.classList.remove('visible');
            }
        });
    }

    // 4. Smooth Scroll for "Join Play Waitlist" Links (Navbar offset + auto-focus)
    const waitlistScrollLinks = document.querySelectorAll('.waitlist-scroll-link, a[href="#waitlist-card"]');
    waitlistScrollLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const target = document.getElementById('waitlist-card');
            if (target) {
                const navbarHeight = document.getElementById('navbar')?.offsetHeight || 80;
                const elementPosition = target.getBoundingClientRect().top + window.pageYOffset;
                const offsetPosition = elementPosition - navbarHeight - 24;

                window.scrollTo({
                    top: Math.max(0, offsetPosition),
                    behavior: 'smooth'
                });

                // Auto focus email input field after smooth scroll completes
                setTimeout(() => {
                    const emailInput = document.getElementById('waitlist-email');
                    if (emailInput) {
                        emailInput.focus();
                    }
                }, 550);
            }
        });
    });

    // 5. Waitlist Form Submission Logic (Google Sheets + LocalStorage Fallback)
    const waitlistForm = document.getElementById('waitlist-form');
    const waitlistEmail = document.getElementById('waitlist-email');
    const waitlistBtn = document.getElementById('waitlist-btn');

    if (waitlistForm && waitlistEmail) {
        waitlistForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const email = waitlistEmail.value.trim();

            if (!email || !validateEmail(email)) {
                showToast('⚠️ Please enter a valid email address.', 'error');
                return;
            }

            // Save to localStorage backup
            const existingEmails = JSON.parse(localStorage.getItem('streamfolio_waitlist') || '[]');
            if (!existingEmails.includes(email)) {
                existingEmails.push(email);
                localStorage.setItem('streamfolio_waitlist', JSON.stringify(existingEmails));
            }

            // UI Loading state
            if (waitlistBtn) {
                waitlistBtn.disabled = true;
                waitlistBtn.textContent = 'Submitting...';
            }

            // Send POST request to Google Apps Script Web App (using text/plain content-type to avoid 403 CORS preflight)
            if (WAITLIST_ENDPOINT && WAITLIST_ENDPOINT !== 'YOUR_GOOGLE_APPS_SCRIPT_URL_HERE') {
                try {
                    const payload = JSON.stringify({ email: email, timestamp: new Date().toISOString() });
                    await fetch(WAITLIST_ENDPOINT, {
                        method: 'POST',
                        mode: 'no-cors',
                        headers: {
                            'Content-Type': 'text/plain;charset=utf-8'
                        },
                        body: payload
                    });
                } catch (err) {
                    console.warn('Google Sheets submission warning:', err);
                }
            }

            // UI Success Feedback
            if (waitlistBtn) {
                waitlistBtn.disabled = false;
                waitlistBtn.textContent = 'Joined!';
                waitlistBtn.style.opacity = '0.9';
            }
            showToast('🎉 Thank you! You\'re on the StreamFolio Play Store waitlist.', 'success');

            waitlistEmail.value = '';
            setTimeout(() => {
                if (waitlistBtn) {
                    waitlistBtn.textContent = 'Join Waitlist';
                    waitlistBtn.style.opacity = '1';
                }
            }, 3500);
        });
    }

    // 6. Smooth Scroll for Logo
    const logoLink = document.getElementById('logo-link');
    if (logoLink) {
        logoLink.addEventListener('click', (e) => {
            e.preventDefault();
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        });
    }
});

// Email Regex Validation Helper
function validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(String(email).toLowerCase());
}

// Toast Notification System
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = message;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(20px) scale(0.9)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => {
            toast.remove();
        }, 300);
    }, 4000);
}
