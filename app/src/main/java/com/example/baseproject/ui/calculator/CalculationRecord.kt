package com.example.baseproject.ui.calculator

import java.util.UUID

/**
 * Bản ghi lưu trữ một phép tính trong lịch sử gồm biểu thức, kết quả và mốc thời gian thực hiện.
 */
data class CalculationRecord(
    val id: String = UUID.randomUUID().toString(),
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)
