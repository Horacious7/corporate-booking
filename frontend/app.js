/**
 * TechQuarter Corporate Booking - Frontend Application
 *
 * Vanilla JS single-page app for the booking & employee APIs.
 * Talks to the API Gateway endpoints.
 */

// ==================== Configuration ====================

// Production API URL (from SAM deploy output)
const API_BASE_URL = 'https://eoufh9djsk.execute-api.eu-central-1.amazonaws.com/Prod';

// For local development, uncomment below and comment out the line above:
// const API_BASE_URL = 'http://127.0.0.1:3000';

// ==================== Theme Toggle ====================

function initTheme() {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme) {
        document.documentElement.setAttribute('data-theme', savedTheme);
    } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
        document.documentElement.setAttribute('data-theme', 'dark');
    } else {
        document.documentElement.setAttribute('data-theme', 'light');
    }
    updateThemeIcon();
}

function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme');
    const next = current === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', next);
    localStorage.setItem('theme', next);
    updateThemeIcon();
}

function updateThemeIcon() {
    const theme = document.documentElement.getAttribute('data-theme');
    const iconEl = document.getElementById('themeIcon');
    const btn = document.getElementById('themeToggleBtn');

    if (theme === 'dark') {
        // Show sun icon in dark mode
        iconEl.innerHTML = `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 3V4M12 20V21M4 12H3M6.31412 6.31412L5.5 5.5M17.6859 6.31412L18.5 5.5M6.31412 17.69L5.5 18.5001M17.6859 17.69L18.5 18.5001M21 12H20M16 12C16 14.2091 14.2091 16 12 16C9.79086 16 8 14.2091 8 12C8 9.79086 9.79086 8 12 8C14.2091 8 16 9.79086 16 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>`;
        btn.title = 'Switch to light mode';
    } else {
        // Show moon icon in light mode
        iconEl.innerHTML = `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M13 6V3M18.5 12V7M14.5 4.5H11.5M21 9.5H16M15.5548 16.8151C16.7829 16.8151 17.9493 16.5506 19 16.0754C17.6867 18.9794 14.7642 21 11.3698 21C6.74731 21 3 17.2527 3 12.6302C3 9.23576 5.02061 6.31331 7.92462 5C7.44944 6.05072 7.18492 7.21708 7.18492 8.44523C7.18492 13.0678 10.9322 16.8151 15.5548 16.8151Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>`;
        btn.title = 'Switch to dark mode';
    }
}

// ==================== Init ====================

document.addEventListener('DOMContentLoaded', () => {
    // Initialize theme
    initTheme();

    // Theme toggle button
    document.getElementById('themeToggleBtn').addEventListener('click', toggleTheme);

    // Dynamic booking form based on resource type
    const resourceSelect = document.getElementById('bookResourceType');
    if (resourceSelect) {
        resourceSelect.addEventListener('change', updateBookingFormLabels);
    }
});

function updateBookingFormLabels() {
    const type = document.getElementById('bookResourceType').value;
    const labelDest = document.getElementById('labelDestination');
    const labelDep = document.getElementById('labelDeparture');
    const labelRet = document.getElementById('labelReturn');
    const labelTravelers = document.getElementById('labelTravelers');
    const labelPurpose = document.getElementById('labelPurpose');
    const returnInput = document.getElementById('bookReturn');
    const purposeInput = document.getElementById('bookPurpose');
    const returnGroup = document.getElementById('returnDateGroup');
    const destInput = document.getElementById('bookDestination');

    // Reset defaults
    returnGroup.style.display = '';
    returnInput.required = true;
    purposeInput.required = true;

    switch (type) {
        case 'FLIGHT':
            labelDest.textContent = 'Destination';
            destInput.placeholder = 'e.g. Cluj-Napoca';
            labelDep.textContent = 'Departure Date';
            labelRet.textContent = 'Return Date (optional)';
            returnInput.required = false;
            labelTravelers.textContent = 'Passengers';
            labelPurpose.textContent = 'Trip Purpose';
            purposeInput.placeholder = 'e.g. Client meeting in Cluj';
            break;
        case 'HOTEL':
            labelDest.textContent = 'Hotel Location';
            destInput.placeholder = 'e.g. Hilton Cluj-Napoca';
            labelDep.textContent = 'Check-in Date';
            labelRet.textContent = 'Check-out Date';
            labelTravelers.textContent = 'Guests';
            labelPurpose.textContent = 'Stay Purpose';
            purposeInput.placeholder = 'e.g. Business trip accommodation';
            break;
        case 'CAR_RENTAL':
            labelDest.textContent = 'Pickup Location';
            destInput.placeholder = 'e.g. Airport Cluj';
            labelDep.textContent = 'Pickup Date';
            labelRet.textContent = 'Return Date';
            labelTravelers.textContent = 'Drivers';
            labelPurpose.textContent = 'Trip Purpose';
            purposeInput.placeholder = 'e.g. Travel between offices';
            break;
        case 'CONFERENCE_ROOM':
            labelDest.textContent = 'Location';
            destInput.placeholder = 'e.g. Meeting Room A, Floor 3';
            labelDep.textContent = 'Start Date';
            labelRet.textContent = 'End Date';
            labelTravelers.textContent = 'Attendees';
            labelPurpose.textContent = 'Purpose (optional)';
            purposeInput.placeholder = 'e.g. Sprint planning meeting';
            purposeInput.required = false;
            break;
        default:
            labelDest.textContent = 'Destination';
            destInput.placeholder = 'e.g. Cluj-Napoca';
            labelDep.textContent = 'Departure Date';
            labelRet.textContent = 'Return Date';
            labelTravelers.textContent = 'Travelers';
            labelPurpose.textContent = 'Trip Purpose';
            purposeInput.placeholder = 'e.g. Client meeting in Cluj';
    }
}

