package com.scholarvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.data.model.SubjectEntryState
import java.util.UUID

@Composable
fun BoardMarksTable(
    evaluationSystem: String, // "Percentage" or "Grade"
    subjects: List<SubjectEntryState>,
    onSubjectsChange: (List<SubjectEntryState>) -> Unit,
    modifier: Modifier = Modifier,
    showBestOfFiveToggle: Boolean = false,
    useBestOfFive: Boolean = false,
    onUseBestOfFiveChange: ((Boolean) -> Unit)? = null
) {
    val isDark = com.scholarvault.ui.theme.LocalThemeController.current.isDarkTheme
    val textColorPrimary = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Subjects / Marks Board", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColorPrimary)
            TextButton(
                onClick = {
                    onSubjectsChange(subjects + SubjectEntryState())
                }
            ) {
                Text("+ Add Subject", fontWeight = FontWeight.SemiBold)
            }
        }
        
        if (showBestOfFiveToggle && onUseBestOfFiveChange != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Switch(
                    checked = useBestOfFive,
                    onCheckedChange = onUseBestOfFiveChange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Best-of-Five (e.g., CBSE Aggregate)", fontSize = 14.sp, color = textColorPrimary)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        if (subjects.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No subjects added. Click '+ Add Subject' above to list subjects.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            subjects.forEachIndexed { index, subject ->
                SubjectRowCard(
                    index = index + 1,
                    subject = subject,
                    evaluationSystem = evaluationSystem,
                    textColorPrimary = textColorPrimary,
                    onUpdate = { updated ->
                        val newList = subjects.toMutableList()
                        newList[index] = updated
                        onSubjectsChange(newList)
                    },
                    onRemove = {
                        val newList = subjects.toMutableList()
                        newList.removeAt(index)
                        onSubjectsChange(newList)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SummaryFooter(evaluationSystem = evaluationSystem, subjects = subjects, useBestOfFive = useBestOfFive)
    }
}

@Composable
private fun SubjectRowCard(
    index: Int,
    subject: SubjectEntryState,
    evaluationSystem: String,
    textColorPrimary: androidx.compose.ui.graphics.Color,
    onUpdate: (SubjectEntryState) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = index.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subject Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textColorPrimary)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove Subject", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Subject name field
            OutlinedTextField(
                value = subject.subjectName,
                onValueChange = { onUpdate(subject.copy(subjectName = it)) },
                placeholder = { Text("e.g. Mathematics, Physics") },
                label = { Text("Subject Name") },
                leadingIcon = { Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Grid for subject code and total/obtained marks
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = subject.subjectCode,
                    onValueChange = { onUpdate(subject.copy(subjectCode = it)) },
                    placeholder = { Text("e.g. MA101") },
                    label = { Text("Code") },
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    shape = RoundedCornerShape(12.dp)
                )

                val isMaxMarksError = subject.maxMarks.isNotBlank() && subject.maxMarks.toDoubleOrNull() == null
                OutlinedTextField(
                    value = subject.maxMarks,
                    onValueChange = { onUpdate(subject.copy(maxMarks = it)) },
                    placeholder = { Text(if (evaluationSystem == "Percentage") "100" else "10") },
                    label = { Text(if (evaluationSystem == "Percentage") "Total Marks" else "Max SGPA") },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    modifier = Modifier.weight(1.2f),
                    singleLine = true,
                    isError = isMaxMarksError,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Switch(
                    checked = subject.hasTheoryPracticalSplit,
                    onCheckedChange = { onUpdate(subject.copy(hasTheoryPracticalSplit = it)) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Has Theory / Practical Split", fontSize = 13.sp, color = textColorPrimary)
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (subject.hasTheoryPracticalSplit) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subject.theoryMaxMarks,
                        onValueChange = { onUpdate(subject.copy(theoryMaxMarks = it)) },
                        label = { Text("Theory Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = subject.theoryObtained,
                        onValueChange = {
                            val newSubject = subject.copy(theoryObtained = it)
                            val total = (newSubject.theoryObtained.toDoubleOrNull() ?: 0.0) + (newSubject.practicalObtained.toDoubleOrNull() ?: 0.0)
                            onUpdate(newSubject.copy(obtainedMarks = total.toString()))
                        },
                        label = { Text("Theory Obt.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subject.practicalMaxMarks,
                        onValueChange = { onUpdate(subject.copy(practicalMaxMarks = it)) },
                        label = { Text("Prac. Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = subject.practicalObtained,
                        onValueChange = {
                            val newSubject = subject.copy(practicalObtained = it)
                            val total = (newSubject.theoryObtained.toDoubleOrNull() ?: 0.0) + (newSubject.practicalObtained.toDoubleOrNull() ?: 0.0)
                            onUpdate(newSubject.copy(obtainedMarks = total.toString()))
                        },
                        label = { Text("Prac. Obt.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isObtainedError = subject.obtainedMarks.isNotBlank() && subject.obtainedMarks.toDoubleOrNull() == null
                OutlinedTextField(
                    value = subject.obtainedMarks,
                    onValueChange = { onUpdate(subject.copy(obtainedMarks = it)) },
                    placeholder = { Text(if (evaluationSystem == "Percentage") "85" else "9.5") },
                    label = { Text(if (evaluationSystem == "Percentage") "Total Obtained" else "Obtained SGPA") },
                    leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = if (evaluationSystem == "Grade") androidx.compose.ui.text.input.ImeAction.Next else androidx.compose.ui.text.input.ImeAction.Done),
                    modifier = Modifier.weight(1.2f),
                    singleLine = true,
                    readOnly = subject.hasTheoryPracticalSplit,
                    isError = isObtainedError,
                    shape = RoundedCornerShape(12.dp)
                )

                if (evaluationSystem == "Grade") {
                    OutlinedTextField(
                        value = subject.grade,
                        onValueChange = { onUpdate(subject.copy(grade = it)) },
                        placeholder = { Text("e.g. A+, O") },
                        label = { Text("Grade") },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = subject.creditPoints,
                        onValueChange = { onUpdate(subject.copy(creditPoints = it)) },
                        placeholder = { Text("e.g. 4") },
                        label = { Text("Credits") },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryFooter(
    evaluationSystem: String, 
    subjects: List<SubjectEntryState>, 
    useBestOfFive: Boolean = false
) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    val activeSubjects = if (useBestOfFive && subjects.size > 5) {
        subjects
            .filter { it.obtainedMarks.toDoubleOrNull() != null }
            .sortedByDescending { it.obtainedMarks.toDoubleOrNull() ?: 0.0 }
            .take(5)
    } else {
        subjects
    }

    val totalMax = activeSubjects.sumOf { it.maxMarks.toDoubleOrNull() ?: 0.0 }
    val totalObtained = activeSubjects.sumOf { it.obtainedMarks.toDoubleOrNull() ?: 0.0 }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Automatic Performance Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = contentColor)
            if (useBestOfFive && subjects.size > 5) {
                Text("Showing top 5 subjects by marks (CBSE aggregate rule)", fontSize = 12.sp, color = contentColor.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (evaluationSystem == "Percentage") {
                val percentage = if (totalMax > 0) (totalObtained / totalMax) * 100 else 0.0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Obtained Marks:", fontSize = 14.sp, color = contentColor.copy(alpha = 0.8f))
                    Text("${totalObtained.toInt()} / ${totalMax.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Overall Percentage:", fontSize = 14.sp, color = contentColor.copy(alpha = 0.8f))
                    Text(String.format("%.2f%%", percentage), fontWeight = FontWeight.Black, fontSize = 18.sp, color = contentColor)
                }
            } else {
                val weightedSum = activeSubjects.sumOf { s ->
                    val gp = s.obtainedMarks.toDoubleOrNull() ?: 0.0
                    val credits = s.creditPoints.toDoubleOrNull() ?: 1.0
                    gp * credits
                }
                val totalCredits = activeSubjects.sumOf { s ->
                    if ((s.obtainedMarks.toDoubleOrNull() ?: 0.0) > 0)
                        s.creditPoints.toDoubleOrNull() ?: 1.0
                    else 0.0
                }
                val cgpa = if (totalCredits > 0) weightedSum / totalCredits else 0.0
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Calculated SGPA:", fontSize = 14.sp, color = contentColor.copy(alpha = 0.8f))
                    Text(String.format("%.2f", totalObtained), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Calculated CGPA:", fontSize = 14.sp, color = contentColor.copy(alpha = 0.8f))
                    Text(String.format("%.2f", cgpa), fontWeight = FontWeight.Black, fontSize = 18.sp, color = contentColor)
                }
                Text("Calculated using entered credit points. Verify against your official transcript.", fontSize = 11.sp, color = contentColor.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
