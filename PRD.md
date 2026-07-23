# 🛒 Project Context: B2C E-Commerce System Architecture & Specification

## 1. Project Overview
- **Project Name**: Spring Boot 기반 B2C e-Commerce Platform
- **Target Tech Stack**:
  - **Backend**: Java 17+, Spring Boot 3.x, Spring Data JPA, MyBatis, Spring Security
  - **Database**: PostgreSQL / MySQL
  - **Auth**: JWT (Access & Refresh Token), OAuth 2.0
  - **Infrastructure/CI/CD**: Docker, Jenkins/GitHub Actions
  - **Batch/Scheduling**: Spring Batch / @Scheduled

---

## 2. Core System Architecture & Data Flow

### System Layering
1. **Consumer Track (Web/App)**: Browsing, Auth, Cart, Checkout, Order Tracking, CS
2. **Admin Track (Backoffice)**: Dashboard, Product/Stock Management, Order Fulfillment, CS Resolution
3. **System & Database**: Core Domain APIs, Transactional DB, Scheduled Jobs
4. **External API Layer**: OAuth (Kakao/Naver/Google), PG (Toss/PortOne), Address (Kakao/Gov), Carrier Tracking API

---

## 3. Database Schema Specification (DDL Reference)

### 3.1. Domain: Auth & Users
- `users`: Primary user account.
  - Fields: `user_id` (PK), `email`, `password` (Nullable for OAuth), `name`, `phone_number` (Nullable for initial OAuth), `provider` (LOCAL/KAKAO/NAVER/GOOGLE), `provider_id`, `role` (USER/ADMIN), `status` (ACTIVE/NEED_INFO/SUSPENDED), `created_at`
- `user_addresses`: Delivery destination management.
  - Fields: `address_id` (PK), `user_id` (FK), `recipient_name`, `recipient_phone`, `zipcode`, `address_main`, `address_detail`, `is_default`

### 3.2. Domain: Products & Stock
- `products`: Product catalog.
  - Fields: `product_id` (PK), `name`, `price`, `description`, `status` (ON_SALE/OUT_OF_STOCK/HIDDEN), `created_at`
- `product_stocks`: Stock and temporary reservation tracking (Timeout/Concurrency Control).
  - Fields: `product_id` (PK, FK), `stock_quantity`, `reserved_quantity`, `updated_at`
  - *Rule*: $Available Stock = stock\_quantity - reserved\_quantity$

### 3.3. Domain: Orders & Items
- `orders`: Order master table.
  - Fields: `order_id` (PK), `order_number` (UNIQUE), `user_id` (FK), `total_amount`, `discount_amount`, `payment_amount`, `status` (PENDING_PAYMENT/PAYMENT_COMPLETED/PREPARING/CANCELLED), `created_at`
- `order_items`: Order item details.
  - Fields: `order_item_id` (PK), `order_id` (FK), `product_id` (FK), `order_price`, `quantity`

### 3.4. Domain: Payments & Deliveries
- `payments`: PG transaction validation & record.
  - Fields: `payment_id` (PK), `order_id` (FK, UNIQUE), `pg_provider`, `imp_uid` / `payment_key`, `requested_amount`, `paid_amount`, `status` (READY/PAID/FAILED/CANCELLED), `paid_at`
- `deliveries`: Courier Tracking & Status control.
  - Fields: `delivery_id` (PK), `order_id` (FK, UNIQUE), `courier_code`, `tracking_number`, `status` (READY/SHIPPED/IN_TRANSIT/DELIVERED/RETURN_REQUESTED), `shipped_at`, `delivered_at`

### 3.5. Domain: CS & Inquiries
- `inquiries`: Customer support & Chatbot ticketing.
  - Fields: `inquiry_id` (PK), `user_id` (FK), `order_id` (FK, Nullable), `category`, `title`, `content`, `answer`, `status` (WAITING/ANSWERED), `created_at`, `answered_at`

---

## 4. Key Business Logic & Sequence Rules (Critical Specifications)

### 🔴 Sequence 1: OAuth 2.0 & Mandatory Info Redirect
- When a user logs in via Social OAuth, verify if `phone_number` and default `address` exist.
- If missing, set user status to `NEED_INFO` and redirect to the **Additional Info Form** before allowing checkout.

### 🔴 Sequence 2: Silent Token Refresh
- Client includes JWT Access Token in HTTP `Authorization` Header.
- On `401 Expired` response, client interceptor triggers `/api/v1/auth/refresh` using HTTP-Only Refresh Cookie to issue a new Access Token seamlessly.

### 🔴 Sequence 3: Payment Amount Validation (Anti-Tampering)
- Upon PG Payment completion callback:
  1. Fetch `paid_amount` directly via PG Server-to-Server API.
  2. Compare `paid_amount` with `orders.payment_amount` stored in DB.
  3. **If mismatched**: Trigger immediate PG Cancellation API call and set order status to `CANCELLED`.

### 🔴 Sequence 4: Stock Reservation & Timeout Scheduler
- When entering the Checkout screen:
  - Increase `reserved_quantity` by $N$ for the requested items (Soft Lock).
- **Scheduler Task**: Every 1 minute, scan for orders in `PENDING_PAYMENT` state older than 10 minutes.
  - Release reserved stock: `reserved_quantity = reserved_quantity - N`.

### 🔴 Sequence 5: Order Cancellation vs Return Flow
- **Before Shipping (`PENDING_PAYMENT`, `PREPARING`)**:
  - User can instantly cancel the order $\rightarrow$ Auto-refund via PG API + Restore DB Stock.
- **After Shipping (`SHIPPED`, `IN_TRANSIT`, `DELIVERED`)**:
  - Direct cancellation disabled $\rightarrow$ Redirect to **Return/Exchange Request Flow** (`RETURN_REQUESTED`).

### 🔴 Sequence 6: Automated Courier Status Batch
- **Spring Batch / Scheduled Job**:
  - Periodically poll the Courier Tracking API for orders in `IN_TRANSIT`.
  - When status updates to `DELIVERED`:
    - Update `deliveries.status = 'DELIVERED'`, set `delivered_at`.
    - Trigger notification to customer for Purchase Confirmation & Review Prompt.

---

## 5. Development Guidelines for AI Assistant (Vibe Coding Instructions)

1. **Architecture**: Strictly follow Layered Architecture (`Controller` -> `Service` -> `Repository` -> `Entity`/`DTO`).
2. **DTO Isolation**: Do NOT expose JPA Entities directly in API responses. Always map to Request/Response DTOs.
3. **Concurrency Control**: Use Pessimistic Locks or Redis Locks on `product_stocks` updates during checkout to prevent race conditions.
4. **Exception Handling**: Implement `@RestControllerAdvice` to standardize API error responses (`code`, `message`, `errors`).
5. **RESTful Standards**: Follow standard HTTP methods (`POST` for creation, `PUT`/`PATCH` for updates, `DELETE` for soft deletes).