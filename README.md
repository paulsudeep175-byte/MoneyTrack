# MoneyTrack Android — Cloud APK Build

This project includes a GitHub Actions workflow that builds the Android debug APK in GitHub's cloud. You do not need Android Studio, Android SDK, Java, or Gradle installed on your computer.

## Create your APK using GitHub in a browser

1. Sign in to https://github.com (create a free account if needed).
2. Click **New repository**. Name it `MoneyTrack`, choose **Private** if you prefer, then create it.
3. On the new repository page, choose **Add file → Upload files**.
4. Extract this ZIP on your computer. Upload the project files and folders from inside `MoneyTrackAndroid` to the repository root. Include `app`, `build.gradle.kts`, `gradle.properties`, `settings.gradle.kts`, and `README.md`.
5. Make sure the workflow file is also in the exact path `.github/workflows/build-apk.yml`. If GitHub's upload screen does not include the hidden `.github` directory, use **Add file → Create new file**, enter `.github/workflows/build-apk.yml` as the filename, and paste the contents of that file from the extracted project.
6. Commit the uploaded files to the `main` branch.
7. Open the repository's **Actions** tab. Select **Build MoneyTrack APK** and wait for the run to finish successfully (green check mark).
8. Open the completed workflow run. In **Artifacts**, click **MoneyTrack-debug-apk** to download the APK ZIP.
9. Extract that downloaded artifact ZIP. It contains `app-debug.apk`.
10. Transfer `app-debug.apk` to your Android phone and install it. If prompted, allow your browser or file manager to install unknown apps.

## Build workflow file

The workflow is `.github/workflows/build-apk.yml`. It uses GitHub-hosted Ubuntu runners, Java 17, Android SDK 35, Gradle 8.9, and uploads the generated APK as a workflow artifact.

## App features in this starter build

- Notification-based capture from supported payment/bank apps after the user enables Android Notification Access.
- All-time transaction count, people count, income, expenses, and net flow.
- Person-wise transaction count, money sent, money received, and net received.
- Search, local storage, and CSV export.

## Important limitations

- This is not direct account integration with Google Pay, PhonePe, Paytm, or banks. It only parses notifications that Android makes available after the user grants access.
- It cannot retrieve old in-app transaction history. Notifications must be enabled and contain recognizable amount/direction text.
- Notification parsing is heuristic and differs by app version, bank, language, and notification format. It may miss or misclassify transactions. Review records before relying on totals.
- Transfers between your own accounts can be counted twice if both sides notify.
- The app stores notification text locally on the phone. Protect the device and do not install it on someone else's phone without their informed consent.
- This is an unsigned debug APK for personal testing, not a Play Store release. A production release should use a secure signing key and additional testing.
