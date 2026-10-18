# Narada-Inspired IPO Application & Demat Management App

A production-grade, privacy-first Android application to organize Initial Public Offering (IPO) applications across multiple Demat accounts and family members, track UPI mandate IDs with SEBI-compliant PAN matching rules, manage bidding checklists and deadlines, and verify allotment statuses from a centralized dashboard.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX choices have been aligned based on user preferences and Indian financial market regulations:

- **Confirmed Decision 1 (Visual Theme)**: Modern fintech deep navy (`#0B132B`, `#1C2541`) and slate foundation with crisp emerald (`#00C49F`) and electric cyan (`#48CAE4`) financial accent badges, adhering strictly to Material 3 tokens.
- **Confirmed Decision 2 (Account Structure)**: Family Member Profiles with linked Demat accounts (Zerodha, Groww, AngelOne, Upstox, etc.) and linked UPI IDs, enforcing SEBI's strict **One Application per PAN** and **PAN-matching UPI validation** rule to prevent mandate rejections.
- **Confirmed Decision 3 (Sample Data Scope)**: High-fidelity realistic pre-populated dataset featuring active, upcoming, and recently closed Mainboard and SME IPOs (e.g., Hyundai Motor India, Tata Technologies, Premier Energies, SME tech innovators), complete with issue dates, lot sizes, price bands, and registrar links.
- **Confirmed Decision 4 (Architecture & Durability)**: Local-first persistence via Jetpack Room with reactive Kotlin Coroutines/Flow, client-side data export/deletion, zero network scraping, zero storage of sensitive credentials (no PINs, passwords, or OTPs), and explicit modular extension points for Stage 2 authorized broker/registrar integrations.

---

## 1. Overview & Core Concept

### What It Does
Indian retail and HNI investors frequently apply for oversubscribed IPOs across multiple family demat accounts to increase allotment probability. However, managing applications across multiple brokers (Zerodha, Groww, Angel One, ICICI Direct), matching the correct applicant's UPI ID, approving UPI mandates before 5 PM deadlines, and checking allotments across fragmented registrar portals (Link Intime, KFintech, Bigshare, Cameo) causes missed mandates, erroneous third-party UPI bids, and chaotic tracking.

This app solves these pain points by:
1. **Multi-Demat & Family Management**: Grouping Demat accounts under distinct family member PAN profiles with depository details (CDSL/NSDL, DP ID, Client ID/BOID).
2. **SEBI Compliance & Third-Party UPI Guard**: Enforcing and alerting that the UPI ID and bank account must match the applicant's PAN as mandated by SEBI/NPCI, preventing automatic application cancellation.
3. **Multi-Account Application Wizard**: Creating individual or batch application entries across eligible family Demat accounts with automatic lot/cutoff calculations and allotment status tracking.
4. **IPO Calendar & T+3 Timelines**: Tracking Issue Open, Issue Close (with 5:00 PM cutoff warning), Allotment Finalization (T+1), Refund/Unblock (T+2), and Listing (T+3).
5. **Direct Official Allotment Lookup**: Launching targeted, pre-filled registrar and exchange query links without illegal page scraping.
6. **Privacy-Preserving Local Storage**: Keeping all financial accounts securely on-device with full JSON export and instant data wiping capabilities.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Onboarding & First-Time Setup**:
   - Welcome banner introducing privacy-first local tracking and regulatory principles.
   - Quick setup wizard to create the Primary Investor profile (Name, Masked PAN `ABCDE****F`), default broker account, and default UPI VPA (`name@okhdfcbank`).
2. **Dashboard Overview**:
   - Metrics cards: Total Applications, Blocked Capital (₹), Active Mandates Awaiting Approval, Allotments Won.
   - Actionable "Mandates Expiring Today" banner.
   - Quick-access horizontal carousel of Open/Upcoming IPOs.
   - Recent Applications list with real-time status chips (`Applied`, `Mandate Accepted`, `Allotted`, `Not Allotted`, `Refunded`).
3. **IPO Explorer (Mainboard & SME)**:
   - Filterable tabs: `All`, `Open Now`, `Upcoming`, `Closed / Allotment Out`.
   - Detailed IPO sheet: Price Band, Minimum Bid, Lot Size, Issue Size, Retail/HNI Quota, GMP Indicator note, Registrar info, and T+3 schedule checklist.
   - "Apply for Family" button launching the multi-account application planner.
4. **Application Creator & UPI Safeguard**:
   - Select IPO and select one or more family Demat accounts.
   - Choose category: Retail (RII, up to ₹2 Lakh), Small HNI (sHNI, ₹2L–₹5L via UPI), or Big HNI (bHNI, >₹5L via ASBA net banking).
   - Validation alert: Verifies that the selected UPI ID belongs to the Demat account holder's PAN profile. Warns if a mismatched third-party UPI is attempted.
   - Checklist: "Bid submitted on broker/ASBA", "UPI mandate approved on GPay/PhonePe/BHIM", "Application number recorded".
