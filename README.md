# 📖 Roomatch – README

## 🏠 What is Roomatch?
Roomatch is an Android application designed to smartly and efficiently match apartment seekers, roommates, and property owners.  
It enables users to post apartments, search and filter listings based on preferences (price, location, lifestyle, etc.), chat in real-time, manage personal profiles, receive push notifications, and report inappropriate content.

---

## 📂 Project Structure

```plaintext
Roomatch/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/roomatch/
│   │   │   │   ├── model/           # Data models (Apartment, UserProfile, Message, Chat...)
│   │   │   │       ├── repository/      # Firebase data access (Firestore, Storage, Auth)
│   │   │   │   ├── utils/           # utils classes (ChatUtil, FirebaseUtils, MyFirebaseMessagingServicec...)
│   │   │   │   ├── view/            # UI (Activities + Fragments)
│   │   │   │   │   ├── activities/ 
│   │   │   │   │   └── fragments/   # OwnerApartmentsFragment, ApartmentSearchFragment, etc.
│   │   │   │   ├── viewmodel/       # ViewModels per MVVM
│   │   │   │   └── adapters/        # RecyclerView adapters
│   │   │   └── res/                 # Layout XMLs, images, strings...
│   │   ├── androidTest/             # 🧪 UI Tests (Espresso)
│   │   │   └── java/com/example/roomatch/ui/
│   │   │       ├── AuthActivityTest.java
│   │   └── test/                    # 🧪 Unit Tests (Mockito, Robolectric)
│   │       └── java/com/example/roomatch/
│   │           └── view/fragments
│   │               └── ApartmentManagementTests.java
│   │
│   └── build.gradle
│
├── build.gradle (root)
└── README.md  (📌 You are here)
```

---

## 🧪 Testing

Roomatch includes two main types of tests:

### 1. **Unit Tests** – ViewModel and Repository logic
- Location:  
  `app/src/test/java/com/example/roomatch/`
- Examples:
  - `OwnerApartmentsViewModelTest.java` – tests for publish/update/load using mocked repository.
  - Uses `@Mock`, `when(...)`, `Tasks`, etc.

### 2. **UI Tests (Espresso)** – User interface tests
- Location:  
  `app/src/androidTest/java/com/example/roomatch/ui/`
- Examples:
  - `AuthActivityTest.java` – tests login (checks for Toast "Login successful").
  - `ApartmentManagementUITest.java` – tests posting/updating/deleting apartments via `OwnerApartmentsFragment`.

---

## 🏗 Architecture
The app uses **MVVM** architecture:
- **Model** – POJO models and Firebase data access.
- **ViewModel** – Logic, state management, interaction with repositories.
- **View** – Activities and Fragments using LiveData and observers to reflect UI state.

---

## 📱 Key Features
- 🔑 Auth (Email & Google Sign-In via Firebase)
- 👤 Profile management
- 🏠 Apartment listing and filtering (price, area, lifestyle)
- 💬 Real-time private & group chat
- 📢 Push notifications (FCM)
- 🚩 Apartment reporting
- 🗺 Advanced search via address/location (Google Maps + Places API)

---

## 🚀 How to Run
1. Open project in **Android Studio**
2. Minimum SDK: `minSdk 26`
3. Add `google-services.json` with Firebase config and Maps API keys.
4. Run on emulator or physical Android 8+ device.

---

## 👨‍💻 Team
- Ariel Yaakobi (Team Lead)  
- Yoav Yaakobi  
- Yaakov Nechmani  
- Gabi Karovayak

---

Generated on 2025-09-08.
