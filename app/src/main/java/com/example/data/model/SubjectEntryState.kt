package com.scholarvault.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SubjectEntryState(
    val id: String = UUID.randomUUID().toString(),
    val subjectName: String = "",
    val subjectCode: String = "",
    val maxMarks: String = "",
    val obtainedMarks: String = "",
    val grade: String = "",
    val creditPoints: String = "",
    val hasTheoryPracticalSplit: Boolean = false,
    val theoryMaxMarks: String = "",
    val theoryObtained: String = "",
    val practicalMaxMarks: String = "",
    val practicalObtained: String = "",
    val isOptionalSubject: Boolean = false
)

object SubjectSerializer {
    fun serialize(subjects: List<SubjectEntryState>): String {
        val array = JSONArray()
        subjects.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("subjectName", it.subjectName)
            obj.put("subjectCode", it.subjectCode)
            obj.put("maxMarks", it.maxMarks)
            obj.put("obtainedMarks", it.obtainedMarks)
            obj.put("grade", it.grade)
            obj.put("creditPoints", it.creditPoints)
            obj.put("hasTheoryPracticalSplit", it.hasTheoryPracticalSplit)
            obj.put("theoryMaxMarks", it.theoryMaxMarks)
            obj.put("theoryObtained", it.theoryObtained)
            obj.put("practicalMaxMarks", it.practicalMaxMarks)
            obj.put("practicalObtained", it.practicalObtained)
            obj.put("isOptionalSubject", it.isOptionalSubject)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserialize(json: String?): List<SubjectEntryState> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val list = mutableListOf<SubjectEntryState>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SubjectEntryState(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        subjectName = obj.optString("subjectName", ""),
                        subjectCode = obj.optString("subjectCode", ""),
                        maxMarks = obj.optString("maxMarks", ""),
                        obtainedMarks = obj.optString("obtainedMarks", ""),
                        grade = obj.optString("grade", ""),
                        creditPoints = obj.optString("creditPoints", ""),
                        hasTheoryPracticalSplit = obj.optBoolean("hasTheoryPracticalSplit", false),
                        theoryMaxMarks = obj.optString("theoryMaxMarks", ""),
                        theoryObtained = obj.optString("theoryObtained", ""),
                        practicalMaxMarks = obj.optString("practicalMaxMarks", ""),
                        practicalObtained = obj.optString("practicalObtained", ""),
                        isOptionalSubject = obj.optBoolean("isOptionalSubject", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