5. **Allotment Checker & Registrar Hub**:
   - Shows registrar for each IPO (Link Intime, KFintech, Bigshare, etc.) with quick copy of PAN or Application Number.
   - Direct button to launch the official registrar allotment lookup webpage or exchange verification portal.
   - Status toggle: Mark as Allotted (enter shares allotted & refund amount), Not Allotted, or Awaiting.
6. **Demat & Family Profiles**:
   - Manage family members (Self, Spouse, Parents, Children).
   - Add/edit Demat accounts with broker selection (Zerodha, Groww, Angel One, Upstox, Kotak, HDFC Sky, ICICI Direct).
   - Masked PAN display for security.
7. **Compliance, Privacy & Settings**:
   - SEBI/NPCI Regulatory Notice explaining UPI mandate rules, ₹5 Lakh limit, and T+3 cycle.
   - Data Export (JSON formatted for user backup).
   - Clear All Data (immediate device wipe).
   - Stage 2 Integrations Matrix & Developer Extension Hub.

### Visual Identity & Theme
- **Theme Style**: Modern Fintech Dark/Light with Navy and Slate tones.
- **Color Tokens**:
  - `Primary`: `#1A56DB` (Deep Royal Blue) / `#3B82F6` (Electric Blue)
  - `Secondary / Accents`: `#00C49F` (Emerald Green for Allotted/Success), `#F59E0B` (Amber for Mandate Pending), `#EF4444` (Coral Red for Rejected/Closed)
  - `Background Dark`: `#0A0F1D` (Midnight Slate)
  - `Surface Dark`: `#131B2E` (Layered Navy Card)
  - `Surface Variant`: `#1E293B` (Border & Chip Fill)
  - `OnSurface`: `#F8FAFC` (High contrast text)
  - `Outline`: `#334155`
- **Typography**: Clean, readable sans-serif system type hierarchy with tabular figures (`FontFeatureSettings = "tnum"`) for monetary amounts (₹) and lot numbers.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Local Room Database vs Remote Cloud DB**
  - *Chosen Approach*: Device-local Room database with reactive Kotlin `Flow`s.
  - *Why*: User financial Demat numbers, PANs, and application numbers are sensitive. Users expect maximum privacy. Room allows instantaneous offline operation without requiring login credentials or risk of cloud data leakage.
  - *Alternatives Considered*: Firebase Firestore was considered, but storing Indian PANs and bank VPAs on cloud servers without extensive CERT-In compliance and banking partnerships creates legal exposure. Room local-first with JSON export is the gold standard for personal finance utilities.
- **Decision 2: Direct Registrar Launch vs Web Scraping / Captcha Bypass**
  - *Chosen Approach*: Curated registry of verified official registrar & exchange URLs with 1-tap clipboard copy of PAN / Application No. and launch in Custom Tabs.
  - *Why*: SEBI regulations and Indian IT Act strictly prohibit unauthorized scraping of registrar portals (Link Intime, KFintech) or bypassing CAPTCHA challenges. Attempting automated scraping is fragile, illegal, and causes IP blocks.
- **Decision 3: PAN-Matching UPI Guard**
  - *Chosen Approach*: Demat accounts are linked to a specific Family Member PAN. When recording an application, the app validates whether the selected UPI ID is owned by that same family member. If mismatched, it displays an informative SEBI compliance warning explaining why third-party UPI bids get rejected.
  - *Why*: One of the most common reasons retail investors lose IPO opportunities is paying with a spouse/parent's UPI ID for their own Demat bid.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
├────────────────────────────────────────────────────────────────────────┤
│  DashboardScreen  │  IpoListScreen  │  ApplicationScreen  │  DematScreen│
│  AllotmentHub     │  ComplianceView │  SettingsScreen     │  Theme/M3   │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ (StateFlow / Events)
┌────────────────────────────────────▼───────────────────────────────────┐
│                      ViewModel Layer (MVVM)                            │
│  - IpoViewModel: Manages IPO catalog, filters, upcoming schedules      │
│  - DematViewModel: Family members, Demat accounts, UPI IDs             │
│  - ApplicationViewModel: Multi-account bids, status transitions        │
│  - SettingsViewModel: Data export/import, purge, compliance matrix     │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ (Repository Pattern)
┌────────────────────────────────────▼───────────────────────────────────┐
│                      Domain & Repository Layer                         │
│  - IpoRepository           - DematRepository                           │
│  - ApplicationRepository   - ComplianceValidator                       │
│  - ExtensionPoint: AuthorizedBrokerApi / AuthorizedRegistrarApi (Stubs)│
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                   Room Database Layer (SQLite / KSP)                   │
│  - FamilyMemberEntity  - DematAccountEntity  - UpiAccountEntity        │
│  - IpoEntity           - IpoApplicationEntity- StatusEventEntity       │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Models & Room Schema

1. **`FamilyMemberEntity`**:
   - `id`: Long (PK)
   - `fullName`: String
   - `relationship`: String (Self, Spouse, Parent, Child, Other)
   - `maskedPan`: String (e.g. `ABCDE1234F` stored, masked as `ABCDE****F` in UI)
   - `notes`: String?

