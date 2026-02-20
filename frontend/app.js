/**
 * TechQuarter Corporate Booking - Frontend Application
 *
 * Vanilla JS single-page app for testing the booking & employee APIs.
 * Talks to the API Gateway (or SAM local) endpoints.
 */

// ==================== Configuration ====================

const DEFAULT_LOCAL_URL = 'http://127.0.0.1:3000';
let API_BASE_URL = localStorage.getItem('apiBaseUrl') || '';

// Populate the settings input on load
document.addEventListener('DOMContentLoaded', () => {
    const apiInput = document.getElementById('apiBaseUrl');
    if (apiInput) apiInput.value = API_BASE_URL;
});

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

// ==================== Settings ====================

function saveApiUrl() {
    const url = document.getElementById('apiBaseUrl').value.trim().replace(/\/+$/, '');
    API_BASE_URL = url;
    localStorage.setItem('apiBaseUrl', url);
    showResult('settingsResult', 'success', `✅ API URL saved: ${url || '(empty)'}`);
}

function useLocalUrl() {
    document.getElementById('apiBaseUrl').value = DEFAULT_LOCAL_URL;
    API_BASE_URL = DEFAULT_LOCAL_URL;
    localStorage.setItem('apiBaseUrl', DEFAULT_LOCAL_URL);
    showResult('settingsResult', 'success', `✅ Using local URL: ${DEFAULT_LOCAL_URL}`);
}

async function healthCheck() {
    const resultDiv = document.getElementById('healthCheckResult');
    showResult('healthCheckResult', 'info', '<span class="loading"></span> Testing connection...');

    try {
        // Try fetching employees with a dummy query - even a 400 means the API is reachable
        const response = await fetch(`${API_BASE_URL}/employees?department=test`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });

        if (response.ok || response.status === 400) {
            showResult('healthCheckResult', 'success', `✅ API is reachable! Status: ${response.status}`);
        } else {
            showResult('healthCheckResult', 'error', `⚠️ API responded with status: ${response.status}`);
        }
    } catch (err) {
        showResult('healthCheckResult', 'error', `❌ Connection failed: ${err.message}<br><small>Make sure the API URL is correct and CORS is enabled.</small>`);
    }
}

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
    const dept = document.getElementById('searchEmpDept').value.trim();
    if (!dept) return showResult('searchEmployeeResult', 'error', '⚠️ Enter a department');

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

    const payload = {
        employeeId: document.getElementById('bookEmpId').value.trim(),
        resourceType: document.getElementById('bookResourceType').value,
        destination: document.getElementById('bookDestination').value.trim(),
        departureDate: document.getElementById('bookDeparture').value,
        returnDate: document.getElementById('bookReturn').value,
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

// Update Booking Status
async function updateBookingStatus() {
    const ref = document.getElementById('updateBookingRef').value.trim();
    const status = document.getElementById('bookNewStatus').value;
    if (!ref) return showResult('updateBookingResult', 'error', '⚠️ Enter a booking reference ID');

    showResult('updateBookingResult', 'info', '<span class="loading"></span> Updating status...');

    try {
        const data = await apiCall('PATCH', `/bookings/${encodeURIComponent(ref)}/status`, { status });

        if (data.status === 'SUCCESS') {
            showResult('updateBookingResult', 'success', `✅ ${data.message}`);
        } else {
            showResult('updateBookingResult', 'error', `❌ ${data.status}: ${data.message}`);
        }
    } catch (err) {
        showResult('updateBookingResult', 'error', `❌ Error: ${err.message}`);
    }
}

// ==================== API Helper ====================

async function apiCall(method, path, body = null) {
    if (!API_BASE_URL) {
        throw new Error('API URL not configured. Go to Settings tab and set it.');
    }

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

// ==================== Formatting Helpers ====================

function formatEmployee(emp) {
    const statusClass = emp.status === 'SUCCESS' ? 'success' : (emp.status === 'NOT_FOUND' ? 'error' : 'info');
    return `
        <div class="result-item">
            <span class="status-badge ${statusClass}">${emp.status}</span>
            <br><span class="label">Employee ID:</span> <span class="value">${emp.employeeId || 'N/A'}</span>
            <br><span class="label">Details:</span> <span class="value">${emp.message || 'N/A'}</span>
        </div>`;
}

function formatBooking(booking) {
    const statusClass = booking.status === 'SUCCESS' ? 'success' : (booking.status === 'NOT_FOUND' ? 'error' : 'info');
    return `
        <div class="result-item">
            <span class="status-badge ${statusClass}">${booking.status}</span>
            <br><span class="label">Reference:</span> <span class="value">${booking.bookingReferenceId || 'N/A'}</span>
            <br><span class="label">Details:</span> <span class="value">${booking.message || 'N/A'}</span>
        </div>`;
}

// ==================== UI Helpers ====================

function showResult(elementId, type, html) {
    const el = document.getElementById(elementId);
    el.className = `result-box ${type}`;
    el.innerHTML = html;
}

