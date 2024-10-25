package com.example.taskplanner

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences
    private val taskList = mutableListOf<Task>() // Список задач
    private val categories = mutableListOf("Все", "Работа", "Учеба") // Список категорий
    private var filteredTaskList = mutableListOf<Task>() // Список для отображения задач в зависимости от фильтра

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var categorySpinner: Spinner
    private lateinit var scaleGestureDetector: ScaleGestureDetector // Для обработки Pinch жеста

    private val createTaskLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = result.data?.getSerializableExtra("task") as? Task
                task?.let {
                    taskList.add(it)
                    if (!categories.contains(it.category)) {
                        categories.add(it.category)
                    }
                    updateFilteredTasks() // Обновляем список отображаемых задач
                    saveData() // Сохраняем данные
                }
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Включаем режим Edge-to-Edge
        val isTablet = resources.getBoolean(R.bool.isTablet)
        if (isTablet) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setContentView(R.layout.activity_main)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom) // Устанавливаем отступы
            insets // Возвращаем insets
        }

        // Инициализация SharedPreferences
        preferences = getSharedPreferences("task_preferences", MODE_PRIVATE)

        // Инициализация RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.task_recycler_view)
        taskAdapter = TaskAdapter(filteredTaskList) { position ->
            removeTask(position) // Удаляем задачу по позиции
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = taskAdapter

        // Устанавливаем слушатель касаний на RecyclerView
        recyclerView.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event) // Передаем событие в ScaleGestureDetector
            false // Возвращаем false, чтобы RecyclerView продолжал обрабатывать остальные события
        }

        // Добавляем ItemTouchHelper для обработки свайпов
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false // Не обрабатываем движение
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                removeTask(position) // Удаляем задачу по свайпу
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Инициализация Spinner с категориями
        categorySpinner = findViewById(R.id.category_spinner)
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        // Устанавливаем слушатель для выбора категории
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateFilteredTasks()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Ничего не делаем
            }
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

        // Кнопка для создания новой задачи
        val addTaskButton: Button = findViewById(R.id.add_task_button)
        addTaskButton.setOnClickListener {
            val intent = Intent(this, CreateTaskActivity::class.java)
            // Передаём лист данных
            intent.putStringArrayListExtra("categories", ArrayList(categories))
            // Запускаем вторую активность с ожиданием результата
            createTaskLauncher.launch(intent)
        }

        // Загрузка данных после инициализации всех представлений
        loadData()

        // Инициализация ScaleGestureDetector для обработки Pinch
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (detector.scaleFactor < 0.9f) {
                    // Если жест "сжатие" (pinch), то открываем LoginActivity
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
            .setPositiveButton("Да") { dialog: DialogInterface, _: Int ->
                deleteTasksByCategory(category)
            }
            .setNegativeButton("Нет") { dialog: DialogInterface, _: Int ->
                changeTasksCategoryToDefault(category)
            }
            .create()
            .show()
    }

    private fun deleteTasksByCategory(category: String) {
        taskList.removeAll { it.category == category } // Удаляем все задачи данной категории
        categories.remove(category) // Удаляем категорию
        saveData() // Сохраняем данные
        updateCategorySpinner() // Обновляем Spinner
        updateFilteredTasks() // Обновляем список задач

    }

    private fun changeTasksCategoryToDefault(category: String) {
        // Меняем категорию всех задач данной категории на "Все"
        taskList.forEach { task ->
            if (task.category == category) {
                task.category = "Все"
            }
        }

        // Удаляем категорию из списка категорий
        categories.remove(category)

        // Сохраняем изменения
        saveData() // Сохраняем данные
        updateCategorySpinner() // Обновляем Spinner, чтобы отобразить изменения
        updateFilteredTasks() // Обновляем список задач

    }

    private fun updateCategorySpinner() {
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter
    }

    private fun updateFilteredTasks() {
        val selectedCategory = categorySpinner.selectedItem.toString()
        filteredTaskList.clear()

        if (selectedCategory == "Все") {
            filteredTaskList.addAll(taskList) // Добавляем все задачи
        } else {
            filteredTaskList.addAll(taskList.filter { it.category == selectedCategory }) // Фильтруем по категории
        }

        taskAdapter.notifyDataSetChanged() // Уведомляем адаптер об изменениях
    }

    private fun removeTask(position: Int) {
        val taskToRemove = filteredTaskList[position]
        taskList.remove(taskToRemove) // Удаляем задачу из оригинального списка
        filteredTaskList.removeAt(position) // Удаляем задачу из фильтрованного списка
        taskAdapter.notifyItemRemoved(position) // Уведомляем адаптер об удалении
        saveData() // Сохраняем данные после удаления
    }

    private fun saveData() {
        val editor = preferences.edit()
        val gson = Gson()
        val taskListJson = gson.toJson(taskList)
        editor.putString("task_list", taskListJson)
        editor.putStringSet("categories", categories.toSet())
        editor.apply()
    }

    private fun loadData() {
        val gson = Gson()
        val taskListJson = preferences.getString("task_list", null)

        // Проверяем, есть ли данные для загрузки
        if (taskListJson != null) {
            Log.d("MainActivity", "Загрузка задач из SharedPreferences")
            val type = object : TypeToken<MutableList<Task>>() {}.type
            taskList.clear()
            taskList.addAll(gson.fromJson(taskListJson, type) ?: emptyList())
        } else {
            Log.d("MainActivity", "Нет сохраненных задач")
        }

        // Загружаем категории
        val categorySet = preferences.getStringSet("categories", null)
        if (categorySet != null) {
            categories.clear()
            categories.add("Все") // Убедитесь, что "Все" всегда на первом месте
            categories.addAll(categorySet.filter { it != "Все" }) // Добавляем остальные категории, исключая "Все"
        } else {
            Log.d("MainActivity", "Нет сохраненных категорий")
        }

        updateFilteredTasks() // Обновляем отображаемые задачи после загрузки
    }
}
