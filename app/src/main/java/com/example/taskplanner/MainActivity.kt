package com.example.taskplanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.ScaleGestureDetector
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences
    private val taskList = mutableListOf<Task>()
    private val categories = mutableListOf("Все", "Работа", "Учеба")
    private var filteredTaskList = mutableListOf<Task>()

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var categorySpinner: Spinner
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var notificationHelper: NotificationHelper

    private val updateTaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedTask = result.data?.getSerializableExtra("task") as? Task
            val taskPosition = result.data?.getIntExtra("task_position", -1) ?: -1

            if (updatedTask != null && taskPosition in taskList.indices) {
                Log.d("MainActivity", "Updating task at position $taskPosition: $updatedTask")
                taskList[taskPosition] = updatedTask

                // Проверка разрешения на установку уведомлений
                checkNotificationPermission(updatedTask)

                if (!categories.contains(updatedTask.category)) {
                    categories.add(updatedTask.category)
                }
                saveData() // Сохраняем данные
                updateFilteredTasks() // Обновляем список отображаемых задач

            } else {
                Log.e("MainActivity", "Failed to update task. Updated task: $updatedTask, Position: $taskPosition")
            }
        }
    }

    private val createTaskLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = result.data?.getSerializableExtra("task") as? Task
                task?.let {
                    taskList.add(it)
                    Log.d("MainActivity", "Opening UpdateTaskActivity with Task: ${task.title}, Date: ${task.date}, Time: ${task.time}, Category: ${task.category}")

                    // Проверка разрешения на установку уведомлений
                    checkNotificationPermission(it)

                    if (!categories.contains(it.category)) {
                        categories.add(it.category)
                    }
                    updateFilteredTasks()
                    saveData()
                }
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isTablet = resources.getBoolean(R.bool.isTablet)
        if (isTablet) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel() // Создаем канал уведомлений

        preferences = getSharedPreferences("task_preferences", MODE_PRIVATE)

        val recyclerView: RecyclerView = findViewById(R.id.task_recycler_view)
        taskAdapter = TaskAdapter(filteredTaskList, { position ->
            // Открытие UpdateTaskActivity при клике на задачу
            val task = filteredTaskList[position]
            Log.d("MainActivity", "Opening UpdateTaskActivity with Task: ${task.title}, Date: ${task.date}, Time: ${task.time}, Category: ${task.category}")
            val intent = Intent(this, UpdateTaskActivity::class.java)
            intent.putExtra("task", task)
            val index = taskList.indexOf(task)
            intent.putExtra("task_position", index)
            intent.putStringArrayListExtra("categories", ArrayList(categories)) // Передаем категории
            updateTaskLauncher.launch(intent)
        }, { position ->
            removeTask(position) // Удаление задачи по позиции
        })

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = taskAdapter

        recyclerView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            false
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                removeTask(position)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        categorySpinner = findViewById(R.id.category_spinner)
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateFilteredTasks()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val deleteCategoryButton: Button = findViewById(R.id.delete_category_button)
        deleteCategoryButton.setOnClickListener {
            val selectedCategory = categorySpinner.selectedItem.toString()
            if (selectedCategory != "Все") {
                showDeleteConfirmationDialog(selectedCategory)
            } else {
                Toast.makeText(this, "Нельзя удалить категорию 'Все'", Toast.LENGTH_SHORT).show()
            }
        }

        val addTaskButton: Button = findViewById(R.id.add_task_button)
        addTaskButton.setOnClickListener {
            val intent = Intent(this, CreateTaskActivity::class.java)
            intent.putStringArrayListExtra("categories", ArrayList(categories))
            createTaskLauncher.launch(intent)
        }

        loadData()

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (detector.scaleFactor < 0.9f) {
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    startActivity(intent)
                    return true
                }
                return false
            }
        })
    }

    private fun showDeleteConfirmationDialog(category: String) {
        AlertDialog.Builder(this)
            .setTitle("Подтверждение удаления")
            .setMessage("Вы хотите удалить все задачи категории \"$category\"?")
            .setPositiveButton("Да") { _, _ -> deleteTasksByCategory(category) }
            .setNegativeButton("Нет") { _, _ -> changeTasksCategoryToDefault(category) }
            .create()
            .show()
    }

    private fun deleteTasksByCategory(category: String) {
        taskList.removeAll { it.category == category }
        categories.remove(category)
        saveData()
        updateCategorySpinner()
        updateFilteredTasks()
    }

    private fun changeTasksCategoryToDefault(category: String) {
        taskList.forEach { task ->
            if (task.category == category) {
                task.category = "Все"
            }
        }
        categories.remove(category)
        saveData()
        updateCategorySpinner()
        updateFilteredTasks()
    }

    private fun updateCategorySpinner() {
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateFilteredTasks() {
        val selectedCategory = categorySpinner.selectedItem.toString()
        filteredTaskList.clear()
        if (selectedCategory == "Все") {
            filteredTaskList.addAll(taskList)
        } else {
            filteredTaskList.addAll(taskList.filter { it.category == selectedCategory })
        }
        // Сортировка задач по дате и времени
        filteredTaskList.sortWith(compareBy<Task> { it.date }.thenBy { it.time })
        taskAdapter.notifyDataSetChanged()
    }

    private fun removeTask(position: Int) {
        val taskToRemove = filteredTaskList[position]
        taskList.remove(taskToRemove)
        filteredTaskList.removeAt(position)
        taskAdapter.notifyItemRemoved(position)
        saveData()
    }

    private fun saveData() {
        val editor = preferences.edit()
        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
            .create()
        editor.putString("task_list", gson.toJson(taskList))
        editor.putStringSet("categories", categories.toSet())
        editor.apply()

    }

    private fun loadData() {
        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
            .create()
        val taskListJson = preferences.getString("task_list", null)
        if (taskListJson != null) {
            val type = object : TypeToken<MutableList<Task>>() {}.type
            taskList.clear()
            taskList.addAll(gson.fromJson(taskListJson, type) ?: emptyList())
        }
        val categorySet = preferences.getStringSet("categories", null)
        if (categorySet != null) {
            categories.clear()
            categories.add("Все")
            categories.addAll(categorySet.filter { it != "Все" })
        }
        updateFilteredTasks()
    }

    // Константа кода запроса разрешения для уведомлений
    private val NOTIFICATION_PERMISSION_CODE = 1001

    private fun checkNotificationPermission(task: Task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Запросить разрешение на отправку уведомлений
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
                checkAndScheduleNotification(task)
            } else {
                // Разрешение предоставлено, проверяем и устанавливаем будильник
                checkAndScheduleNotification(task)
            }
        } else {
            checkAndScheduleNotification(task)
        }
    }



    // Метод для проверки разрешения и установки уведомления
    private fun checkAndScheduleNotification(task: Task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                // Разрешение предоставлено, устанавливаем уведомление
                notificationHelper.scheduleNotification(task)
            } else {
                // Разрешение не предоставлено, направляем пользователя в настройки
                Toast.makeText(this, "Please enable exact alarm permission in settings.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                notificationHelper.scheduleNotification(task)
            }
        } else {
            // Устанавливаем уведомление без проверки разрешения для Android < 5
            notificationHelper.scheduleNotification(task)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Разрешение на уведомления предоставлено
                    //checkAndScheduleNotification(/* передайте вашу задачу */)
                } else {
                    Toast.makeText(this, "Permission denied to post notifications", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Разрешение на точные будильники предоставлено
                    //checkAndScheduleNotification(/* передайте вашу задачу */)
                } else {
                    Toast.makeText(this, "Permission denied to schedule exact alarms", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    companion object {
        private const val REQUEST_CODE = 1001 // Код запроса для разрешения
    }

}