// ==================== Tab Navigation ====================

document.querySelectorAll('.nav-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        // Remove active from all
        document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));

        // Activate selected
        btn.classList.add('active');
        document.getElementById(btn.dataset.tab).classList.add('active');
    });
});

// ==================== Employee Operations ====================

// Register Employee
document.getElementById('registerEmployeeForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const payload = {
        employeeId: document.getElementById('empId').value.trim(),
        name: document.getElementById('empName').value.trim(),
        email: document.getElementById('empEmail').value.trim(),
        department: document.getElementById('empDepartment').value,
        costCenterRef: document.getElementById('empCostCenter').value.trim()
    };

    showResult('registerEmployeeResult', 'info', '<span class="loading"></span> Registering employee...');

    try {
        const data = await apiCall('POST', '/employees', payload);

        if (data.status === 'SUCCESS') {
            showResult('registerEmployeeResult', 'success',
                `✅ Employee registered!<br>
                <strong>ID:</strong> ${data.employeeId}<br>
                <strong>Message:</strong> ${data.message}`);
            e.target.reset();
        } else {
            showResult('registerEmployeeResult', 'error',
                `❌ ${data.status}: ${data.message}`);
        }
    } catch (err) {
        showResult('registerEmployeeResult', 'error', `❌ Error: ${err.message}`);
    }
});

// Search Employee by ID
async function searchEmployeeById() {
    const id = document.getElementById('searchEmpId').value.trim();
    if (!id) return showResult('searchEmployeeResult', 'error', '⚠️ Enter an Employee ID');

    showResult('searchEmployeeResult', 'info', '<span class="loading"></span> Searching...');

    try {
        const data = await apiCall('GET', `/employees/${encodeURIComponent(id)}`);

        if (data.status === 'SUCCESS') {
            showResult('searchEmployeeResult', 'success', formatEmployee(data));
        } else {
            showResult('searchEmployeeResult', 'error', `❌ ${data.status}: ${data.message}`);
        }
    } catch (err) {
        showResult('searchEmployeeResult', 'error', `❌ Error: ${err.message}`);
    }
}

// Search Employees by Email
async function searchEmployeesByEmail() {
    const email = document.getElementById('searchEmpEmail').value.trim();
    if (!email) return showResult('searchEmployeeResult', 'error', '⚠️ Enter an email');

    showResult('searchEmployeeResult', 'info', '<span class="loading"></span> Searching...');

    try {
        const data = await apiCall('GET', `/employees?email=${encodeURIComponent(email)}`);

        if (Array.isArray(data)) {
            if (data.length === 0) {
                showResult('searchEmployeeResult', 'info', 'No employees found with that email.');
            } else {
                showResult('searchEmployeeResult', 'success', data.map(formatEmployee).join(''));
            }
        } else {
            showResult('searchEmployeeResult', 'error', `❌ ${data.status}: ${data.message}`);
        }
    } catch (err) {
        showResult('searchEmployeeResult', 'error', `❌ Error: ${err.message}`);
    }
}

