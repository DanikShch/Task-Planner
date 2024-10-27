package com.example.taskplanner

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class UpdateTaskActivity : AppCompatActivity() {
    private lateinit var categories: MutableList<String>
    private var taskPosition: Int = -1 // Позиция задачи в списке
    private var task: Task? = null // Текущая задача для редактирования

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isTablet = resources.getBoolean(R.bool.isTablet)
        requestedOrientation = if (isTablet) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setContentView(R.layout.activity_update_task)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.update_task)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        categories = intent.getStringArrayListExtra("categories")?.toMutableList() ?: mutableListOf()
        taskPosition = intent.getIntExtra("task_position", -1)
        task = intent.getSerializableExtra("task") as? Task

        val categorySpinner: Spinner = findViewById(R.id.update_task_category_spinner)
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter




        categories = intent.getStringArrayListExtra("categories")?.toMutableList() ?: mutableListOf()




        val datePickerButton: Button = findViewById(R.id.update_date_picker_button)
        datePickerButton.setOnClickListener {
            showDatePicker()
        }

        val timePickerButton: Button = findViewById(R.id.update_time_picker_button)
        timePickerButton.setOnClickListener {
            showTimePicker()
        }

        val saveTaskButton: Button = findViewById(R.id.update_save_task_button)
        saveTaskButton.setOnClickListener {
            updateTask()
        }

        val backButton: Button = findViewById(R.id.update_back_button)
        backButton.setOnClickListener {
            finish()
        }

        loadTaskData() // Загружаем данные задачи
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTaskData() {
        task?.let {
            findViewById<EditText>(R.id.update_task_edit_text).setText(it.title)
            findViewById<EditText>(R.id.update_date_edit_text).setText(it.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            findViewById<EditText>(R.id.update_time_edit_text).setText(it.time.format(DateTimeFormatter.ofPattern("HH:mm")))

            val categorySpinner = findViewById<Spinner>(R.id.update_task_category_spinner)
            val categoryPosition = categories.indexOf(it.category)
            if (categoryPosition != -1) {
                categorySpinner.setSelection(categoryPosition)
            } else {
                findViewById<EditText>(R.id.update_new_category_edit_text).setText(it.category)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateTask() {
        // Логируем начало обновления задачи
        Log.d("UpdateTaskActivity", "Starting task update...")

        val title = findViewById<EditText>(R.id.update_task_edit_text).text.toString().trim()
        val dateString = findViewById<EditText>(R.id.update_date_edit_text).text.toString().trim()
        val timeString = findViewById<EditText>(R.id.update_time_edit_text).text.toString().trim()
        val categorySpinner = findViewById<Spinner>(R.id.update_task_category_spinner)

        val selectedCategory = categorySpinner.selectedItem.toString()
        val newCategoryEditText = findViewById<EditText>(R.id.update_new_category_edit_text).text.toString().trim()

        // Логируем значения, которые собираемся использовать
        Log.d("UpdateTaskActivity", "Title: $title, Date: $dateString, Time: $timeString, Selected Category: $selectedCategory, New Category: $newCategoryEditText")

        if (title.isEmpty() || dateString.isEmpty() || timeString.isEmpty()) {
            showToast("Пожалуйста, заполните все поля")
            Log.w("UpdateTaskActivity", "One or more fields are empty.")
            return
        }

        val category = if (newCategoryEditText.isNotEmpty()) newCategoryEditText else selectedCategory

        try {
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val date = LocalDate.parse(dateString, dateFormatter)
            val time = LocalTime.parse(timeString, timeFormatter)

            // Проверка на допустимые значения
            if (date.monthValue < 1 || date.monthValue > 12) {
                showToast("Некорректное значение месяца")
                Log.w("UpdateTaskActivity", "Invalid month value: ${date.monthValue}")
                return
            }

            val updatedTask = Task(title, date, time, category)

            // Логируем обновленную задачу
            Log.d("UpdateTaskActivity", "Updated Task: $updatedTask")

            val resultIntent = Intent().apply {
                putExtra("task", updatedTask)
                putExtra("task_position", taskPosition)
            }
            setResult(Activity.RESULT_OK, resultIntent)

            // Логируем завершение обновления задачи
            Log.d("UpdateTaskActivity", "Task update completed successfully.")
            finish()
        } catch (e: Exception) {
            showToast("Некорректный формат даты или времени: ${e.message}")
            Log.e("UpdateTaskActivity", "Error updating task: ${e.message}", e)
        }
    }



    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showDatePicker() {
        val dateEditText = findViewById<EditText>(R.id.update_date_edit_text)
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            dateEditText.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
        }, year, month, day).show()
    }

    private fun showTimePicker() {
        val timeEditText = findViewById<EditText>(R.id.update_time_edit_text)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            timeEditText.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
        }, hour, minute, true).show()
    }
}
