package com.scholarvault.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "academic_items")
data class AcademicItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: String,
    @ColumnInfo(name = "timeline_date") val timelineDate: Date,
    @ColumnInfo(name = "institution_name") val institutionName: String? = null,
    @ColumnInfo(name = "conducting_body") val conductingBody: String? = null,
    @ColumnInfo(name = "conducting_year") val conductingYear: String? = null,
    @ColumnInfo(name = "class_number") val classNumber: String? = null,
    @ColumnInfo(name = "degree_type") val degreeType: String? = null,
    @ColumnInfo(name = "structure_type") val structureType: String? = null,
    @ColumnInfo(name = "evaluation_system") val evaluationSystem: String? = null,
    @ColumnInfo(name = "subjects_json") val subjectsJson: String? = null,
    
    // --- NEW FIELDS ---
    @ColumnInfo(name = "board_roll_number") val boardRollNumber: String? = null,
    @ColumnInfo(name = "board_roll_code") val boardRollCode: String? = null,
    @ColumnInfo(name = "board_registration_number") val boardRegistrationNumber: String? = null,
    @ColumnInfo(name = "marksheet_index_number") val marksheetIndexNumber: String? = null,
    @ColumnInfo(name = "school_code") val schoolCode: String? = null,
    @ColumnInfo(name = "medium_of_instruction") val mediumOfInstruction: String? = null,
    @ColumnInfo(name = "stream") val stream: String? = null,
    @ColumnInfo(name = "use_best_of_five") val useBestOfFive: Boolean = false,
    @ColumnInfo(name = "passing_status") val passingStatus: String? = null,
    @ColumnInfo(name = "nta_percentile") val ntaPercentile: Double? = null,
    @ColumnInfo(name = "all_india_rank") val allIndiaRank: Int? = null,
    @ColumnInfo(name = "category_rank") val categoryRank: Int? = null,
    @ColumnInfo(name = "domicile_state") val domicileState: String? = null,
    @ColumnInfo(name = "institution_district") val institutionDistrict: String? = null,
    
    // --- NEW REFACTORING FIELDS ---
    @ColumnInfo(name = "grade_conversion_formula") val gradeConversionFormula: String? = null,
    @ColumnInfo(name = "best_of_n") val bestOfN: Int? = null
)
