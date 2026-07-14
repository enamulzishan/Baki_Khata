# Baki Khata (বাকি খাতা) - Product Requirements Document (PRD)

## 1. Executive Summary

**Baki Khata** is a mobile application designed for small and medium-sized enterprise (SME) owners in Bangladesh, such as grocery store owners, pharmacies, hardware shops, and mobile retailers. The app solves the pervasive problem of tracking customer credit (due payments/baki). By moving away from physical paper ledgers, Baki Khata allows shopkeepers to easily record credit sales, track collections, send automated WhatsApp/SMS payment reminders, and generate actionable business reports. The application is built with an offline-first architecture to ensure seamless operation in low-connectivity areas, with robust cloud synchronization via Firebase.

## 2. Problem Statement

Small business owners in emerging markets heavily rely on offering credit to customers to maintain loyalty and sales volume. However, managing this credit using traditional paper notebooks (Khata) leads to several critical issues:
- **Loss of Data:** Physical ledgers can be lost, damaged, or stolen.
- **Calculation Errors:** Manual tallying of balances is prone to mistakes.
- **Delayed Collections:** Shopkeepers hesitate to ask for money directly or forget to follow up, leading to cash flow crunches.
- **Lack of Insights:** It is difficult to see a consolidated view of total market due, daily collections, or top defaulters.

## 3. Business Goals

- **Digitize Credit Management:** Transition 100% of a shopkeeper's credit tracking from paper to digital.
- **Improve Cash Flow:** Reduce the average payment recovery time for shopkeepers by at least 30% through automated reminders.
- **User Adoption:** Achieve a high retention rate by maintaining an extremely simple, localized (Bangla first), and fast user experience.
- **Reliability:** Ensure 100% data availability and zero data loss through robust offline-first functionality and cloud backups.

## 4. Target Users

The primary target audience consists of small business owners and shopkeepers in Bangladesh:
- Grocery Stores (Mudikhanar Dokan)
- Pharmacies
- Clothing / Garment Shops
- Mobile Phone / Accessories Shops
- Hardware / Electronics Stores
- Wholesalers selling on credit

## 5. User Personas

**Persona 1: Rahim (45), Grocery Store Owner**
- **Tech Savviness:** Low to Medium. Uses WhatsApp, Facebook, and YouTube on his smartphone.
- **Pain Point:** Has 100+ regular customers who buy on credit. Often forgets who owes how much and feels shy asking for money directly.
- **Goal:** Needs a simple app in Bangla to quickly note down dues and send polite WhatsApp reminders without awkward phone calls.

**Persona 2: Selim (30), Pharmacy Owner**
- **Tech Savviness:** High. Uses mobile banking apps (bKash) and POS systems.
- **Pain Point:** Paper notebooks get ruined quickly. Needs to generate monthly reports on total outstanding balances to plan his inventory purchases.
- **Goal:** Wants a secure, fast app that backs up data automatically and provides clear financial summaries.

## 6. User Stories

- As a shopkeeper, I want to add a customer's due amount quickly so that I don't keep other customers waiting.
- As a shopkeeper, I want to send a WhatsApp reminder with one tap so that I can collect dues faster.
- As a shopkeeper, I want to use the app in Bangla so that I can understand all the features easily.
- As a shopkeeper, I want the app to work offline so that I can record transactions even when the internet is down.
- As a shopkeeper, I want to see my total market due on the dashboard so I know my current financial standing.

---

## 7. Functional Requirements

### Authentication
- **Email & Password Login:** Standard login mechanism.
- **Signup:** Register with Shop Name, Email, and Password.
- **Forgot Password:** Email-based password reset flow.
- **Logout:** Securely clear local session data.

### Language
- **Supported Languages:** Bangla (Primary/Default) and English.
- **Language Switch:** Accessible from Settings and Login screen. Instant UI update without requiring app restart.

### Dashboard
- **Total Due:** Aggregate sum of all outstanding balances.
- **Today's Collection:** Total amount collected on the current date.
- **Today's New Due:** Total credit given on the current date.
- **Today's Transactions:** Quick list of recent activities.
- **Overdue Customers:** List of customers who have exceeded their promised payment date.
- **Recent Customers:** Quick access to frequently interacted customers.
- **Quick Actions:** FAB (Floating Action Button) to quickly add a transaction.

