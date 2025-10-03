### Tangisu: Alarm That Actually Wake You Up ###

**Vision:**
To create an Android alarm clock app that goes beyond simple time-based alerts. Tangisu aims to ensure users genuinely wake up by incorporating engaging or challenging tasks that must be completed to dismiss the alarm. The app will feature a clean, calming aesthetic inspired by Japanese design principles.

**Target Audience:**
Heavy sleepers, students who struggle to get out of bed, and anyone looking for a more effective and less jarring wake-up experience.

**Key Features (Core Functionality):**
-   Standard time-based alarms with customizable repeat settings (days of the week).
-   User-configurable alarm properties:
    -   Label
    -   Ringtone selection
    -   Vibration (on/off, standard insistent pattern)
    -   Snooze duration
    -   12/24 hour display preference per alarm
-   **Dismissal Challenges (Initial Focus: Math Puzzles):**
    -   Math puzzles (not overly complex, but enough to require focus).
    -   Alarm sound/vibration continues until the challenge is successfully completed.
-   Reliable alarm scheduling and triggering.
-   Minimalist and intuitive UI with Material 3 and custom Japanese-inspired theming (using Shippori Mincho font).
-   List display of all configured alarms with quick toggle (enable/disable).
-   Ability to add, edit, and delete alarms.

**Tech Stack & Architecture:**
-   **UI:** Jetpack Compose with Material 3.
-   **Asynchronous Operations:** Kotlin Coroutines.
-   **Database:** Room for persistent storage of alarms.
-   **Alarm Scheduling:** `AlarmManager` for precise, time-based alarm triggers.
-   **Dependency Injection:** Hilt for managing dependencies (ViewModels, Repositories, Database, etc.).
-   **Navigation:** Jetpack Navigation Compose for navigating between screens (e.g., Alarm List, Add/Edit Alarm, Alarm Firing Screen).
-   **Architecture Pattern:** MVVM (Model-View-ViewModel) with a Repository layer.
    -   ViewModel: Manages UI-related data and business logic.
    -   Repository: Abstracts data sources (Room database).
-   **Background Execution for Alarm Firing:** Foreground Services to ensure reliable sound playback and UI presentation when an alarm triggers, especially on newer Android versions.

**Key Architectural Considerations / To-Do:**
-   **Permissions:** Implement robust handling for `SCHEDULE_EXACT_ALARM` (or `USE_EXACT_ALARM`), `POST_NOTIFICATIONS`, and other necessary permissions.
-   **Alarm Reliability:** Thorough testing across different Android versions and OEM devices to ensure alarms fire consistently (considering Doze mode, battery optimization, etc.).
-   **State Management:** Utilize `StateFlow` and `SharedFlow` in ViewModels for efficient state exposure to Compose UI.
-   **Error Handling:** Implement proper error handling for database operations, alarm scheduling, etc.
-   **Testing:** Plan for unit tests (ViewModels, Repository) and potentially UI tests.
-   **Code Quality:** Maintain clean, readable, and well-documented Kotlin code.

**Design Philosophy:**
-   Clean, calming, and intuitive user experience.
-   Leverage Material 3 components with custom theming inspired by Japanese aesthetics (minimalism, natural colors, specific typography like Shippori Mincho).
-   Focus on reliability and effectiveness as an alarm clock.

