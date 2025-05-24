# Allocate Optimize Track - Expense Management App

Allocate Optimize Track is an Android application designed to help users manage their personal finances by tracking expenses, setting budget goals, and visualizing spending habits. The app utilizes Firebase for real-time data storage and authentication, and Supabase for image storage.

## Table of Contents

1.  [Overview](#overview)
2.  [Features](#features)
    *   [Core Features](#core-features)
    *   [Special Feature 1: Help & Support](#special-feature-1-help--support)
    *   [Special Feature 2: Last Used Category Shortcut](#special-feature-2-last-used-category-shortcut)
3.  [Tech Stack & Architecture](#tech-stack--architecture)
4.  [Prerequisites](#prerequisites)
5.  [Setup & Configuration](#setup--configuration)
    *   [Firebase Setup](#firebase-setup)
    *   [Supabase Setup](#supabase-setup)
    *   [Android Project Setup](#android-project-setup)
6.  [Building and Running the App](#building-and-running-the-app)
7.  [How to Use the App](#how-to-use-the-app)

## Overview

This application provides a user-friendly interface for:
*   Registering and logging in.
*   Managing spending categories with optional monthly limits and goals.
*   Adding expense entries with details like amount, date, description, category, and an optional receipt photo.
*   Viewing a dashboard with daily spending graphs and budget progress against monthly goals.
*   Filtering expenses and viewing totals for user-selectable periods.

## Features

### Core Features

*   **User Authentication:** Secure sign-up and login using Firebase Authentication (Email/Password and Google Sign-In).
*   **Category Management:**
    *   Create, view, update, and delete custom spending categories.
    *   Set optional monthly spending limits for categories.
    *   Set optional monthly budget goals for categories.
*   **Expense Tracking:**
    *   Add new expenses with amount, date, description, and category.
    *   Optionally attach a photo (e.g., a receipt) to each expense entry (stored in Supabase).
    *   View a list of all expenses.
    *   Update and delete existing expense entries.
*   **Dashboard & Reporting:**
    *   View a graph showing daily spending over a user-selectable period, with category spending breakdown.
    *   View a progress dashboard displaying how well the user is staying within their budget goals for categories (based on the selected period for totals).
    *   Filter expenses by date range and view the grand total for that period.
*   **Data Persistence:** User data (categories, expenses) is stored in Firebase Realtime Database.
*   **Real-time Updates:** Expense and category lists update in real-time.
*   **Navigation:** Clean bottom navigation for easy access to Home (Dashboard), Categories, Expenses, and Logout.

### Special Feature 1: Help & Support

*   **In-App Guidance:** A dedicated "Help & Support" screen provides users with:
    *   Clear, step-by-step instructions on how to use the app's main features (managing categories, adding expenses, understanding the dashboard).
    *   A curated list of recommended books on budgeting and personal finance to further assist users in their financial journey. Each book entry can link to an external resource (e.g., Amazon).
    *   Contact information for support.

### Special Feature 2: Last Used Category Shortcut

*   **Quick Expense Entry:** When adding a new expense, a "Use Last: [Category Name]" button appears if a category was previously used.
*   **Convenience:** Tapping this button instantly selects the most recently used category, speeding up the process of logging recurring expenses for the same category.
*   **Adaptive:** The button only shows if there's a stored "last used" category and updates its text to reflect that category's name.

## Tech Stack & Architecture

*   **Language:** Kotlin
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **UI:** Android XML Views with Material Design Components
*   **Asynchronous Operations:** Kotlin Coroutines
*   **Dependency Injection (Manual/ViewModelFactory):** ViewModels are used to manage UI-related data.
*   **Backend:**
    *   **Firebase Realtime Database:** For storing user data (categories, expenses, gamification).
    *   **Firebase Authentication:** For user sign-up and login.
    *   **Supabase Storage:** For storing receipt images.
    *   **Supabase Edge Function:** For secure image uploads to Supabase Storage, authorized by Firebase ID Tokens.
*   **Image Loading:** Glide
*   **Charting:** MPAndroidChart
*   **Navigation:** Android Navigation Component
*   **HTTP Client (for Edge Function):** Ktor
*   **Serialization (for Edge Function communication):** Kotlinx.serialization
*   **Build System:** Gradle

## Prerequisites

*   Android Studio (latest stable version recommended, e.g., Hedgehog or newer)
*   Kotlin Plugin installed in Android Studio
*   Android SDK installed with appropriate API levels (minSdk 24, compileSdk/targetSdk 35 or as per `build.gradle`)
*   A Firebase Project
*   A Supabase Project
*   Supabase CLI (for deploying Edge Functions)
*   Deno (for developing/testing Supabase Edge Functions locally)

## Setup & Configuration

### Firebase Setup

1.  Go to the [Firebase Console](https://console.firebase.google.com/).
2.  Create a new Firebase project or use an existing one.
3.  Add an Android app to your Firebase project:
    *   Use `com.example.allocate_optimize_track` as the package name (or update to your actual package name).
    *   Download the `google-services.json` file.
4.  Place the downloaded `google-services.json` file in the `app/` directory of your Android project.
5.  In the Firebase Console, enable the following services:
    *   **Authentication:** Enable Email/Password and Google Sign-In methods.
    *   **Realtime Database:** Create a database and set up initial security rules (see "Security Rules for RTDB" example in this README or project documentation). A good starting point for user-specific data:
        ```json
        {
          "rules": {
            "categories": { "$uid": { ".read": "auth.uid === $uid", ".write": "auth.uid === $uid" }},
            "expenses": { "$uid": { ".read": "auth.uid === $uid", ".write": "auth.uid === $uid" }},
            "user_gamification_data": { "$uid": { ".read": "auth.uid === $uid", ".write": "auth.uid === $uid" }}
            // Add other top-level nodes as needed
          }
        }
        ```

### Supabase Setup

1.  Go to [Supabase Dashboard](https://app.supabase.io/).
2.  Create a new project or use an existing one.
3.  Navigate to **Storage** and create a new bucket (e.g., `receipt-images`). Make it public if your Edge Function handles signed URLs for reads, or set appropriate RLS policies (see below).
4.  Navigate to **Project Settings > API**:
    *   Note down your **Project URL**.
    *   Note down your **anon public key**.
    *   Note down your **service_role key** (keep this secret, it will be used in Edge Function environment variables).
5.  **Set up Row Level Security (RLS) for Storage (if not solely relying on Edge Functions for all access):**
    *   If your Edge Function (using the service role key) is the *only* way files are written/read/deleted, RLS on `storage.objects` might be very restrictive (e.g., no public access).
    *   If you allow direct client access for reads (e.g., public URLs), ensure appropriate SELECT policies.
    *   Example policy for allowing public reads from the bucket:
        ```sql
        CREATE POLICY "Public read access for images"
        ON storage.objects FOR SELECT
        TO anon, authenticated -- Or just anon if truly public
        USING (bucket_id = 'receipt-images');
        ```
6.  **Edge Function Setup (`upload-receipt`):**
    *   Follow the Supabase documentation to create and deploy the `upload-receipt` Edge Function (TypeScript/Deno). The code is provided in previous discussions.
    *   **Crucially, set the following Environment Variables for this Edge Function in the Supabase Dashboard:**
        *   `SUPABASE_URL`: Your Supabase project URL.
        *   `SUPABASE_SERVICE_ROLE_KEY`: Your Supabase project's service_role key.
        *   `FIREBASE_PROJECT_ID`: Your Firebase Project ID.
        *   (If using Firebase Admin SDK in Edge Function) `FIREBASE_SERVICE_ACCOUNT_KEY`: The JSON content of your Firebase service account key.

### Android Project Setup

1.  Clone the repository (if applicable) or open the project in Android Studio.
2.  Ensure you have the correct `google-services.json` in the `app/` directory.
3.  Create a `strings.xml` file (if it doesn't exist) or update `app/src/main/res/values/strings.xml` with your Supabase credentials and Edge Function URL:
    ```xml
    <resources>
        <string name="YOUR_SUPABASE_URL">YOUR_ACTUAL_SUPABASE_URL</string>
        <string name="YOUR_SUPABASE_ANON_KEY">YOUR_ACTUAL_SUPABASE_ANON_KEY</string>
        <string name="YOUR_SUPABASE_UPLOAD_FUNCTION_ENDPOINT">YOUR_ACTUAL_SUPABASE_FUNCTION_URL/upload-receipt</string>
        <!-- Add other string resources used by the app -->
        <string name="notification_channel_quick_add_name">Quick Add Actions</string>
        <string name="notification_channel_quick_add_description">Shortcuts for adding expenses</string>
        <string name="quick_add_notification_title">Quick Add Expense</string>
        <string name="quick_add_notification_text">Tap to log a new expense.</string>
        <string name="update_expense_button_text">Update Expense</string>
        <string name="save_expense_button_text">Save Expense</string>
        <string name="selected_date_prefix">Selected: %1$s</string>

    </resources>
    ```
    *   Replace placeholder values with your actual Supabase details. The `YOUR_SUPABASE_UPLOAD_FUNCTION_ENDPOINT` is used by `SupabaseImageService.kt`.

## Building and Running the App

1.  Open the project in Android Studio.
2.  Let Gradle sync and download dependencies.
3.  Ensure you have an Android emulator running or a physical Android device connected (with USB debugging enabled).
4.  Click the "Run" button (green play icon) in Android Studio, or select **Run > Run 'app'**.
5.  The app should build and install on your selected device/emulator.

## How to Use the App

1.  **Sign Up / Login:**
    *   On the first launch, you'll be presented with a login screen.
    *   You can sign up using an email and password or sign in with your Google account.
2.  **Dashboard (Home Screen):**
    *   This is your main overview.
    *   **Daily Spending Graph:** Select a "From" and "To" date to see a line graph of your spending per day within that period, broken down by category.
    *   **Monthly Budget Progress:** View a list of your categories that have monthly goals. For each, see how much you've spent in the selected period compared to the goal, visualized with a progress bar. Categories where spending exceeds the goal are highlighted.
3.  **Categories Screen:**
    *   View all your spending categories.
    *   Each item shows the category name, description, and if set, its monthly spending limit and goal, along with total spending for the selected date range (dates selected here also affect the dashboard).
    *   Tap the '+' FAB to add a new category (name, description, optional limit, optional goal).
    *   Tap a category to edit it.
    *   Long-press a category to delete it (this will also delete all associated expenses and their receipt images).
    *   Use the "From" and "To" date pickers at the top to filter the spending totals displayed for each category.
4.  **Expenses Screen:**
    *   View a list of your expense entries for the selected date period.
    *   Each item shows the description, category name, amount, date, and a thumbnail of the receipt if attached.
    *   **Date Filter:** Use the "From" and "To" date pickers at the top to filter the list of expenses and the "Total Spent" displayed.
    *   **Total Spent:** See the sum of all expenses within the currently selected date range.
    *   Tap the '+' FAB to navigate to the "Add Expense" screen.
    *   Tap an expense item to edit it.
    *   Long-press an expense item to delete it.
5.  **Add/Edit Expense Screen:**
    *   **Amount:** Enter the expense amount.
    *   **Date:** Select the date of the expense.
    *   **Category:** Choose from your list of categories using a dropdown.
        *   **Last Used Category Shortcut:** A button "Use Last: [Category Name]" will appear below the dropdown if you've previously saved an expense. Tapping it quickly selects that category.
    *   **Description:** Provide a description for the expense.
    *   **Attach Receipt:** Tap to either take a new photo with the camera or choose an existing image from your gallery. A preview will be shown.
    *   Tap "Save Expense" or "Update Expense".
6.  **Help & Support Screen:**
    *   Access this screen (e.g., from bottom navigation or a menu).
    *   Read instructions on how to use various app features.
    *   Browse recommended books on budgeting, with links to learn more.
    *   Find contact information for support.
7.  **Logout:**
    *   Select "Logout" from the bottom navigation to sign out of the app.

---
