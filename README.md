# SriramMart – E-Commerce Platform (Buyer · Seller · Admin · AI Assistant)

A full-stack, enterprise-grade e-commerce web application built with **Java 17**, **Spring Boot 3**, **Spring Security**, **Thymeleaf**, and **JPA/Hibernate**, supporting **PostgreSQL (Render/Cloud)**, **H2 (Local/Zero-Config)**, and **MySQL**.

---

## 🚀 Live Render Deployment Guide (Step-by-Step)

Deploying **SriramMart** on [Render](https://render.com) with **PostgreSQL** takes less than 3 minutes.

### Option A: 1-Click Blueprint Deployment (Recommended)
1. Push this repository to your GitHub account.
2. In the Render Dashboard, click **New +** → **Blueprint**.
3. Select your repository. Render will automatically read `render.yaml`, provision the PostgreSQL database, and build the Docker container!

---

### Option B: Manual Web Service & PostgreSQL Setup

#### Step 1: Create a PostgreSQL Database on Render
1. Go to your Render Dashboard and click **New +** → **PostgreSQL**.
2. Fill in the database details:
   - **Name:** `srirammart-postgres`
   - **Database:** `srirammart`
   - **User:** `srirammart_user`
   - **Region:** Choose the region closest to you (e.g. *Oregon* or *Frankfurt*).
   - **Plan:** *Free*
3. Click **Create Database**.
4. Once created, copy the **Internal Database URL** (e.g., `postgres://srirammart_user:password@dpg-xxxx-a:5432/srirammart`).

#### Step 2: Create the Web Service
1. Click **New +** → **Web Service**.
2. Connect your GitHub repository (`sriram-mart`).
3. Set the following settings:
   - **Name:** `srirammart`
   - **Environment:** `Docker` (or choose `Native` with Java)
   - **Branch:** `main` (or `master`)
   - **Region:** Same region as your database!
   - **Plan:** *Free*
4. Under **Environment Variables**, add the following:

| Key | Value | Description |
|---|---|---|
| `PORT` | `8080` | Internal application port (Render routes web traffic here) |
| `DATABASE_URL` | *(Paste your Internal Database URL from Step 1)* | PostgreSQL connection string |
| `SPRING_PROFILES_ACTIVE` | `postgres` | Activates PostgreSQL profile |
| `SEED_DEMO_DATA` | `true` | Automatically seeds categories, products, demo sellers & shoppers |
| `ADMIN_USERNAME` | `admin` | Administrator login username |
| `ADMIN_PASSWORD` | `Admin@123` | Administrator login password |
| `REMEMBER_ME_KEY` | `SriramMartSecretKey2026` | Random secure string for session cookies |

5. Click **Deploy Web Service**!
6. Render will build the Docker container and start your website. Your application will be live at `https://srirammart.onrender.com`!

---

## 💻 Local Quick Start (Zero Setup)

Prerequisites: **JDK 17+**

```bash
# 1. Run with embedded H2 database (starts instantly, no database installation needed):
./mvnw spring-boot:run
# or on Windows:
mvnw.cmd spring-boot:run
```

Open **http://localhost:8080** in your browser.

### Local PostgreSQL Run
```bash
# If you have PostgreSQL running locally:
export SPRING_PROFILES_ACTIVE=postgres
export DB_USER=postgres
export DB_PASS=your_postgres_password
./mvnw spring-boot:run
```

---

## 🔑 Default Login Credentials

| Role | Username | Password | Dashboard URL | Capabilities |
|---|---|---|---|---|
| **Buyer (Demo)** | `sriram` | `Buyer@123` | `/` | Browse, Wishlist, Cart, Checkout, Order Tracking |
| **Seller** | `techhub` | `Seller@123` | `/seller` | Inventory, Add/Edit Products, Order Fulfilment |
| **Admin** | `admin` | `Admin@123` | `/admin` | Store Analytics, User Approval, Bulk CSV Import |

*Generated demo shoppers (`firstname.lastname`) use password `Demo@1234`.*

---

## 🛠️ Key Architectural Highlights & Fixes

1. **PostgreSQL Auto-Detection & Parsing**: Native support for Render `postgres://` connection strings with automatic JDBC translation and connection pooling.
2. **512MB RAM Optimization**: Dockerfile tuned with `-XX:MaxRAMPercentage=65.0 -XX:+UseSerialGC -Xss512k` preventing out-of-memory container restarts on free cloud tiers.
3. **Robust Seed Data**: Built-in 38 curated products across 9 categories with realistic specs, ratings, reviews, and images.
4. **Resilient AI Assistant**: Smart hybrid assistant with deterministic database fallbacks and optional external LLM rephrasing.
5. **Full E-Commerce Security**: BCrypt password hashing, CSRF protection, Content Security Policy, and Role-Based Access Control (RBAC).
