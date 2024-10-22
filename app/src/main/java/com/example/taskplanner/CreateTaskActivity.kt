package com.example.taskplanner


import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class CreateTaskActivity : AppCompatActivity() {
    private lateinit var categories: MutableList<String> // Список категорий

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)

        // Получаем списки из Intent
        categories = intent.getStringArrayListExtra("categories")?.toMutableList() ?: mutableListOf()


        // Инициализация Spinner с категориями
        val categorySpinner: Spinner = findViewById(R.id.task_category_spinner)
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        // Кнопка для выбора даты
        val datePickerButton: Button = findViewById(R.id.date_picker_button)
        datePickerButton.setOnClickListener {
            showDatePicker()
        }

        // Кнопка для выбора времени
        val timePickerButton: Button = findViewById(R.id.time_picker_button)
        timePickerButton.setOnClickListener {
            showTimePicker()
        }

        // Кнопка для сохранения задачи
        val saveTaskButton: Button = findViewById(R.id.save_task_button)
        saveTaskButton.setOnClickListener {
            saveTask()
        }

        // Кнопка Назад
        val backButton: Button = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish() // Завершить активность и вернуться назад
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveTask() {
        val title = findViewById<EditText>(R.id.task_edit_text).text.toString().trim()
        val dateString = findViewById<EditText>(R.id.date_edit_text).text.toString().trim()
        val timeString = findViewById<EditText>(R.id.time_edit_text).text.toString().trim()
        val categorySpinner = findViewById<Spinner>(R.id.task_category_spinner)

        val selectedCategory = categorySpinner.selectedItem.toString()
        val newCategoryEditText = findViewById<EditText>(R.id.new_category_edit_text).text.toString().trim()

        // Валидация
        if (title.isEmpty() || dateString.isEmpty() || timeString.isEmpty()) {
            showToast("Пожалуйста, заполните все поля")
            return
        }

        // Если введена новая категория, используем ее
        val category = if (newCategoryEditText.isNotEmpty()) newCategoryEditText else selectedCategory

        try {
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val date = LocalDate.parse(dateString, dateFormatter)
            val time = LocalTime.parse(timeString, timeFormatter)

            val newTask = Task(title, date, time, category)

            // Возвращаем новый task в первую Activity
            val resultIntent = Intent()
            resultIntent.putExtra("task", newTask)
            setResult(Activity.RESULT_OK, resultIntent)
            finish() // Закрываем Activity2 и возвращаемся на Activity1
        } catch (e: Exception) {
            showToast("Некорректный формат даты или времени: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showDatePicker() {
        val dateEditText = findViewById<EditText>(R.id.date_edit_text)
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            dateEditText.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
        }, year, month, day).show()
    }

    private fun showTimePicker() {
        val timeEditText = findViewById<EditText>(R.id.time_edit_text)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            timeEditText.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
        }, hour, minute, true).show()
    }
}