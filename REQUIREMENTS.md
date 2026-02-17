# TechQuarter Corporate Booking Tool Assessment

## 1. Project Overview
TechQuarter is building a Corporate Booking Platform to improve operational efficiency. The goal is to build a **Workflow Service** that handles high-volume booking requests.

## 2. Core Technical Requirements
* **Goal:** Build a backend service and a frontend interface.
* **Performance:** The system must handle a peak traffic of **100 transactions per second (TPS)**.
* **Solution:** the solution should utilize **AWS Lambda (Serverless)** architecture for auto-scaling and cost-efficiency.
* **Tech Stack:**
 * **Backend:** Java on AWS.
 * **Frontend:** React Native (or a web UI with mobile structure in mind).
 * **Architecture:** Clear separation of concerns (View vs Controller).

## 3. Functional Requirements
The Workflow Service must expose an endpoint to consume and process the following request types:
1.  **Register Employee:** Initial setup for a new user.
2.  **Search for Booking Options:** Flight or hotel based on criteria.
3.  **Create New Booking:** Reserving a flight or hotel.
4.  **Appointment in Hotel Booking:** Specific appointment logic.

## 4. System Architecture Components
1.  **Message Consumption:** A REST endpoint to receive workflow requests.
2.  **Message Processor:** Logic to validate and process the incoming messages.
3.  **Message Frontend:** A UI to render the output (e.g., booking list, calendar view, map).

## 5. Engineering Standards & Evaluation
* **Code Quality:** Clean, readable, reusable, secure, and maintainable code.
* **OOP:** Strong command of Object-Oriented Programming and Design Patterns.
* **Testing:** Provide sample unit/integration tests and describe the testing strategy.
* **Automation (DevOps):** The solution must support rapid build, test, and deploy cycles (CI/CD) and monitoring for an Enterprise environment.

## 6. Deliverables
1.  **Public Endpoint:** URL to POST messages to.
2.  **Frontend URL:** URL to view the output.
3.  **Source Code:** GitHub repository with a README.md explaining the approach.
4.  **AI Strategy Document:** A short outline (max 500 words) on how AI tools were used to accelerate Backend, Infrastructure as Code (IaC), and UI development.

## 7. Data Model (JSON Contract)
The application must accept POST requests. Here is the strict JSON structure for a "Create New Booking" request that must be modeled in Java:
```json
{
 "employeeId": "EMP9876",
 "resourceType": "Flight",
 "destination": "NYC",
 "departureDate": "2024-11-05 08:00:00",
 "returnDate": "2024-11-08 18:00:00",
 "travelerCount": 1,
 "costCenterRef": "CC-456",
 "tripPurpose": "Client meeting - Acme Corp"
}
```