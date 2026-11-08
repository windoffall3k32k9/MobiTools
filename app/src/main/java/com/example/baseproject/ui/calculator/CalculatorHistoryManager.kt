package com.example.baseproject.ui.calculator

import android.content.Context
import com.example.baseproject.R
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Trình quản lý lưu trữ, truy xuất và phân loại theo ngày cho lịch sử các phép tính máy tính.
 */
object CalculatorHistoryManager {

    private const val PREFS_NAME = "calculator_history_prefs"
    private const val KEY_HISTORY = "key_calculation_history_json"
    private const val MAX_HISTORY_RECORDS = 100

    // Lưu một bản ghi phép tính mới vào SharedPreferences.
    fun addRecord(context: Context, expression: String, result: String) {
        val records = getHistory(context).toMutableList()
        records.removeAll { it.expression == expression && it.result == result }
        records.add(0, CalculationRecord(expression = expression, result = result))

        saveRecords(context, records.take(MAX_HISTORY_RECORDS))
    }

    // Đọc danh sách toàn bộ bản ghi lịch sử phép tính đã lưu.
    fun getHistory(context: Context): List<CalculationRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()

        val list = mutableListOf<CalculationRecord>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CalculationRecord(
                        id = obj.optString("id", i.toString()),
                        expression = obj.getString("expression"),
                        result = obj.getString("result"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // Xóa toàn bộ lịch sử tính toán trong SharedPreferences.
    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    // Lưu danh sách bản ghi tính toán thành chuỗi JSON vào SharedPreferences.
    private fun saveRecords(context: Context, records: List<CalculationRecord>) {
        val array = JSONArray()
        for (item in records) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("expression", item.expression)
                put("result", item.result)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    // Định dạng tiêu đề nhóm ngày thân thiện: Hôm nay, Hôm qua, hoặc định dạng dd/MM/yyyy.
    fun formatDateHeader(context: Context, timestamp: Long): String {
        val itemCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance()

        val isSameYear = itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
        val dayDiff = nowCal.get(Calendar.DAY_OF_YEAR) - itemCal.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameYear && dayDiff == 0 -> context.getString(R.string.calc_history_today)
            isSameYear && dayDiff == 1 -> context.getString(R.string.calc_history_yesterday)
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    // Định dạng thời gian giờ:phút cho từng bản ghi tính toán.
    fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
