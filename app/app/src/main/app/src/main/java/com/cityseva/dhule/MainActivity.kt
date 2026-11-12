package com.cityseva.dhule

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

private val Green = Color(0xFF166534)

data class Service(
    val english: String,
    val hindi: String,
    val emoji: String,
    val gallery: List<Int>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CitySevaApp(this)
        }
    }
}

@Composable
fun CitySevaApp(activity: MainActivity) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    var loggedIn by remember {
        mutableStateOf(auth.currentUser != null)
    }
    var hindi by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember {
        mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null)
    }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    var selected by remember { mutableStateOf<Service?>(null) }
    var customer by remember { mutableStateOf("") }
    var bookingPhone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    val bookings = remember { mutableStateListOf<String>() }

    fun handleCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                loading = false
                if (task.isSuccessful) {
                    loggedIn = true
                    message = ""
                } else {
                    message = task.exception?.localizedMessage
                        ?: "OTP verification failed"
                }
            }
    }

    fun sendOtp(resend: Boolean = false) {
        if (!phone.matches(Regex("[6-9][0-9]{9}"))) {
            message = "Enter a valid 10-digit mobile number"
            return
        }

        loading = true
        message = ""

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(
                credential: PhoneAuthCredential
            ) {
                handleCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                loading = false
                message = e.localizedMessage ?: "OTP sending failed"
            }

            override fun onCodeSent(
                id: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                super.onCodeSent(id, token)
                verificationId = id
                resendToken = token
                loading = false
                message = "OTP sent successfully"
            }
        }

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phone")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resend && resendToken != null) {
            builder.setForceResendingToken(resendToken!!)
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    val services = listOf(
        Service(
            "Party Decoration", "पार्टी सजावट", "🎈",
            listOf(R.drawable.party_01, R.drawable.party_02, R.drawable.party_03, R.drawable.party_04, R.drawable.party_05)
        ),
        Service(
            "House Cleaning", "घर की सफाई", "🧹",
            listOf(R.drawable.cleaning_01, R.drawable.cleaning_02, R.drawable.cleaning_03, R.drawable.cleaning_04, R.drawable.cleaning_05)
        ),
        Service(
            "Washing Service", "कपड़े धोना", "👕",
            listOf(R.drawable.laundry_01, R.drawable.laundry_02, R.drawable.laundry_03, R.drawable.laundry_04, R.drawable.laundry_05)
        ),
        Service(
            "Painting Contract", "पेंटिंग", "🎨",
            listOf(R.drawable.painting_01, R.drawable.painting_02, R.drawable.painting_03, R.drawable.painting_04, R.drawable.painting_05)
        ),
        Service(
            "AC Repair", "एसी रिपेयर", "❄️",
            listOf(R.drawable.ac_01, R.drawable.ac_02, R.drawable.ac_03, R.drawable.ac_04, R.drawable.ac_05)
        ),
        Service(
            "Plumbing & Electrical", "प्लंबिंग और इलेक्ट्रिकल", "🔧",
            listOf(R.drawable.plumbing_01, R.drawable.plumbing_02, R.drawable.plumbing_03, R.drawable.plumbing_04, R.drawable.plumbing_05)
        )
    )

    MaterialTheme(colorScheme = lightColorScheme(primary = Green)) {
        if (!loggedIn) {
            Scaffold { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "CitySeva",
                        fontSize = 32.sp,
                        color = Green,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Dhule, Maharashtra")
                    Spacer(Modifier.height(30.dp))

                    Text(
                        "Customer Login",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            if (it.length <= 10 && it.all(Char::isDigit)) {
                                phone = it
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Mobile Number") },
                        prefix = { Text("+91 ") },
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    if (verificationId == null) {
                        Button(
                            onClick = { sendOtp() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading
                        ) {
                            Text(if (loading) "Sending..." else "Send OTP")
                        }
                    } else {
                        OutlinedTextField(
                            value = otp,
                            onValueChange = {
                                if (it.length <= 6 && it.all(Char::isDigit)) {
                                    otp = it
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Enter 6-digit OTP") },
                            singleLine = true
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val id = verificationId
                                if (otp.length == 6 && id != null) {
                                    loading = true
                                    val credential =
                                        PhoneAuthProvider.getCredential(id, otp)
                                    handleCredential(credential)
                                } else {
                                    message = "Enter a valid 6-digit OTP"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading
                        ) {
                            Text(if (loading) "Verifying..." else "Verify OTP")
                        }

                        TextButton(
                            onClick = { sendOtp(true) },
                            enabled = !loading
                        ) {
                            Text("Resend OTP")
                        }
                    }

                    if (message.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(message, color = Green)
                    }
                }
            }
        } else {
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
                        Row {
                            TextButton(onClick = { hindi = !hindi }) {
                                Text(if (hindi) "English" else "हिन्दी")
                            }
                            TextButton(onClick = {
                                auth.signOut()
                                loggedIn = false
                                verificationId = null
                                otp = ""
                                phone = ""
                            }) {
                                Text("Logout")
                            }
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
                                Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = service.gallery.first()),
                                    contentDescription = service.english,
                                    modifier = Modifier.size(width = 104.dp, height = 88.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (hindi) service.hindi else service.english,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        if (hindi) "बुक करने के लिए टैप करें"
                                        else "Tap to book",
                                        color = Green,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
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
                            Text(
                                if (hindi) "फोटो देखें" else "Work photos"
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                service.gallery.forEachIndexed { index, imageRes ->
                                    Image(
                                        painter = painterResource(id = imageRes),
                                        contentDescription = "${service.english} work sample ${index + 1}",
                                        modifier = Modifier.size(width = 150.dp, height = 108.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(
                                value = customer,
                                onValueChange = { customer = it },
                                label = {
                                    Text(if (hindi) "नाम" else "Your name")
                                }
                            )
                            OutlinedTextField(
                                value = bookingPhone,
                                onValueChange = {
                                    if (it.length <= 10 && it.all(Char::isDigit)) {
                                        bookingPhone = it
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
                        Button(onClick = {
                            if (
                                customer.isNotBlank() &&
                                bookingPhone.matches(Regex("[6-9][0-9]{9}")) &&
                                address.isNotBlank()
                            ) {
                                val bookingMessage = """
                                    New CitySeva Dhule Booking
                                    Service: ${service.english}
                                    Customer Name: $customer
                                    Mobile Number: $bookingPhone
                                    Address: $address
                                """.trimIndent()

                                val whatsappUrl =
                                    "https://wa.me/919511926356?text=${Uri.encode(bookingMessage)}"

                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                                )

                                bookings.add("${service.english} – $customer")
                                selected = null
                                customer = ""
                                bookingPhone = ""
                                address = ""
                            }
                        }) {
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
}

                 
                            