// Search Employees by Department
async function searchEmployeesByDepartment() {
    const dept = document.getElementById('searchEmpDept').value;
    if (!dept) return showResult('searchEmployeeResult', 'error', '⚠️ Select a department');

    showResult('searchEmployeeResult', 'info', '<span class="loading"></span> Searching...');

    try {
        const data = await apiCall('GET', `/employees?department=${encodeURIComponent(dept)}`);

        if (Array.isArray(data)) {
            if (data.length === 0) {
                showResult('searchEmployeeResult', 'info', 'No employees found in that department.');
            } else {
                showResult('searchEmployeeResult', 'success', data.map(formatEmployee).join(''));
            }
        } else {
            showResult('searchEmployeeResult', 'error', `❌ ${data.status}: ${data.message}`);
        }
    } catch (err) {
        showResult('searchEmployeeResult', 'error', `❌ Error: ${err.message}`);
    }
}

// List All Employees
async function listAllEmployees() {
    showResult('searchEmployeeResult', 'info', '<span class="loading"></span> Loading all employees...');

    try {
        const data = await apiCall('GET', '/employees');

        if (Array.isArray(data)) {
            if (data.length === 0) {
                showResult('searchEmployeeResult', 'info', 'No employees registered yet.');
            } else {
                const summary = data.map(formatEmployeeSummary).join('');
                showResult('searchEmployeeResult', 'success',
                    `<strong>Total: ${data.length} employee(s)</strong>${summary}`);
            }
        } else {
            showResult('searchEmployeeResult', 'error', `❌ ${data.status}: ${data.message}`);
        }
    } catch (err) {
        showResult('searchEmployeeResult', 'error', `❌ Error: ${err.message}`);
    }
}

// Update Employee Status
async function updateEmployeeStatus() {
    const id = document.getElementById('statusEmpId').value.trim();
    const status = document.getElementById('empNewStatus').value;
    if (!id) return showResult('manageEmployeeResult', 'error', '⚠️ Enter an Employee ID');

    showResult('manageEmployeeResult', 'info', '<span class="loading"></span> Updating status...');

    try {
        const data = await apiCall('PATCH', `/employees/${encodeURIComponent(id)}/status`, { status });

        if (data.status === 'SUCCESS') {
            showResult('manageEmployeeResult', 'success', `✅ ${data.message}`);
        } else {
            showResult('manageEmployeeResult', 'error', `❌ ${data.status}: ${data.message}`);
        }
    } catch (err) {
        showResult('manageEmployeeResult', 'error', `❌ Error: ${err.message}`);
    }
}

// Delete Employee
async function deleteEmployee() {
    const id = document.getElementById('deleteEmpId').value.trim();
    if (!id) return showResult('manageEmployeeResult', 'error', '⚠️ Enter an Employee ID');

    if (!confirm(`Are you sure you want to delete employee ${id}?`)) return;

    showResult('manageEmployeeResult', 'info', '<span class="loading"></span> Deleting...');

    try {
        const data = await apiCall('DELETE', `/employees/${encodeURIComponent(id)}`);

        if (data.status === 'SUCCESS') {
            showResult('manageEmployeeResult', 'success', `✅ ${data.message}`);
        } else {
            showResult('manageEmployeeResult', 'error', `❌ ${data.status}: ${data.message}`);
        }
    } catch (err) {
        showResult('manageEmployeeResult', 'error', `❌ Error: ${err.message}`);
    }
}

// ==================== Booking Operations ====================

// Create Booking
document.getElementById('createBookingForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const rawDeparture = document.getElementById('bookDeparture').value;
    const rawReturn = document.getElementById('bookReturn').value;

    const payload = {
        employeeId: document.getElementById('bookEmpId').value.trim(),
        resourceType: document.getElementById('bookResourceType').value,
        destination: document.getElementById('bookDestination').value.trim(),
        departureDate: rawDeparture ? rawDeparture + ' 08:00:00' : '',
        returnDate: rawReturn ? rawReturn + ' 18:00:00' : '',
        travelerCount: parseInt(document.getElementById('bookTravelers').value),
        costCenterRef: document.getElementById('bookCostCenter').value.trim(),
        tripPurpose: document.getElementById('bookPurpose').value.trim()
    };

    showResult('createBookingResult', 'info', '<span class="loading"></span> Creating booking...');

    try {
        const data = await apiCall('POST', '/bookings', payload);

        if (data.status === 'SUCCESS') {
            showResult('createBookingResult', 'success',
                `✅ Booking created!<br>
                <strong>Reference:</strong> ${data.bookingReferenceId}<br>
                <strong>Message:</strong> ${data.message}`);
            e.target.reset();
        } else {
            showResult('createBookingResult', 'error', `❌ ${data.status}: ${data.message}`);
        }
    } catch (err) {
        showResult('createBookingResult', 'error', `❌ Error: ${err.message}`);
    }
});

