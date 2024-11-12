// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("taskplanner");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("taskplanner")
//      }
//    }

#include <jni.h>
#include <string>
#include <vector>

struct User {
    std::string username;
    std::string password;
};

// C++ функция для поиска пользователя
bool findUser(const std::string& username, const std::string& hashedPassword, const std::vector<User>& users) {
    return std::any_of(users.begin(), users.end(), [&](const User& user) {
        return user.username == username && user.password == hashedPassword;
    });
}

// JNI-обертка для вызова findUser
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_taskplanner_LoginActivity_findUserInNative(JNIEnv *env, jobject thiz, jstring username, jstring hashedPassword, jobjectArray users) {
    const char* usernameChars = env->GetStringUTFChars(username, 0);
    const char* passwordChars = env->GetStringUTFChars(hashedPassword, 0);

    // Преобразование строки из JNI в std::string
    std::string usernameStr(usernameChars);
    std::string passwordStr(passwordChars);

    // Преобразование массива пользователей из jobjectArray
    std::vector<User> userList;
    jsize userCount = env->GetArrayLength(users);
    for (jsize i = 0; i < userCount; ++i) {
        jobject userObj = env->GetObjectArrayElement(users, i);

        jclass userClass = env->GetObjectClass(userObj);

        // Получение полей username и password из объекта User
        jfieldID usernameField = env->GetFieldID(userClass, "username", "Ljava/lang/String;");
        jfieldID passwordField = env->GetFieldID(userClass, "password", "Ljava/lang/String;");

        jstring userUsername = (jstring) env->GetObjectField(userObj, usernameField);
        jstring userPassword = (jstring) env->GetObjectField(userObj, passwordField);

        const char* userUsernameChars = env->GetStringUTFChars(userUsername, 0);
        const char* userPasswordChars = env->GetStringUTFChars(userPassword, 0);

        userList.push_back(User{userUsernameChars, userPasswordChars});

        // Освобождение ресурсов
        env->ReleaseStringUTFChars(userUsername, userUsernameChars);
        env->ReleaseStringUTFChars(userPassword, userPasswordChars);
    }

    // Выполнение поиска
    bool result = findUser(usernameStr, passwordStr, userList);

    // Освобождение ресурсов
    env->ReleaseStringUTFChars(username, usernameChars);
    env->ReleaseStringUTFChars(hashedPassword, passwordChars);

    return result ? JNI_TRUE : JNI_FALSE;
}