### Customer Management
- **Customer List:** Alphabetical or chronological list of all customers.
- **Search:** Real-time search by Customer Name or Phone Number.
- **Sorting:** By Name (A-Z), Highest Due, or Recent Activity.
- **Filtering:** Show All, Only Due, or Settled customers.
- **Customer Tags:** Bad Payer, Good Payer, VIP.
- **Customer Status:** Active or Inactive.

### Customer Details
- **Profile:** Name, Phone, Address, Photo (optional).
- **Transaction History:** Chronological list of all dues and payments for this specific customer.
- **Due Balance:** Prominent display of current outstanding amount.
- **Payment History:** Filterable view of collections only.
- **Notes:** Text field for specific terms or item details.
- **WhatsApp Reminder:** Deep link to WhatsApp pre-filled with a polite payment request message.
- **SMS Reminder:** Fallback native SMS intent with pre-filled message.
- **Call Button:** Native dialer intent.

### Transactions
- **Add Sale (New Due):** Record date, amount, items/notes.
- **Partial Payment:** Record collection less than total due, automatically calculating remaining balance.
- **Full Payment:** One-tap settlement of full due.
- **Edit:** Modify amount or note of a past transaction (requires PIN/confirmation).
- **Delete:** Remove a transaction with warning and audit trail.
- **Payment Methods:** Cash, bKash, Nagad, Bank Transfer.
- **Receipt Photo:** Option to attach an image of the physical bill or items.

### Reports
- **Timeframes:** Today, Weekly, Monthly, Yearly filters.
- **Charts:** Bar charts for Collections vs. New Dues.
- **Top Customers:** List of customers generating the most revenue or holding the highest debt.
- **Export PDF:** Generate a professional PDF statement for a customer or total business summary.
- **Export Excel:** CSV/Excel export for advanced accounting.

### Reminder System
- **WhatsApp:** Integration via `url_launcher` (e.g., `wa.me/phone_number?text=message`).
- **SMS:** Integration via native SMS intent.
- **Bulk Reminder:** Select multiple customers and trigger SMS/WhatsApp intent iteratively.
- **Custom Message:** Allow shopkeeper to edit the default reminder template in Settings.
- **Scheduled Reminder (Future):** Push notification to shopkeeper to remind a specific customer on a specific date.

### Notifications
- **Overdue Reminder:** Local push notification alerting shopkeeper of overdue accounts.
- **Collection Reminder:** Daily evening summary notification ("You collected X today").
- **Backup Reminder:** Alert if local data hasn't synced with the cloud for 3+ days.

### Settings
- **Shop Name & Logo:** Customizable business identity for PDF exports.
- **Language:** Toggle Bangla/English.
- **Currency:** Default BDT (৳), option to change.
- **Theme:** Light / Dark / System Default.
- **Notification Settings:** Toggle specific alerts.
- **Backup & Restore:** Manual trigger to sync with Firebase.
- **Logout:** Sign out of Firebase Auth.

### Security
- **Firebase Authentication:** Secure token management.
- **PIN Lock:** 4-digit in-app PIN required to open the app.
- **Biometric:** Fingerprint / Face Unlock fallback for PIN.
- **Secure Storage:** Encrypt auth tokens and PIN in local secure storage.

### Offline Support
- **Local Database:** Complete CRUD operations available without internet.
- **Auto Sync:** Background synchronization when internet connection is restored.

### Cloud Backup
- **Firebase Firestore:** NoSQL database syncing local data.
- **Cloud Storage:** Storing receipt photos and shop logos.

### Analytics (Internal to Shopkeeper)
- **Monthly Due Trend:** Line graph showing due growth/reduction.
- **Collection Trend:** Bar graph of daily/weekly collections.
- **Recovery Rate:** Percentage of dues collected vs given.

---

## 8. Non-functional Requirements

- **Fast Performance:** App must load in under 2 seconds. Smooth 60fps scrolling on mid-range Android devices.
- **Secure:** All API calls over HTTPS. Firestore security rules to strictly isolate tenant (shopkeeper) data.
- **Offline First:** 100% of core features (Add Customer, Add Transaction, View Dashboard) must work instantly without a network request.
- **Responsive UI:** Layouts must adapt to different screen sizes, including tablets (often used at shop counters).
- **Material Design 3:** Utilize standard, accessible, and intuitive UI components.
- **Accessibility:** High contrast text, scalable fonts, and readable Bangla typography.
- **Scalable Architecture:** Clean Architecture implementation allowing future expansion (e.g., Multi-shop).

