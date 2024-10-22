package com.example.taskplanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val tasks: MutableList<Task>, // Сделаем список изменяемым
    private val onTaskMarked: (Int) -> Unit // Добавляем callback для удаления задачи
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskTextView: TextView = itemView.findViewById(R.id.taskTextView)
        val markButton: Button = itemView.findViewById(R.id.markButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.taskTextView.text = task.title

        // Устанавливаем слушатель для кнопки отметки
        holder.markButton.setOnClickListener {
            onTaskMarked(position) // Вызываем callback с позицией задачи
        }
    }

    override fun getItemCount(): Int {
        return tasks.size
    }
}