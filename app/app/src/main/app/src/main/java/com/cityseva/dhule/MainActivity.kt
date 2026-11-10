
package com.cityseva.dhule

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Green = Color(0xFF166534)

data class Service(
    val english: String,
    val hindi: String,
    val emoji: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CitySevaApp()
        }
    }
}

@Composable
fun CitySevaApp() {
    val context = LocalContext.current

    var hindi by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Service?>(null) }
    var customer by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    val bookings = remember { mutableStateListOf<String>() }

    val services = listOf(
        Service("Party Decoration", "पार्टी सजावट", "🎈"),
        Service("House Cleaning", "घर की सफाई", "🧹"),
        Service("Washing Service", "कपड़े धोना", "👕"),
        Service("Painting Contract", "पेंटिंग", "🎨"),
        Service("AC Repair", "एसी रिपेयर", "❄️"),
        Service("Plumbing & Electrical", "प्लंबिंग और इलेक्ट्रिकल", "🔧")
    )

    MaterialTheme(
        colorScheme = lightColorScheme(primary = Green)
    ) {
        Scaffold(
            topBar = {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "CitySeva",
                            color = Green,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Dhule, Maharashtra")
                    }

                    TextButton(onClick = { hindi = !hindi }) {
                        Text(if (hindi) "English" else "हिन्दी")
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Green
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                if (hindi) "धुले की अपनी सर्विस ऐप"
                                else "Your city's service partner",
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (hindi) "घर की सेवाएं अब आसान"
                                else "Book trusted local services",
                                color = Color.White
                            )
                        }
                    }
                }

                item {
                    Text(
                        if (hindi) "अपनी सेवा चुनें"
                        else "Choose your service",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(services) { service ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = service },
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(15.dp)
                        ) {
                            Text(service.emoji, fontSize = 30.sp)
                            Column {
                                Text(
                                    if (hindi) service.hindi else service.english,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (hindi) "बुक करने के लिए टैप करें"
                                    else "Tap to book",
                                    color = Green
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        if (hindi) "मेरी बुकिंग" else "My Bookings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                items(bookings) { booking ->
                    Text("✓ $booking")
                }
            }
        }

        selected?.let { service ->
            AlertDialog(
                onDismissRequest = { selected = null },
                title = {
                    Text(if (hindi) service.hindi else service.english)
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = customer,
                            onValueChange = { customer = it },
                            label = {
                                Text(if (hindi) "नाम" else "Your name")
                            }
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = {
                                if (it.length <= 10 && it.all(Char::isDigit)) {
                                    phone = it
                                }
                            },
                            label = {
                                Text(if (hindi) "मोबाइल नंबर" else "Mobile number")
                            }
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = {
                                Text(if (hindi) "पता" else "Address in Dhule")
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (
                                customer.isNotBlank() &&
                                phone.matches(Regex("[6-9][0-9]{9}")) &&
                                address.isNotBlank()
                            ) {
                                val message = """
                                    New CitySeva Dhule Booking

                                    Service: ${service.english}
                                    Customer Name: $customer
                                    Mobile Number: $phone
                                    Address: $address
                                """.trimIndent()

                                val whatsappUrl =
                                    "https://wa.me/919511926356?text=${Uri.encode(message)}"

                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(whatsappUrl)
                                )

                                context.startActivity(intent)

                                bookings.add("${service.english} – $customer")
                                selected = null
                                customer = ""
                                phone = ""
                                address = ""
                            }
                        }
                    ) {
                        Text(if (hindi) "बुक करें" else "Book now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selected = null }) {
                        Text(if (hindi) "रद्द करें" else "Cancel")
                    }
                }
            )
        }
    }
}