---

## 9. Database Design (Firebase Firestore & Hive Local)

### Firestore Collections & Documents

**Collection: `users` (Shopkeepers)**
- Document ID: `uid` (Firebase Auth UID)
- Fields:
  - `shopName` (String)
  - `email` (String)
  - `phone` (String)
  - `logoUrl` (String, nullable)
  - `createdAt` (Timestamp)
  - `currency` (String)
  - `language` (String)

**Collection: `customers` (Sub-collection under `users/{uid}`)**
- Document ID: `customerId` (Auto-generated)
- Fields:
  - `name` (String)
  - `phone` (String)
  - `address` (String)
  - `totalDue` (Double)
  - `tags` (Array of Strings)
  - `status` (String: "active", "inactive")
  - `createdAt` (Timestamp)
  - `updatedAt` (Timestamp)
  - `synced` (Boolean)

**Collection: `transactions` (Sub-collection under `users/{uid}/customers/{customerId}`)**
- Document ID: `transactionId` (Auto-generated)
- Fields:
  - `type` (String: "due_given", "payment_received")
  - `amount` (Double)
  - `date` (Timestamp)
  - `note` (String)
  - `paymentMethod` (String: "cash", "bkash", etc.)
  - `receiptUrl` (String, nullable)
  - `createdAt` (Timestamp)
  - `synced` (Boolean)

### Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // User can only read and write their own document
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      // User can only access their own customers and transactions
      match /customers/{customerId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
        
        match /transactions/{transactionId} {
          allow read, write: if request.auth != null && request.auth.uid == userId;
        }
      }
    }
  }
}
```

### Hive Local Database Models (TypeAdapters)
- `CustomerBox`: Stores Customer objects. Index by `customerId`.
- `TransactionBox`: Stores Transaction objects. Index by `transactionId`.
- `MetadataBox`: Stores Dashboard aggregates to prevent calculating total due from scratch on every load.

---

## 10. API Structure (Firebase Services)

Since this is a Firebase-backed app, "APIs" are direct SDK calls wrapped in repository classes.

- **AuthService:**
  - `signInWithEmail(email, password)`
  - `signUp(email, password, shopName)`
  - `signOut()`
  - `resetPassword(email)`
- **CustomerService:**
  - `createCustomer(Customer)`
  - `updateCustomer(Customer)`
  - `deleteCustomer(customerId)`
  - `getCustomers()` -> Stream (Listens to Hive locally, syncs with Firestore in bg)
- **TransactionService:**
  - `addTransaction(customerId, Transaction)` -> Updates customer's `totalDue` atomically.
  - `deleteTransaction(customerId, transactionId)`
  - `getTransactions(customerId)`
- **SyncService:**
  - `syncUnpushedData()` -> Queries Hive for `synced == false`, pushes to Firestore via Batch Writes.
  - `pullCloudUpdates()` -> Fetches new data from Firestore based on `lastSyncTimestamp`.

---

## 11. App Navigation Flow

- **Splash Screen** -> Checks Auth State
- **Auth Flow:**
  - Login Screen <-> Signup Screen
  - Forgot Password Screen
- **App Lock (if enabled):**
  - PIN / Biometric Screen
- **Main Navigation (BottomNavBar):**
  1. **Home/Dashboard:** Summary metrics, Quick Actions.
  2. **Customers:** Searchable list of all customers.
  3. **Reports:** Charts and PDF exports.
  4. **Settings:** Preferences, Profile, Sync status.
- **Inner Screens:**
  - **Customer Details:** Accessed from Home or Customers list.
    - **Add Transaction Modal/Screen:** Accessed from Customer Details.
  - **Edit Profile:** Accessed from Settings.
  - **Security Settings:** Accessed from Settings.

---

## 12. Folder Structure (Clean Architecture - Flutter)

```text
lib/
├── core/                   # App-wide configurations and utilities
│   ├── constants/          # Colors, Strings, Assets
│   ├── errors/             # Failure models, Exceptions
│   ├── network/            # Network info checker
│   ├── theme/              # Light/Dark themes
│   ├── utils/              # Formatters, Validators
│   └── widgets/            # Reusable UI components (Buttons, Inputs)
├── data/                   # Data layer
│   ├── datasources/        # Local (Hive) and Remote (Firebase)
│   ├── models/             # DTOs, Hive models with TypeAdapters
│   └── repositories/       # Implementation of domain repositories
├── domain/                 # Domain layer (Business Logic)
│   ├── entities/           # Core business objects
│   ├── repositories/       # Interfaces for repositories
│   └── usecases/           # Specific business actions (e.g., AddCustomerUseCase)
├── presentation/           # UI layer
│   ├── providers/          # Riverpod StateNotifier/FutureProviders
│   ├── screens/            # UI Pages (Auth, Home, Customer, Settings)
│   └── widgets/            # Screen-specific widgets
├── l10n/                   # Localization files (app_en.arb, app_bn.arb)
└── main.dart               # Entry point, ProviderScope, Hive init
```

---

## 13. State Management

**Riverpod** is used for reactive state management.
- **Providers:** Expose use cases and repositories.
- **StateNotifierProviders:** Manage complex UI states (e.g., `CustomerListState` containing loading, loaded, error states).
- **StreamProviders:** Listen to Hive boxes or Firestore snapshots for real-time UI updates (e.g., Dashboard metrics).
- **AsyncValue:** Handled gracefully in UI to show `CircularProgressIndicator` or Error states without writing manual boilerplate.

---

## 14. Local Database

**Hive** is chosen for its exceptional read/write speed and zero native dependencies, making it perfect for an offline-first Flutter app.
- **Strategy:** All reads power the UI directly from Hive. All writes go to Hive first (optimistic update), marked as `synced: false`. A background worker (via `workmanager` or sync service) pushes changes to Firestore and marks them `synced: true`.

---

## 15. Firebase Services

- **Authentication:** Email/Password for identity management.
- **Firestore:** Cloud NoSQL database for secure data backup.
- **Cloud Storage:** Storing receipt images securely.
- **Firebase Cloud Messaging (FCM):** For system notifications (e.g., "Don't forget to backup your data!").
- **Google Analytics:** Screen tracking, user engagement, feature adoption metrics.
- **Crashlytics:** Real-time crash reporting to ensure app stability.

---

## 16. UI/UX Specification

### Design System (Material 3)
- **Primary Color:** Deep Green (`#1B5E20`) - Symbolizes money, trust, and growth.
- **Secondary Color:** Amber/Gold (`#FFC107`) - For warnings, overdue alerts.
- **Danger Color:** Red (`#D32F2F`) - For Due Given (negative cashflow).
- **Success Color:** Green (`#388E3C`) - For Payment Received.
- **Background:** Off-white (`#F5F5F5`) for light mode, Dark Grey (`#121212`) for dark mode.
- **Typography:** Noto Sans Bengali (Google Fonts) for native readability.

### Screen Examples