2. **`DematAccountEntity`**:
   - `id`: Long (PK)
   - `familyMemberId`: Long (FK to FamilyMember)
   - `brokerName`: String (Zerodha, Groww, Angel One, Upstox, etc.)
   - `depository`: String (CDSL / NSDL)
   - `dpId`: String
   - `clientId`: String / BOID
   - `accountAlias`: String

3. **`UpiAccountEntity`**:
   - `id`: Long (PK)
   - `familyMemberId`: Long (FK to FamilyMember)
   - `vpa`: String (e.g. `rahul@okhdfcbank`)
   - `bankName`: String
   - `isDefault`: Boolean

4. **`IpoEntity`**:
   - `id`: Long (PK)
   - `companyName`: String
   - `symbol`: String
   - `series`: String (Mainboard / SME)
   - `minPrice`: Double
   - `maxPrice`: Double
   - `lotSize`: Int
   - `issueSizeCr`: Double
   - `openDate`: String
   - `closeDate`: String
   - `allotmentDate`: String
   - `listingDate`: String
   - `registrarName`: String (Link Intime, KFintech, Bigshare, Cameo, etc.)
   - `registrarUrl`: String
   - `status`: String (Upcoming, Open, Closed, Allotted, Listed)
   - `gmpNote`: String?

5. **`IpoApplicationEntity`**:
   - `id`: Long (PK)
   - `ipoId`: Long (FK)
   - `dematAccountId`: Long (FK)
   - `familyMemberId`: Long (FK)
   - `upiAccountId`: Long? (FK)
   - `category`: String (Retail, sHNI, bHNI, Shareholder, Employee)
   - `lotCount`: Int
   - `appliedCutoff`: Boolean
   - `bidPrice`: Double
   - `totalAmount`: Double
   - `applicationNumber`: String
   - `mandateStatus`: String (Pending, MandateApproved, Rejected)
   - `allotmentStatus`: String (Awaiting, Allotted, NotAllotted, Refunded)
   - `sharesAllotted`: Int
   - `mandateExpiry`: String?
   - `createdAt`: Long

6. **`StatusEventEntity`**:
   - `id`: Long (PK)
   - `applicationId`: Long
   - `timestamp`: Long
   - `eventType`: String (Submitted, MandateReceived, MandateApproved, AllotmentChecked)
   - `note`: String

---

## 5. Regulatory Compliance & Feature Matrix

| Feature | Execution Stage | Technical Method | Regulatory / Authorization Requirement |
|---|---|---|---|
| **Multi-Demat Account Catalog** | Stage 1 (MVP) | Local Room DB | Permitted. Client-side personal tracker. |
| **Family Member PAN Profiles** | Stage 1 (MVP) | Masked display, local storage | Safe storage. Never transmits PAN to remote servers. |
| **PAN & UPI Matching Guard** | Stage 1 (MVP) | Client validation rule | Enforces **SEBI circular SEBI/HO/CFD/DIL2/CIR/P/2022/45**: UPI and ASBA bank account holder must match Demat PAN. |
| **₹5 Lakh UPI Mandate Limit Check** | Stage 1 (MVP) | UI validator | Enforces NPCI / RBI UPI IPO mandate ceiling of ₹5,00,000. |
| **T+3 Settlement Calendar** | Stage 1 (MVP) | Local rules engine | Complies with SEBI mandate (effective Dec 1, 2023) for listing within 3 business days of issue closure. |
| **Allotment Lookup Web Links** | Stage 1 (MVP) | Android Custom Tabs / Intent | Permitted official deep links. No scraping, zero CAPTCHA bypass. |
| **Data Export & Wipeout** | Stage 1 (MVP) | JSON exporter & DB clear | Complies with Digital Personal Data Protection (DPDP) Act 2023 right to erasure and portability. |
| **Direct Broker Bid Submission (Stage 2)** | Stage 2 (Architecture Stub) | Extension Point Interface | Requires commercial broker API agreements (e.g. Zerodha Kite Connect, Angel One SmartAPI Partner). |
| **Live Registrar Allotment Webhooks (Stage 2)** | Stage 2 (Architecture Stub) | Extension Point Interface | Requires registrar B2B API authorization (Link Intime / KFintech). |

---

## 6. Implementation Milestones

- **Milestone 1**: Set up Room database schema, DAOs, entities, and type converters with pre-seeded realistic Indian IPOs and sample Demat profiles.
- **Milestone 2**: Build clean Material 3 theme, navigation, and core Dashboard with active mandate countdown and portfolio breakdown.
- **Milestone 3**: Build IPO List & Detail view with Mainboard/SME filters, price calculations, and T+3 timeline visualizer.
- **Milestone 4**: Build Family Member & Demat Account Manager with PAN masking and linked UPI VPAs.
- **Milestone 5**: Build the Multi-Account Application Wizard with SEBI PAN-matching validation and checklist.
- **Milestone 6**: Build the Allotment & Registrar Hub with 1-click PAN copy and official portal links.
- **Milestone 7**: Build Compliance, Privacy, Data Export, and Stage 2 Authorized Integration Architecture documentation screen.
- **Milestone 8**: Verify end-to-end compilation with `compile_applet` and perform UI sanity validation.