// Search Booking by Reference ID
async function searchBookingByRef() {
    const ref = document.getElementById('searchBookingRef').value.trim();
    if (!ref) return showResult('searchBookingResult', 'error', '⚠️ Enter a booking reference ID');

    showResult('searchBookingResult', 'info', '<span class="loading"></span> Searching...');

    try {
        const data = await apiCall('GET', `/bookings/${encodeURIComponent(ref)}`);

        if (data.status === 'SUCCESS') {
            showResult('searchBookingResult', 'success', formatBooking(data));
        } else {
            showResult('searchBookingResult', 'error', `❌ ${data.status}: ${data.message}`);
        }
    } catch (err) {
        showResult('searchBookingResult', 'error', `❌ Error: ${err.message}`);
    }
}

// Search Bookings by Employee ID
async function searchBookingsByEmployee() {
    const empId = document.getElementById('searchBookingEmpId').value.trim();
    if (!empId) return showResult('searchBookingResult', 'error', '⚠️ Enter an Employee ID');

    showResult('searchBookingResult', 'info', '<span class="loading"></span> Searching...');

    try {
        const data = await apiCall('GET', `/bookings?employeeId=${encodeURIComponent(empId)}`);

        if (Array.isArray(data)) {
            if (data.length === 0) {
                showResult('searchBookingResult', 'info', 'No bookings found for this employee.');
            } else {
                showResult('searchBookingResult', 'success', data.map(formatBooking).join(''));
            }
        } else {
            showResult('searchBookingResult', 'error', `❌ ${data.status}: ${data.message}`);
        }
    } catch (err) {
        showResult('searchBookingResult', 'error', `❌ Error: ${err.message}`);
    }
}

// List All Bookings
async function listAllBookings() {
    showResult('searchBookingResult', 'info', '<span class="loading"></span> Loading all bookings...');

    try {
        const data = await apiCall('GET', '/bookings');

        if (Array.isArray(data)) {
            if (data.length === 0) {
                showResult('searchBookingResult', 'info', 'No bookings created yet.');
            } else {
                const summary = data.map(formatBookingSummary).join('');
                showResult('searchBookingResult', 'success',
                    `<strong>Total: ${data.length} booking(s)</strong>${summary}`);
            }
        } else {
            showResult('searchBookingResult', 'error', `❌ ${data.status}: ${data.message}`);
        }
    } catch (err) {
        showResult('searchBookingResult', 'error', `❌ Error: ${err.message}`);
    }
}

// Update Booking Status
async function updateBookingStatus() {
    const ref = document.getElementById('updateBookingRef').value.trim();
    const status = document.getElementById('bookNewStatus').value;
    if (!ref) return showResult('updateBookingResult', 'error', '⚠️ Enter a booking reference ID');

    showResult('updateBookingResult', 'info', '<span class="loading"></span> Updating status...');

    try {
        const data = await apiCall('PATCH', `/bookings/${encodeURIComponent(ref)}/status`, { status });

        if (data.status === 'SUCCESS') {
            showResult('updateBookingResult', 'success', `${SVG_OK} ${data.message}`);
        } else {
            showResult('updateBookingResult', 'error', `${SVG_ERR} ${data.status}: ${data.message}`);
        }
    } catch (err) {
        showResult('updateBookingResult', 'error', `${SVG_ERR} Error: ${err.message}`);
    }
}

// ==================== API Helper ====================

async function apiCall(method, path, body = null) {
    const url = `${API_BASE_URL}${path}`;
    const options = {
        method,
        headers: { 'Content-Type': 'application/json' },
    };

    if (body && (method === 'POST' || method === 'PATCH' || method === 'PUT')) {
        options.body = JSON.stringify(body);
    }

    console.log(`[API] ${method} ${url}`, body || '');

    const response = await fetch(url, options);
    const data = await response.json();

    console.log(`[API] Response (${response.status}):`, data);
    return data;
}

const SVG_OK   = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" style="vertical-align:middle;margin-right:4px"><path d="M20 6L9 17L4 12" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
const SVG_ERR  = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" style="vertical-align:middle;margin-right:4px"><path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
const SVG_WARN = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" style="vertical-align:middle;margin-right:4px"><path d="M12 9V13M12 17H12.01M10.29 3.86L1.82 18C1.64 18.3 1.55 18.65 1.56 19C1.56 19.55 1.81 20.08 2.24 20.44C2.66 20.8 3.22 21 3.8 21H20.2C20.78 21 21.34 20.8 21.76 20.44C22.19 20.08 22.44 19.55 22.44 19C22.45 18.65 22.36 18.3 22.18 18L13.71 3.86C13.52 3.52 13.23 3.25 12.88 3.07C12.53 2.9 12.14 2.82 11.76 2.86C11.22 2.92 10.72 3.25 10.29 3.86Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

