package com.example.taskplanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val onTaskClick: (Int) -> Unit,  // Callback для открытия UpdateTaskActivity
    private val onTaskMarked: (Int) -> Unit  // Callback для отметки задачи
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

        // Обработка клика по элементу задачи для открытия UpdateTaskActivity
        holder.itemView.setOnClickListener {
            onTaskClick(position) // Вызываем callback с позицией задачи
        }

        // Обработка клика по кнопке отметки
        holder.markButton.setOnClickListener {
            onTaskMarked(position) // Вызываем callback для удаления задачи
        }
    }

    override fun getItemCount(): Int {
        return tasks.size
    }
}
