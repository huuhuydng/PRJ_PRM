# 🔐 Logout Implementation Documentation

## 📋 Overview

Đã implement đầy đủ chức năng logout cho ứng dụng Waves of Food Admin với:
- ✅ Firebase Authentication logout
- ✅ Google Sign-In logout
- ✅ Session management với SharedPreferences
- ✅ Clear user data an toàn
- ✅ Navigation flow chính xác
- ✅ Confirmation dialog trước khi logout
- ✅ Helper classes để reuse

---

## 🏗️ Architecture

### 1. **MainActivity.kt** - Main Implementation
File chính xử lý logout trong dashboard

**Features:**
- Confirmation dialog trước khi logout
- Sign out từ Firebase Auth
- Sign out từ Google Sign-In
- Clear user session data
- Navigate về Login screen với clear activity stack
- Check authentication status khi resume

### 2. **AuthManager.kt** - Authentication Helper
Singleton class để quản lý authentication operations

**Features:**
- Centralized authentication management
- Easy to reuse across activities
- Handles both Firebase and Google Sign-In
- Session management
- User information getters

### 3. **SessionManager.kt** - Session Management
Class quản lý user session data trong SharedPreferences

**Features:**
- Store/retrieve user data
- Session expiration check (30 days)
- Remember me functionality
- Clear session on logout

---

## 💻 Code Implementation

### MainActivity.kt - Logout Flow

```kotlin
// 1. User clicks logout button
binding.cardView2.setOnClickListener {
    showLogoutDialog()
}

// 2. Show confirmation dialog
private fun showLogoutDialog() {
    AlertDialog.Builder(this)
        .setTitle("Logout")
        .setMessage("Are you sure you want to logout?")
        .setPositiveButton("Yes") { dialog, _ ->
            performLogout()
        }
        .setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

// 3. Perform logout operations
private fun performLogout() {
    // Sign out from Firebase
    auth.signOut()
    
    // Sign out from Google
    googleSignInClient.signOut().addOnCompleteListener(this) { task ->
        if (task.isSuccessful) {
            clearUserSession()
            navigateToLogin()
        }
    }
}

// 4. Navigate to Login
private fun navigateToLogin() {
    val intent = Intent(this, LoginActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
    finish()
}
```

---

## 🔄 Logout Process Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     User clicks Logout                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│           Show Confirmation Dialog (Are you sure?)          │
└────────────────────────┬────────────────────────────────────┘
                         │
                    Yes  │  No
           ┌─────────────┴─────────────┐
           ▼                           ▼
    ┌──────────────┐           ┌──────────────┐
    │ Perform      │           │ Cancel &     │
    │ Logout       │           │ Stay on Page │
    └──────┬───────┘           └──────────────┘
           │
           ▼
    ┌──────────────────────┐
    │ Firebase Auth        │
    │ signOut()            │
    └──────┬───────────────┘
           │
           ▼
    ┌──────────────────────┐
    │ Google Sign-In       │
    │ signOut()            │
    └──────┬───────────────┘
           │
           ▼
    ┌──────────────────────┐
    │ Clear SharedPrefs    │
    │ (user_prefs)         │
    └──────┬───────────────┘
           │
           ▼
    ┌──────────────────────┐
    │ Show Toast Message   │
    │ "Logged out"         │
    └──────┬───────────────┘
           │
           ▼
    ┌──────────────────────┐
    │ Navigate to Login    │
    │ Clear Activity Stack │
    └──────┬───────────────┘
           │
           ▼
    ┌──────────────────────┐
    │ LoginActivity        │
    │ (Fresh Start)        │
    └──────────────────────┘
```

---

## 🎯 Key Features

### 1. **Confirmation Dialog**
- Prevents accidental logout
- User-friendly confirmation
- Can cancel if clicked by mistake

### 2. **Complete Sign Out**
- Firebase Authentication
- Google Sign-In (if used)
- All authentication providers handled

### 3. **Session Cleanup**
- Clear SharedPreferences
- Remove cached user data
- Clean state for next login

### 4. **Activity Stack Management**
- Use `FLAG_ACTIVITY_NEW_TASK` and `FLAG_ACTIVITY_CLEAR_TASK`
- Prevents user from going back after logout
- Fresh login experience

### 5. **Authentication Check**
- Verify auth status on resume
- Auto-redirect if not authenticated
- Secure against session hijacking

---

## 📱 Usage Examples

### Basic Usage in MainActivity

```kotlin
// Already implemented - just click logout button
binding.cardView2.setOnClickListener {
    showLogoutDialog()
}
```

### Using AuthManager (Alternative)

```kotlin
// In your Application class or Activity onCreate
AuthManager.initialize(this, getString(R.string.default_web_client_id))

