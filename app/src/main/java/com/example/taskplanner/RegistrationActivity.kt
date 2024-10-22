package com.example.taskplanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.Button
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

class RegistrationActivity : AppCompatActivity() {
    private val gson = Gson()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Включаем режим Edge-to-Edge
        setContentView(R.layout.activity_registration) // Устанавливаем разметку

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom) // Устанавливаем отступы
            insets // Возвращаем insets
        }

        val toLoginButton: Button = findViewById(R.id.to_login_button)
        toLoginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val registerButton: Button = findViewById(R.id.register_button)
        registerButton.setOnClickListener {
            val usernameEditText: EditText = findViewById(R.id.register_username)
            val passwordEditText: EditText = findViewById(R.id.register_password)
            val confirmPasswordEditText: EditText = findViewById(R.id.confirm_password)

            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Проверка на совпадение паролей
            if (password != confirmPassword) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Проверка существования пользователя
            if (isUserExists(username)) {
                Toast.makeText(this, "Пользователь с таким именем уже существует", Toast.LENGTH_SHORT).show()
            } else {
                val hashedPassword = hashPassword(password)
                saveUserData(User(username, hashedPassword))
                Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                finish() // Закрываем экран регистрации
            }
        }
    }

    private fun isUserExists(username: String): Boolean {
        val users = loadUsers()
        return users.any { it.username == username }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return Base64.encodeToString(hash, Base64.DEFAULT)
    }

    private fun saveUserData(user: User) {
        val users = loadUsers().toMutableList()
        users.add(user)
        val json = gson.toJson(users)
        openFileOutput("users.json", Context.MODE_PRIVATE).use { output ->
            output.write(json.toByteArray())
        }
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
}