**Home/Dashboard:**
- **Layout:** App bar with Shop Name and Settings icon. Top summary cards (Total Due, Today's Collection). List of "Recent Activity" below. Floating Action Button (FAB) at bottom right.
- **Components:** `Card`, `ListTile`, `FloatingActionButton`.
- **Empty State:** Illustration of an empty notebook with text "No transactions yet. Tap + to add."
- **Loading State:** Shimmer effect on cards.

**Customer Details:**
- **Layout:** Header with Customer Name, Phone, Call/WhatsApp icons. Prominent "Total Due" display. Tab view for "All Transactions" vs "Payments". Two sticky buttons at bottom: "Give Due (Red)" and "Receive Payment (Green)".
- **Icons:** Material Symbols (Call, WhatsApp, History).

**Validation Rules:**
- Phone Number: Must be exactly 11 digits (Bangladesh standard: `01XXXXXXXXX`).
- Amount: Must be > 0.
- Name: Cannot be empty.

---

## 17. Edge Cases

- **No Internet Connectivity:** App must function normally, storing data in Hive. Sync occurs automatically upon reconnection.
- **App Killed During Sync:** Implement background sync logic to resume when app restarts.
- **Simultaneous Login:** If a user logs in on a new device, trigger an initial full sync from Firestore to populate local Hive DB.
- **Device Date/Time Changed:** Warn the user if device time doesn't match network time, to prevent transaction date tampering.
- **Large Data Sets:** If a shop has 5,000+ transactions, UI must use pagination/lazy loading (`ListView.builder`) to prevent memory crashes.

---

## 18. Future Features (Phases 2 & 3)

- **Multi Shop / Multi Staff:** Allow an owner to add employees who can only add transactions but cannot delete them or view total business reports.
- **AI Reminder Generator:** Use Gemini API to draft personalized, context-aware payment reminder messages based on customer behavior.
- **OCR Receipt Scanner:** Automatically extract amount and date from a physical supplier bill.
- **Voice Input:** "Add 500 taka due for Rahim" parsed via NLP.
- **QR / Digital Payments:** Deep link integration with bKash/Nagad merchant APIs to receive payments directly via the app.

---

## 19. Success Metrics

- **Daily Active Users (DAU):** Number of shopkeepers opening the app daily.
- **Transactions Per User:** Average number of dues/payments logged per day.
- **Reminder Conversion Rate:** Percentage of WhatsApp/SMS reminders sent that result in a payment logged within 48 hours.
- **Crash-free Sessions:** Maintain > 99.5% crash-free rate via Crashlytics.

---

## 20. MVP Scope (Phase 1)

**Duration: 4 Weeks**
- Firebase Auth (Email/Pass).
- Hive Local DB + Offline CRUD operations.
- Dashboard (Total calculations).
- Customer Management (Add, View, Edit).
- Transaction Logging (Due and Payment).
- Simple WhatsApp/SMS URL launcher intents.
- Basic background sync to Firestore.

## 21. Phase 2 Scope

**Duration: 3 Weeks**
- PDF and Excel Reporting.
- Biometric / PIN App Lock.
- Interactive Charts and Analytics.
- Advanced Filtering and Search.

## 22. Phase 3 Scope

**Duration: 4 Weeks**
- Multi-device support with real-time conflict resolution.
- Staff Roles (Owner vs Cashier).
- Cloud Storage for Receipt Photos.
- Push Notifications for Overdue accounts.

---

## Development Roadmap & Execution

### Sprint Planning

**Sprint 1: Architecture & Core UI (Week 1)**
- Setup Flutter project, Riverpod, Clean Architecture folders.
- Setup Firebase project, configure Android/iOS apps.
- Implement Hive local storage models.
- Build Authentication UI and logic.

**Sprint 2: Core Features (Offline First) (Week 2)**
- Build Dashboard UI.
- Build Customer CRUD operations.
- Build Transaction logging UI and logic.
- Ensure all calculations work flawlessly using local Hive data.

**Sprint 3: Cloud Sync & Integrations (Week 3)**
- Implement Firestore repositories.
- Build background Sync mechanism (Local -> Cloud, Cloud -> Local).
- Implement WhatsApp/SMS intent links.
- Add Biometric/PIN lock.

**Sprint 4: Polish, Testing & Deployment (Week 4)**
- Implement dark mode and localization (Bangla support).
- Conduct extensive offline-to-online transition testing.
- UI/UX polish (animations, empty states, error handling).
- Prepare Play Store assets and release Beta.

### Estimated Development Timeline
- **Total Time:** 4 Weeks for MVP.
- **Team Size:** 1 Senior Flutter Developer, 1 QA (Part-time), 1 UX/UI Designer (Part-time).

### Testing Checklist
- [ ] Login/Signup flow works with error messages for invalid inputs.
- [ ] App operates normally in Airplane mode.
- [ ] Data created offline syncs correctly when internet is restored.
- [ ] Calculations for Total Due update instantly upon adding a new transaction.
- [ ] Dashboard charts render correctly with accurate data.
- [ ] WhatsApp intent opens correctly with pre-filled message.
- [ ] Biometric lock works across different Android OS versions.
- [ ] App retains state on configuration changes (rotation).

### Deployment Checklist
- [ ] Update `applicationId` and `versionCode`.
- [ ] Generate Release Keystore and configure `build.gradle`.
- [ ] Configure Proguard rules for Riverpod and Hive.
- [ ] Finalize Firebase Security Rules.
- [ ] Ensure Google Services JSON/PLIST are correctly configured for Prod environment.
- [ ] Generate screenshots and write Play Store description (Bangla & English).
- [ ] Submit App Bundle (.aab) to Google Play Console.
