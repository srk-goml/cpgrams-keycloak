# CPGrams Keycloak Google OAuth2 Integration

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Keycloak](https://img.shields.io/badge/Keycloak-22.x-red.svg)](https://www.keycloak.org/)
[![Google OAuth2](https://img.shields.io/badge/Google%20OAuth2-Enabled-green.svg)](https://developers.google.com/identity/protocols/oauth2)

A comprehensive authentication solution for CPGrams application integrating Keycloak Identity and Access Management (IAM) with Google OAuth2 social login capabilities.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Google OAuth2 Setup](#google-oauth2-setup)
- [Keycloak Configuration](#keycloak-configuration)
- [Usage](#usage)
- [API Endpoints](#api-endpoints)
- [Security](#security)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Overview

This project implements a robust authentication system for the CPGrams (Centralized Public Grievance Redress and Monitoring System) application using Keycloak as the Identity Provider (IdP) with Google OAuth2 integration for seamless social login functionality.

The integration enables users to authenticate using their Google accounts while maintaining centralized user management through Keycloak, providing a secure and user-friendly authentication experience.

## Features

- 🔐 **Single Sign-On (SSO)** - Centralized authentication across applications
- 🌐 **Google OAuth2 Integration** - Login with Google accounts
- 👥 **User Management** - Comprehensive user lifecycle management
- 🛡️ **Role-Based Access Control (RBAC)** - Fine-grained permission system
- 🔄 **Token Management** - JWT token generation and validation
- 📱 **Multi-Factor Authentication (MFA)** - Enhanced security options
- 🎨 **Customizable UI** - Branded login and registration pages
- 📊 **Audit Logging** - Comprehensive authentication logs
- 🔒 **Session Management** - Secure session handling

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│                 │    │                 │    │                 │
│   CPGrams UI    │◄──►│    Keycloak     │◄──►│  Google OAuth2  │
│   Application   │    │   Server        │    │   Provider      │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐    ┌─────────────────┐
│                 │    │                 │
│   CPGrams API   │    │   PostgreSQL    │
│   Backend       │    │   Database      │
│                 │    │                 │
└─────────────────┘    └─────────────────┘
```

## Prerequisites

Before setting up the project, ensure you have the following installed:

- **Docker & Docker Compose** (v20.10+)
- **Node.js** (v16+ LTS)
- **Java** (JDK 11+)
- **PostgreSQL** (v13+)
- **Google Cloud Console** account for OAuth2 setup

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/srk-goml/cpgrams-keycloak.git
cd cpgrams-keycloak
git checkout feature/auth-google
```

### 2. Environment Setup

Copy the environment template and configure the variables:

```bash
cp .env.example .env
```

Update the `.env` file with your specific configurations:

```env
# Keycloak Configuration
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=your-admin-password
KEYCLOAK_DB_PASSWORD=your-db-password
KEYCLOAK_HOSTNAME=localhost:8080

# Google OAuth2 Configuration
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=cpgrams_keycloak
DB_USER=keycloak
DB_PASSWORD=your-db-password

# Application Configuration
APP_PORT=3000
JWT_SECRET=your-jwt-secret
CORS_ORIGIN=http://localhost:3000
```

### 3. Start Services

Using Docker Compose:

```bash
docker-compose up -d
```

Or manually:

```bash
# Start PostgreSQL
docker run -d --name keycloak-db \
  -e POSTGRES_DB=cpgrams_keycloak \
  -e POSTGRES_USER=keycloak \
  -e POSTGRES_PASSWORD=your-db-password \
  -p 5432:5432 postgres:13

# Start Keycloak
docker run -d --name keycloak \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=your-admin-password \
  -e KC_DB=postgres \
  -e KC_DB_URL=jdbc:postgresql://localhost:5432/cpgrams_keycloak \
  -e KC_DB_USERNAME=keycloak \
  -e KC_DB_PASSWORD=your-db-password \
  -p 8080:8080 \
  quay.io/keycloak/keycloak:22.0 start-dev
```

## Configuration

### Google OAuth2 Setup

1. **Create Google Cloud Project**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select existing one

2. **Enable Google+ API**
   - Navigate to "APIs & Services" > "Library"
   - Search for "Google+ API" and enable it

3. **Create OAuth2 Credentials**
   - Go to "APIs & Services" > "Credentials"
   - Click "Create Credentials" > "OAuth client ID"
   - Select "Web application"
   - Add authorized redirect URIs:
     ```
     http://localhost:8080/realms/cpgrams/broker/google/endpoint
     https://your-domain.com/realms/cpgrams/broker/google/endpoint
     ```

4. **Configure Consent Screen**
   - Set up OAuth consent screen with application details
   - Add necessary scopes: `email`, `profile`, `openid`

### Keycloak Configuration

1. **Access Keycloak Admin Console**
   ```
   URL: http://localhost:8080
   Username: admin
   Password: [as configured in .env]
   ```

2. **Create Realm**
   - Create new realm named `cpgrams`
   - Configure realm settings as needed

3. **Configure Google Identity Provider**
   - Navigate to "Identity Providers" > "Add provider" > "Google"
   - Enter Google OAuth2 credentials:
     - Client ID: `[from Google Console]`
     - Client Secret: `[from Google Console]`
   - Set redirect URI: `http://localhost:8080/realms/cpgrams/broker/google/endpoint`

4. **Create Client Application**
   - Navigate to "Clients" > "Create client"
   - Configure client settings:
     - Client ID: `cpgrams-app`
     - Client Protocol: `openid-connect`
     - Access Type: `confidential`
     - Valid Redirect URIs: `http://localhost:3000/*`

5. **Configure Roles and Users**
   - Create roles: `admin`, `user`, `moderator`
   - Set up role mappings and permissions

### Google OAuth2 Login

Users can authenticate using Google by:
1. Clicking "Login with Google" button
2. Being redirected to Google OAuth2 consent screen
3. Granting permissions and being redirected back to application
4. Automatic user creation/update in Keycloak

## API Endpoints

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/auth/login` | Initiate login flow |
| GET | `/auth/callback` | Handle OAuth2 callback |
| POST | `/auth/token` | Exchange code for tokens |
| POST | `/auth/refresh` | Refresh access token |
| POST | `/auth/logout` | Logout user |

### User Management Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/user/profile` | Get user profile |
| PUT | `/api/user/profile` | Update user profile |
| GET | `/api/users` | List users (admin only) |
| DELETE | `/api/user/{id}` | Delete user (admin only) |

### Example API Usage

```javascript
// Get user profile
const profile = await fetch('/api/user/profile', {
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
}).then(res => res.json());

// Update user profile
const updateResponse = await fetch('/api/user/profile', {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@example.com'
  })
});
```

## Security

### Security Features

- **JWT Token Validation** - All API endpoints validate JWT tokens
- **CSRF Protection** - Cross-site request forgery protection
- **HTTPS Enforcement** - SSL/TLS encryption in production
- **Rate Limiting** - API rate limiting to prevent abuse
- **Input Validation** - Comprehensive input sanitization
- **Audit Logging** - Detailed security event logging

### Security Best Practices

1. **Environment Variables** - Store sensitive data in environment variables
2. **Token Expiration** - Configure appropriate token lifetimes
3. **Strong Passwords** - Enforce password complexity requirements
4. **Regular Updates** - Keep Keycloak and dependencies updated
5. **Network Security** - Use firewalls and VPNs for production

## Troubleshooting

### Common Issues

1. **Google OAuth2 Redirect URI Mismatch**
   ```
   Error: redirect_uri_mismatch
   Solution: Ensure redirect URIs in Google Console match Keycloak configuration
   ```

2. **Keycloak Database Connection Issues**
   ```bash
   # Check database connectivity
   docker logs keycloak
   
   # Verify database configuration
   docker exec -it keycloak-db psql -U keycloak -d cpgrams_keycloak
   ```

### Debugging

Enable debug logging in Keycloak:

```bash
# Add to Keycloak startup command
-e KC_LOG_LEVEL=DEBUG
```

Check application logs:

```bash
# Keycloak logs
docker logs keycloak

# Database logs
docker logs keycloak-db


```

## Acknowledgments

- [Keycloak](https://www.keycloak.org/) - Open Source Identity and Access Management
- [Google Identity Platform](https://developers.google.com/identity) - OAuth2 Provider

---
