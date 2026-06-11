package com.scholarvault.ui.pdf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scholarvault.data.model.PageAnnotation
import com.scholarvault.MainApplication
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class CoordinateData(val x: Float, val y: Float)

data class StrokeData(val points: List<CoordinateData>, val colorHex: String, val thickness: Float)

fun serializeStrokes(strokes: List<StrokeData>): String {
    val array = JSONArray()
    for (stroke in strokes) {
        val obj = JSONObject()
        obj.put("colorHex", stroke.colorHex)
        obj.put("thickness", stroke.thickness.toDouble())
        val pointsArray = JSONArray()
        for (p in stroke.points) {
            val pObj = JSONObject()
            pObj.put("x", p.x.toDouble())
            pObj.put("y", p.y.toDouble())
            pointsArray.put(pObj)
        }
        obj.put("points", pointsArray)
        array.put(obj)
    }
    return array.toString()
}

fun deserializeStrokes(jsonStr: String): List<StrokeData> {
    val list = mutableListOf<StrokeData>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val colorHex = obj.getString("colorHex")
            val thickness = obj.getDouble("thickness").toFloat()
            val pointsArray = obj.getJSONArray("points")
            val points = mutableListOf<CoordinateData>()
            for (j in 0 until pointsArray.length()) {
                val pObj = pointsArray.getJSONObject(j)
                points.add(CoordinateData(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
            }
            list.add(StrokeData(points, colorHex, thickness))
        }
    } catch (e: Exception) {}
    return list
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageNotesPanel(documentId: Int, pageIndex: Int) {
    val context = LocalContext.current
    val app = context.applicationContext as MainApplication
    val dao = app.database.pageAnnotationDao()
    val scope = rememberCoroutineScope()

    var typedNote by remember { mutableStateOf("") }
    var strokes by remember { mutableStateOf(listOf<StrokeData>()) }
    var currentStroke by remember { mutableStateOf<MutableList<CoordinateData>?>(null) }
    var annotation: PageAnnotation? by remember { mutableStateOf(null) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Text Notes, 1 = Freehand Sketch

    LaunchedEffect(documentId, pageIndex) {
        dao.getAnnotationsForPage(documentId, pageIndex).collect { list ->
            val ann = list.firstOrNull()
            annotation = ann
            typedNote = ann?.typedNote ?: ""
            strokes = ann?.handwrittenDrawingJson?.let {
                deserializeStrokes(it)
            } ?: emptyList()
        }
    }

    val saveAnnotation = {
        scope.launch {
            val json = serializeStrokes(strokes)
            val newAnn = annotation?.copy(
                typedNote = typedNote,
                handwrittenDrawingJson = json,
                lastModified = System.currentTimeMillis()
            ) ?: PageAnnotation(
                documentId = documentId,
                pageIndex = pageIndex,
                typedNote = typedNote,
                handwrittenDrawingJson = json
            )
            dao.insertAnnotation(newAnn)
        }
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
            // Adaptive Tab indicator
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    if (activeTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                divider = { }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Comment", style = MaterialTheme.typography.labelLarge) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Sketch Pad", style = MaterialTheme.typography.labelLarge) }
                )
            }
            Spacer(Modifier.height(16.dp))

            if (activeTab == 0) {
                // TEXT NOTE TAB
                OutlinedTextField(
                    value = typedNote,
                    onValueChange = { 
                        typedNote = it
                        saveAnnotation()
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    placeholder = { Text("Add your thoughts here...", style = MaterialTheme.typography.bodyLarge) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha=0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            } else {
                // SKETCH TAB
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { 
                                strokes = emptyList()
                                saveAnnotation()
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                        ) {
                            Text("Clear Canvas", fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                                .pointerInput(pageIndex) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentStroke = mutableListOf(CoordinateData(offset.x, offset.y))
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentStroke?.add(CoordinateData(change.position.x, change.position.y))
                                        },
                                        onDragEnd = {
                                            currentStroke?.let {
                                                if(it.isNotEmpty()) {
                                                    strokes = strokes + StrokeData(it.toList(), "#1A73E8", 4f)
                                                }
                                            }
                                            currentStroke = null
                                            saveAnnotation()
                                        },
                                        onDragCancel = {
                                            currentStroke = null
                                        }
                                    )
                                }
                        ) {
                            for (stroke in strokes) {
                                val path = Path()
                                if (stroke.points.isNotEmpty()) {
                                    path.moveTo(stroke.points.first().x, stroke.points.first().y)
                                    for (i in 1 until stroke.points.size) {
                                        path.lineTo(stroke.points[i].x, stroke.points[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(android.graphics.Color.parseColor(stroke.colorHex)),
                                    style = Stroke(
                                        width = stroke.thickness,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            currentStroke?.let { strokePoints ->
                                val path = Path()
                                if (strokePoints.isNotEmpty()) {
                                    path.moveTo(strokePoints.first().x, strokePoints.first().y)
                                    for (i in 1 until strokePoints.size) {
                                        path.lineTo(strokePoints[i].x, strokePoints[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(android.graphics.Color.parseColor("#1A73E8")),
                                    style = Stroke(
                                        width = 4f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