const ICONS = {
    check: `<svg width="12" height="12" viewBox="0 0 24 24" fill="none"><path d="M20 6L9 17L4 12" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
    x:     `<svg width="12" height="12" viewBox="0 0 24 24" fill="none"><path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
    pause: `<svg width="12" height="12" viewBox="0 0 24 24" fill="none"><path d="M10 9V15M14 9V15" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/></svg>`,
    clock: `<svg width="12" height="12" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/><path d="M12 7V12L15 15" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>`,
};

function statusIcon(status) {
    switch ((status || '').toUpperCase()) {
        case 'ACTIVE':
        case 'CONFIRMED':
        case 'COMPLETED':
            return ICONS.check;
        case 'INACTIVE':
        case 'CANCELLED':
            return ICONS.x;
        case 'SUSPENDED':
            return ICONS.pause;
        case 'PENDING':
            return ICONS.clock;
        default:
            return ICONS.check;
    }
}

function statusClass(status) {
    switch ((status || '').toUpperCase()) {
        case 'ACTIVE':
        case 'CONFIRMED':
        case 'COMPLETED':
            return 'success';
        case 'INACTIVE':
        case 'CANCELLED':
            return 'error';
        case 'SUSPENDED':
        case 'PENDING':
            return 'warning';
        default:
            return 'info';
    }
}

function formatEmployee(emp) {
    if (emp.status !== 'SUCCESS') {
        return `
        <div class="result-item">
            <span class="status-badge error">${ICONS.x} ${emp.status}</span>
            <br><span class="label">Message:</span> <span class="value">${emp.message || 'N/A'}</span>
        </div>`;
    }

    const cls = statusClass(emp.employeeStatus);
    const icon = statusIcon(emp.employeeStatus);
    return `
        <div class="result-item">
            <span class="status-badge ${cls}">${icon} ${emp.employeeStatus || 'N/A'}</span>
            <br><span class="label">Employee ID:</span> <span class="value">${emp.employeeId || 'N/A'}</span>
            <br><span class="label">Name:</span> <span class="value">${emp.name || 'N/A'}</span>
            <br><span class="label">Email:</span> <span class="value">${emp.email || 'N/A'}</span>
            <br><span class="label">Department:</span> <span class="value">${emp.department || 'N/A'}</span>
            <br><span class="label">Cost Center:</span> <span class="value">${emp.costCenterRef || 'N/A'}</span>
        </div>`;
}

function formatEmployeeSummary(emp) {
    const cls = statusClass(emp.employeeStatus);
    const icon = statusIcon(emp.employeeStatus);
    return `
        <div class="result-item" style="padding: 6px 10px;">
            <span class="status-badge ${cls}" style="font-size:0.7em">${icon} ${emp.employeeStatus || '?'}</span>
            <strong>${emp.employeeId}</strong> — ${emp.name || 'N/A'}
            <span style="color:var(--text-light); margin-left:8px;">${emp.department || ''}</span>
        </div>`;
}

function formatBooking(booking) {
    if (booking.status !== 'SUCCESS') {
        return `
        <div class="result-item">
            <span class="status-badge error">${ICONS.x} ${booking.status}</span>
            <br><span class="label">Message:</span> <span class="value">${booking.message || 'N/A'}</span>
        </div>`;
    }
    return `
        <div class="result-item">
            <span class="status-badge success">${ICONS.check}</span>
            <br><span class="label">Reference:</span> <span class="value">${booking.bookingReferenceId || 'N/A'}</span>
            <br><span class="label">Details:</span> <span class="value">${booking.message || 'N/A'}</span>
        </div>`;
}

function formatBookingSummary(booking) {
    const cls = statusClass(booking.bookingStatus);
    const icon = statusIcon(booking.bookingStatus);
    return `
        <div class="result-item" style="padding: 6px 10px;">
            ${booking.bookingStatus ? `<span class="status-badge ${cls}" style="font-size:0.7em">${icon} ${booking.bookingStatus}</span>` : ''}
            <strong>${booking.bookingReferenceId}</strong>
            <span style="color:var(--text-light); margin-left:8px;">${booking.message || ''}</span>
        </div>`;
}

// ==================== UI Helpers ====================

function showResult(elementId, type, html) {
    const el = document.getElementById(elementId);
    el.className = `result-box ${type}`;
    el.innerHTML = html;
}
