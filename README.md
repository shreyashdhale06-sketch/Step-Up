# Step-Up 👟
### Smart Step Tracking & AI-Powered Fitness Application

**Step-Up** is an Android-based fitness application designed to encourage users to maintain an active and healthy lifestyle through daily step tracking, personalized fitness goals, activity monitoring, and interactive gamification.

The application combines Android sensor technology, AI-powered fitness assistance, and virtual plant growth to make walking more engaging, motivating, and rewarding.

## ✨ Features

- **Daily Step Tracking:** Tracks walking activity using supported Android step-counting sensors.
- **Fitness Dashboard:** Displays daily steps, goal progress, estimated calories burned, distance covered, and walking time.
- **Daily Goals:** Helps users monitor their progress toward personalized step targets.
- **Activity History:** Maintains records of daily steps and walking activity.
- **AI Fitness Coach:** Integrates the Gemini API to provide AI-powered conversational fitness assistance.
- **Virtual Garden:** Allows users to nurture a virtual plant as they achieve walking milestones, earning water drops for every 250 steps.
- **Gamification:** Encourages consistency through achievements, daily challenges, and activity streaks.
- **User Profile:** Supports personal fitness preferences and profile information.
- **Notifications and Settings:** Provides access to relevant updates and application preferences.
- **Local Data Storage:** Uses Room Database and DataStore to manage structured activity records and user preferences.

## 🛠️ Technology Stack

- **Programming Language:** Kotlin
- **Development Environment:** Android Studio
- **User Interface:** Jetpack Compose
- **Design System:** Material 3
- **Architecture:** MVVM (Model–View–ViewModel)
- **Database:** Room Database
- **Preference Storage:** DataStore
- **Asynchronous Programming:** Kotlin Coroutines and Flow
- **Activity Tracking:** Android Sensor APIs
- **AI Integration:** Gemini API
- **Build System:** Gradle Kotlin DSL

## 🏗️ Application Architecture

Step-Up follows an MVVM-inspired architecture to maintain separation between the user interface, application logic, and data management.

- **UI Layer:** Displays the dashboard, activity history, AI Coach, virtual garden, profile, and settings using Jetpack Compose.
- **ViewModel Layer:** Manages screen states and coordinates user interactions.
- **Repository Layer:** Connects application logic with the available data sources.
- **Data Layer:** Uses Room Database for structured records and DataStore for user preferences.
- **Sensor Layer:** Uses supported Android step-counting sensors to track walking activity.
- **AI Layer:** Integrates Gemini-powered functionality for conversational fitness assistance.

## 🚀 Getting Started

### Prerequisites

Before running the application, ensure that you have:

- Android Studio installed on your computer.
- A compatible JDK and Android SDK.
- An Android device or emulator running Android 7.0 (API 24) or later, subject to the project's actual configuration.
- A device with compatible step-counting sensors for hardware-based tracking.
- Gemini API access for AI Coach functionality.

### Installation

**1. Clone the repository**

```bash
git clone https://github.com/shreyashdhale06-sketch/Step-Up.git
```

**2. Open the project**

Launch Android Studio and select **Open**. Choose the downloaded `Step-Up` project folder.

**3. Sync Gradle**

Allow Android Studio to synchronize the project and download the required dependencies.

**4. Configure the API key**

Configure the Gemini API key using the secret-management method supported by the project. Do not commit your actual API key to GitHub.

**5. Run the application**

Connect an Android device or start an emulator, grant the required permissions, and run the application from Android Studio.

## 🔐 API Key Security

Step-Up uses the Gemini API for AI-powered functionality.

- Never publish real API keys, passwords, or other sensitive credentials in a public repository.
- Keep local secret files out of version control.
- Use example configuration files only for placeholder values.
- If a key has been exposed, revoke it and generate a replacement.
- For production deployments, consider a secure backend to protect API credentials.

## 📱 Permissions

The application may require the following permissions, depending on the implemented features:

- **Activity Recognition:** To support step counting and physical activity tracking.
- **Internet Access:** To communicate with online services used by AI features.
- **Sensor Access:** To support compatible device sensors where required.

Users must grant applicable runtime permissions for the associated features to work.

## 🎯 Project Objectives

- Encourage users to develop consistent walking habits.
- Make daily activity and fitness goals easier to monitor.
- Improve user engagement through gamification.
- Integrate AI-powered assistance into a fitness application.
- Demonstrate practical Android development using Kotlin, Jetpack Compose, sensors, and local databases.

## 🔮 Future Scope

Future enhancements may include:

- Cloud backup and data synchronization.
- Integration with wearable fitness devices.
- Advanced activity analytics and progress reports.
- More personalized fitness recommendations.
- Additional health and fitness platform integrations.

## 👨‍💻 Developer

**Shreyash Yuvraj Dhale**  
T.Y. B.Sc. Computer Science

**Project Name:** Step-Up  
**Project Type:** Android Fitness and Step-Tracking Application

## 📄 License

No open-source license has been specified for this repository. A suitable license can be added if the project is intended for public reuse or distribution.

---

*Step-Up turns everyday walking into a more engaging, motivating, and rewarding fitness experience.*
