package com.example.taskplanner

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime

data class Task(
    var title: String,
    var date: LocalDate,
    var time: LocalTime,
    var category: String
) : Serializable