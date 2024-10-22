package com.example.taskplanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {
    private val gson = Gson()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Включаем режим Edge-to-Edge
        setContentView(R.layout.activity_login) // Устанавливаем разметку

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            ) // Устанавливаем отступы
            insets // Возвращаем insets
        }


        val toRegistrationButton: Button = findViewById(R.id.to_registration_button)
        toRegistrationButton.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }

        val usernameEditText: EditText = findViewById(R.id.login_username)
        val passwordEditText: EditText = findViewById(R.id.login_password)
        val loginButton: Button = findViewById(R.id.login_button)
        val rememberMeCheckBox: CheckBox = findViewById(R.id.remember_me_checkBox)

        // Загрузка сохраненных данных
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        usernameEditText.setText(sharedPreferences.getString("username", ""))
        passwordEditText.setText(sharedPreferences.getString("password", ""))
        rememberMeCheckBox.isChecked = sharedPreferences.getBoolean("remember_me", false)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (validateUser(username, password)) {
                if (rememberMeCheckBox.isChecked) {
                    // Сохраняем имя пользователя и пароль
                    with(sharedPreferences.edit()) {
                        putString("username", username)
                        putString("password", password) // Сохраняем пароль
                        putBoolean("remember_me", true)
                        apply()
                    }
                } else {
                    // Удаляем сохраненные данные
                    with(sharedPreferences.edit()) {
                        remove("username")
                        remove("password")
                        putBoolean("remember_me", false)
                        apply()
                    }
                }
                Toast.makeText(this, "Логин успешен", Toast.LENGTH_SHORT).show()
                // Здесь можно перейти на главный экран приложения
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Неверное имя пользователя или пароль", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun validateUser(username: String, password: String): Boolean {
        val users = loadUsers()
        val hashedPassword = hashPassword(password)
        return users.any { it.username == username && it.password == hashedPassword }
    }

    private fun loadUsers(): List<User> {
        return try {
            val inputStream = openFileInput("users.json")
            val reader = InputStreamReader(inputStream)
            val usersType = object : TypeToken<List<User>>() {}.type
            gson.fromJson(reader, usersType) ?: emptyList()
        } catch (e: FileNotFoundException) {
            emptyList() // Если файл не найден, возвращаем пустой список
        } catch (e: Exception) {
            emptyList() // Обработка других ошибок
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return Base64.encodeToString(hash, Base64.DEFAULT)
    }
}
