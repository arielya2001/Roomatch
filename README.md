# 🏠 Roomatch – Smart Roommate & Apartment Matching

**Roomatch** is a comprehensive Android-native solution designed to streamline the often frustrating process of finding compatible roommates and apartments. By moving away from cluttered social media groups, Roomatch provides a centralized, profile-based platform that prioritizes lifestyle compatibility and real-time communication.

---

## 💡 The Problem & Our Solution
* **The Problem:** Traditional platforms like Facebook groups or generic listing sites often lack lifestyle filtering, leading to irrelevant leads, privacy concerns, and fragmented communication.
* **The Roomatch Solution:** A holistic ecosystem connecting property owners, seekers, and current tenants through a data-driven matching system and integrated communication tools.

---

## 📱 Key Features & User Benefits

### 👤 Profile-Driven Matching
* **For Seekers:** Create detailed profiles highlighting lifestyle habits (cleanliness, smoking, pets, social style) to find like-minded roommates.
* **For Owners:** Manage listings efficiently with screening tools to ensure only relevant candidates apply.

### 💬 Seamless Communication
* **Real-Time Chat:** Integrated private and group messaging powered by Firebase, eliminating the need to share personal phone numbers prematurely.
* **Instant Notifications:** Stay updated with match alerts and message notifications via Firebase Cloud Messaging (FCM).

### 📍 Location Intelligence
* **Smart Search:** Utilize **Google Places API** for precise address auto-complete and location-based filtering.

### 🛡️ Safety & Moderation
* **Reporting System:** Built-in community reporting tools allowing users to flag inappropriate content or suspicious profiles for admin review.

---

## 🏗 Architecture & Tech Stack
The application is built on a robust, scalable foundation to ensure maintainability and performance:

* **Pattern:** **MVVM (Model-View-ViewModel)** – Ensures a clean separation of concerns, making the UI reactive and the logic testable.
* **Frontend:** Native Android development utilizing **Jetpack Compose** for a modern UI.
* **Backend (BaaS):** **Firebase**:
    * *Authentication:* Secure Email & Google Sign-In.
    * *Realtime Database / Firestore:* Live sync for chats and listings.
    * *Cloud Storage:* High-performance image hosting for apartment and profile photos.
* **APIs:** **Google Places API** for efficient location-based searching.

---

## 📂 Project Structure
```plaintext
Roomatch/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/roomatch/
│   │   │   │   ├── model/         # POJO Data models (Apartment, User, Message)
│   │   │   │   ├── repository/    # Data abstraction layer (Firebase access)
│   │   │   │   ├── viewmodel/     # Logic & State management
│   │   │   │   ├── view/          # Jetpack Compose UI components
│   │   │   │   └── utils/         # Helper classes (Messaging, Chat utils)
│   │   │   └── res/               # Static resources & layouts
│   │   ├── androidTest/           # 🧪 UI Integration Tests (Espresso)
│   │   └── test/                  # 🧪 Logic & ViewModel Unit Tests (Mockito)
└── README.md
```

---

## 🧪 Quality Assurance
We prioritized reliability through a dual-testing strategy:
* **Unit Testing:** Leveraged **Mockito** to isolate ViewModel logic, ensuring data processing and repository interactions are flawless.
* **UI Testing:** Used **Espresso** to simulate user journeys (Login, Search, Posting), ensuring a smooth experience.

---

## 🚀 Getting Started
1. **Clone:** `git clone https://github.com/arielya2001/Roomatch`
2. **Environment:** Open in **Android Studio** (minSdk 26).
3. **Config:** Add your `google-services.json` in the `/app` directory and ensure your **Google Places API** key is configured.
4. **Launch:** Run on an emulator or physical device (Android 8.0+).

---

## 👨‍💻 Development Team
* **Ariel Yaacobi** – Team Lead & System Architect
* **Yoav Nachmany** – Full Stack Developer
* **Yaacov Krawiec** – Backend & Integration Specialist
* **Gabi Rayman** – UI/UX & Frontend Developer
