package com.example.digiarogya

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppointmentScreen( onBackPress: () -> Unit) {

    var showDialog by remember { mutableStateOf(false) }
    val appointments = remember {
        mutableStateListOf(
            "Today at 2:30 PM\nDr. Kari, Pediatrics",
            "Tomorrow at 4:00 PM\nDr. Kari, Pediatrics",
            "Thurs, Mar 24 at 5:00 PM\nDr. Kari, Pediatrics"
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFCCF6F5))
                    .padding(top = 50.dp,
                        start = 25.dp,
                        end = 25.dp,
                        bottom = 25.dp
                        ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.clickable { onBackPress() }
                )

                Spacer(modifier = Modifier.width(12.dp))
                Text("Appointments", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFCCF6F5))
                .padding(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    appointments.forEach {
                        Text(text = it, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .clickable { showDialog = true }
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add new Appointment", fontWeight = FontWeight.Medium)
                }
            }

            if (showDialog) {
                AddAppointmentDialog(
                    onDismiss = { showDialog = false },
                    onAdd = { dateTime, doctor ->
                        appointments.add("$dateTime\nDr. $doctor")
                        showDialog = false
                    }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddAppointmentDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var doctorName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Text(
                "Add",
                modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                        val formattedTime = selectedTime.format(DateTimeFormatter.ofPattern("h:mm a"))
                        onAdd("$formattedDate at $formattedTime", doctorName)
                    },
                color = Color.Blue
            )
        },
        dismissButton = {
            Text(
                "Cancel",
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onDismiss() },
                color = Color.Gray
            )
        },
        title = { Text("Add Appointment") },
        text = {
            Column {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Doctor's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Date: ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                    modifier = Modifier
                        .clickable {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                },
                                selectedDate.year,
                                selectedDate.monthValue - 1,
                                selectedDate.dayOfMonth
                            ).show()
                        }
                        .padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Time: ${selectedTime.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                    modifier = Modifier
                        .clickable {
                            val calendar = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _: TimePicker, hour: Int, minute: Int ->
                                    selectedTime = LocalTime.of(hour, minute)
                                },
                                selectedTime.hour,
                                selectedTime.minute,
                                false
                            ).show()
                        }
                        .padding(vertical = 8.dp)
                )
            }
        }
    )
}


@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun AppointmentScreenPreview() {
    AppointmentScreen( onBackPress = {})
}