// Logout with AuthManager
AuthManager.logout(
    context = this,
    onSuccess = {
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        AuthManager.navigateToLogin(this)
    },
    onError = { exception ->
        Toast.makeText(this, "Logout failed: ${exception.message}", Toast.LENGTH_SHORT).show()
    }
)
```

### Using SessionManager

```kotlin
val sessionManager = SessionManager(this)

// Save session on login
sessionManager.createLoginSession(
    userId = "user123",
    email = "user@example.com",
    userName = "John Doe",
    rememberMe = true
)

// Check if logged in
if (sessionManager.isLoggedIn()) {
    // User is logged in
}

// Get user data
val userId = sessionManager.getUserId()
val email = sessionManager.getUserEmail()

// Check session expiration
if (sessionManager.isSessionExpired()) {
    // Force logout
}

// Clear session on logout
sessionManager.clearSession()
```

---

## 🔒 Security Considerations

### 1. **Authentication Verification**
```kotlin
override fun onResume() {
    super.onResume()
    checkUserAuthentication()
}

private fun checkUserAuthentication() {
    val currentUser = auth.currentUser
    if (currentUser == null) {
        navigateToLogin()
    }
}
```

### 2. **Activity Stack Clearing**
```kotlin
intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
```
- Prevents back button navigation after logout
- Ensures clean logout

### 3. **Session Expiration**
```kotlin
// Check if session expired (30 days)
if (sessionManager.isSessionExpired()) {
    performLogout()
}
```

---

## 🧪 Testing Checklist

- [ ] Click logout button shows confirmation dialog
- [ ] Clicking "No" cancels logout
- [ ] Clicking "Yes" performs logout
- [ ] Firebase Auth signs out correctly
- [ ] Google Sign-In signs out correctly
- [ ] SharedPreferences are cleared
- [ ] Navigation goes to Login screen
- [ ] Back button doesn't go back to MainActivity
- [ ] Login again works correctly
- [ ] Re-login after logout works
- [ ] Session check on resume works

---

## 🐛 Troubleshooting

### Issue: Back button goes back after logout
**Solution:** Use correct intent flags:
```kotlin
intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
```

### Issue: Google Sign-In doesn't sign out
**Solution:** Initialize GoogleSignInClient properly:
```kotlin
val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken(getString(R.string.default_web_client_id))
    .requestEmail()
    .build()
googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
```

### Issue: User data still exists after logout
**Solution:** Clear SharedPreferences:
```kotlin
getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
```

---

## 📊 Files Modified/Created

### Modified:
- ✅ `app/src/main/java/com/example/wavesoffoodadmin/MainActivity.kt`

### Created:
- ✅ `app/src/main/java/com/example/wavesoffoodadmin/utils/AuthManager.kt`
- ✅ `app/src/main/java/com/example/wavesoffoodadmin/utils/SessionManager.kt`
- ✅ `LOGOUT_IMPLEMENTATION.md` (this file)

---

## 🚀 Build & Run

```bash
# Build the project
./gradlew clean build

# Run on device/emulator
./gradlew installDebug

# Or in Android Studio
# Click Run → Run 'app'
```

---

## 📝 Additional Notes

### SessionManager Benefits:
1. **Persistent login** - Remember user between app restarts
2. **User data caching** - Quick access to user info
3. **Session expiration** - Auto-logout after 30 days
4. **Remember me** - Optional feature for convenience

### AuthManager Benefits:
1. **Centralized auth logic** - Use anywhere in app
2. **Reusable code** - Don't repeat yourself
3. **Easy maintenance** - Update once, works everywhere
4. **Error handling** - Consistent error management

---

## 🎓 Best Practices Implemented

✅ **Confirmation before logout** - Better UX
✅ **Complete session cleanup** - Security
✅ **Activity stack management** - Prevent back navigation
✅ **Error handling** - Graceful failures
✅ **Toast messages** - User feedback
✅ **Code documentation** - Maintainable code
✅ **Modular design** - Reusable components
✅ **Security checks** - Auth verification

---

## 📞 Support

Nếu có vấn đề gì, hãy check:
1. Firebase Auth dependencies đã được thêm
2. Google Sign-In cấu hình đúng
3. Internet permission trong AndroidManifest
4. google-services.json file tồn tại

---

## ✨ Summary

Đã implement đầy đủ chức năng logout với:
- 🔐 Secure logout flow
- 🎯 User confirmation dialog  
- 📱 Clean navigation
- 💾 Session management
- 🛡️ Security best practices
- 📚 Reusable helper classes
- 📖 Complete documentation

**Ready to use! Test và enjoy! 🎉**

