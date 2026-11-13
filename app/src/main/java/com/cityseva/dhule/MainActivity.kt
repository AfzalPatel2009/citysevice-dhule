
package com.cityseva.dhule

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

private val Navy = Color(0xFF14265C)
private val Blue = Color(0xFF2855D9)
private val Orange = Color(0xFFFF8A24)
private val Pale = Color(0xFFF4F6FC)
private val Ink = Color(0xFF18213B)

private const val WHATSAPP_NUMBER = "919511926356"

private data class ServiceItem(
    val name: String,
    val subtitle: String,
    val emoji: String,
    val price: String,
    val images: List<Int>
)

private data class Booking(
    val service: String,
    val customer: String,
    val mobile: String,
    val address: String,
    val date: String,
    val status: String
)

private enum class Page {
    HOME, ROLE, LOGIN, PENDING,
    GALLERY, BOOKING, BOOKINGS, ACCOUNT, SETTINGS
}

private enum class UserRole {
    CUSTOMER, RETAILER, ADMIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CitySevaApp()
        }
    }
}

@Composable
private fun CitySevaApp() {

    val context = LocalContext.current

    val services = listOf(
        ServiceItem("Party Decoration", "Birthdays, weddings & events", "🎈", "Packages from ₹999", listOf(R.drawable.party_01, R.drawable.party_02, R.drawable.party_03, R.drawable.party_04, R.drawable.party_05)),
        ServiceItem("House Cleaning", "Deep cleaning & regular cleaning", "🧹", "Packages from ₹499", listOf(R.drawable.cleaning_01, R.drawable.cleaning_02, R.drawable.cleaning_03, R.drawable.cleaning_04, R.drawable.cleaning_05)),
        ServiceItem("Washing Service", "Laundry, ironing & pickup", "👕", "Packages from ₹99", listOf(R.drawable.laundry_01, R.drawable.laundry_02, R.drawable.laundry_03, R.drawable.laundry_04, R.drawable.laundry_05)),
        ServiceItem("Painting Contract", "Home and office painting", "🎨", "Get a free estimate", listOf(R.drawable.painting_01, R.drawable.painting_02, R.drawable.painting_03, R.drawable.painting_04, R.drawable.painting_05)),
        ServiceItem("AC Repair", "Service, repair & installation", "❄️", "Inspection available", listOf(R.drawable.ac_01, R.drawable.ac_02, R.drawable.ac_03, R.drawable.ac_04, R.drawable.ac_05)),
        ServiceItem("Plumbing & Electrical", "Trusted home maintenance", "🔧", "Book a technician", listOf(R.drawable.plumbing_01, R.drawable.plumbing_02, R.drawable.plumbing_03, R.drawable.plumbing_04, R.drawable.plumbing_05))
    )

    var page by remember { mutableStateOf(Page.HOME) }
    var role by remember { mutableStateOf(UserRole.CUSTOMER) }
    var selected by remember { mutableStateOf(services.first()) }

    var mobile by remember { mutableStateOf("") }
    var bookingMobile by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var bookings by remember { mutableStateOf(listOf<Booking>()) }
    var search by remember { mutableStateOf("") }
    var sessionCheckComplete by remember { mutableStateOf(false) }

    // Restore the Firebase Authentication session when the app is reopened.
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            sessionCheckComplete = true
        } else {
            FirebaseFirestore.getInstance().collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val savedName = doc.getString("name") ?: ""
                        val savedPhone = doc.getString("phoneNumber") ?: ""
                        val savedRole = doc.getString("role")?.uppercase(Locale.ROOT)
                        val isRetailer = savedRole == UserRole.RETAILER.name
                        val isActive = doc.getBoolean("active") ?: false

                        customerName = savedName
                        mobile = savedPhone
                        bookingMobile = savedPhone
                        role = when (savedRole) {
                            UserRole.RETAILER.name -> UserRole.RETAILER
                            UserRole.ADMIN.name -> UserRole.ADMIN
                            else -> UserRole.CUSTOMER
                        }
                        page = if (isRetailer && !isActive) Page.PENDING else Page.HOME
                    } else {
                        // A Firebase Auth user without a profile must sign in/register again.
                        FirebaseAuth.getInstance().signOut()
                        page = Page.HOME
                    }
                    sessionCheckComplete = true
                }
                .addOnFailureListener {
                    // Keep the Firebase session; do not silently create a duplicate account.
                    sessionCheckComplete = true
                    Toast.makeText(context, "Profile load nahi hui. Internet aur Firestore Rules check karein.", Toast.LENGTH_LONG).show()
                }
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            secondary = Orange,
            background = Pale
        )
    ) {

        Scaffold(
            containerColor = Pale,
            bottomBar = {
                if (page in listOf(
                        Page.HOME,
                        Page.BOOKINGS,
                        Page.ACCOUNT,
                        Page.SETTINGS
                    )
                ) {
                    NavigationBar(containerColor = Color.White) {

                        NavigationBarItem(
                            selected = page == Page.HOME,
                            onClick = { page = Page.HOME },
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text("Home") }
                        )

                        NavigationBarItem(
                            selected = page == Page.BOOKINGS,
                            onClick = { page = Page.BOOKINGS },
                            icon = { Icon(Icons.Default.CalendarMonth, null) },
                            label = { Text("Bookings") }
                        )

                        NavigationBarItem(
                            selected = page == Page.ACCOUNT,
                            onClick = { page = Page.ACCOUNT },
                            icon = { Icon(Icons.Default.AccountCircle, null) },
                            label = { Text("Account") }
                        )

                        NavigationBarItem(
                            selected = page == Page.SETTINGS,
                            onClick = { page = Page.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, null) },
                            label = { Text("Settings") }
                        )
                    }
                }
            }
        ) { padding ->

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {

                when (page) {

                    Page.HOME -> HomeScreen(
                        services,
                        search,
                        { search = it },
                        {
                            selected = it
                            page = Page.GALLERY
                        },
                        { page = Page.ROLE }
                    )

                    Page.ROLE -> RoleScreen(
                        onSelect = {
                            role = it
                            page = Page.LOGIN
                        },
                        onBack = { page = Page.HOME }
                    )

                    Page.LOGIN -> PasswordAuthScreen(
                        role = role,
                        onBack = { page = Page.ROLE },
                        onAuthenticated = { name, phone, isPending ->
                            customerName = name
                            mobile = phone
                            bookingMobile = phone
                            page = if (isPending) Page.PENDING else Page.HOME
                        }
                    )

                    Page.PENDING -> PendingScreen(
                        onHome = { page = Page.HOME }
                    )

                    Page.GALLERY -> GalleryScreen(
                        selected,
                        onBack = { page = Page.HOME },
                        onBook = {
                            bookingMobile = mobile
                            page = Page.BOOKING
                        }
                    )

                    Page.BOOKING -> BookingScreen(
                        service = selected,
                        name = customerName,
                        onName = { customerName = it },
                        mobile = bookingMobile,
                        onMobile = { bookingMobile = it },
                        address = address,
                        onAddress = { address = it },
                        onBack = { page = Page.GALLERY },
                        onSubmit = {

                            val message = """
Hello CitySeva Dhule!

NEW BOOKING REQUEST

Customer Name: $customerName
Customer Mobile: +91 $bookingMobile
Service Booked: ${selected.name}
Service Address: $address

Booking Status: Pending Confirmation

Please contact the customer to confirm this booking.

Thank you!
CitySeva Dhule
                            """.trimIndent()

                            val whatsappUrl =
                                "https://wa.me/$WHATSAPP_NUMBER?text=${Uri.encode(message)}"

                            val newBooking = Booking(
                                service = selected.name,
                                customer = customerName,
                                mobile = bookingMobile,
                                address = address,
                                date = "Request submitted",
                                status = "Pending confirmation"
                            )

                            bookings = bookings + newBooking
                            page = Page.BOOKINGS

                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(whatsappUrl)
                                )
                                context.startActivity(intent)

                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    "WhatsApp link could not be opened.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )

                    Page.BOOKINGS -> BookingsScreen(bookings)

                    Page.ACCOUNT -> AccountScreen(
                        customerName,
                        mobile,
                        { page = Page.ROLE },
                        { page = Page.SETTINGS }
                    )

                    Page.SETTINGS -> SettingsScreen(
                        onBack = { page = Page.ACCOUNT }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    services: List<ServiceItem>,
    search: String,
    onSearch: (String) -> Unit,
    onService: (ServiceItem) -> Unit,
    onAccount: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "CitySeva",
                        fontSize = 29.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Navy
                    )
                    Text(
                        "DHULE  •  MAHARASHTRA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Blue,
                        letterSpacing = 1.5.sp
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    onClick = onAccount
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        "Account",
                        Modifier.padding(9.dp).size(30.dp),
                        tint = Blue
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search services in Dhule") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )
        }

        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(Navy, Blue))
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "YOUR CITY, YOUR SERVICES",
                        color = Color(0xFFFFD29A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        "Make every day\neasier with CitySeva",
                        color = Color.White,
                        fontSize = 25.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Discover trusted local services around Dhule.",
                        color = Color.White.copy(alpha = .88f),
                        fontSize = 13.sp
                    )
                    Surface(
                        color = Orange,
                        shape = RoundedCornerShape(12.dp),
                        onClick = { onService(services.first()) }
                    ) {
                        Text(
                            "Explore services  →",
                            Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 10.dp
                            ),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            SectionTitle("Explore services", "Choose what you need")
        }

        items(
            services.filter {
                it.name.contains(search, true) ||
                    it.subtitle.contains(search, true)
            }
        ) { service ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onService(service) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    Modifier.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(service.images.first()),
                        contentDescription = service.name,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Column(Modifier.weight(1f)) {
                        Text(
                            service.name,
                            color = Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            service.subtitle,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Text(
                            service.price,
                            color = Blue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }

                    Text("›", color = Blue, fontSize = 28.sp)
                }
            }
        }

        item {
            Text(
                "Local services. Simple bookings. Better days.",
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                color = Color.Gray,
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, sub: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            title,
            fontSize = 20.sp,
            color = Ink,
            fontWeight = FontWeight.ExtraBold
        )
        Text(sub, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun RoleScreen(
    onSelect: (UserRole) -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back") }
        Spacer(Modifier.height(18.dp))
        Text(
            "Welcome to CitySeva",
            fontSize = 27.sp,
            color = Navy,
            fontWeight = FontWeight.ExtraBold
        )
        Text("How would you like to continue?", color = Color.Gray)

        RoleCard(
            "Customer",
            "Book local services and manage your bookings",
            "🧑",
            "I want to book services"
        ) {
            onSelect(UserRole.CUSTOMER)
        }

        RoleCard(
            "Retailer / Service Partner",
            "Register your business and receive customer bookings",
            "🏪",
            "I provide services"
        ) {
            onSelect(UserRole.RETAILER)
        }

        RoleCard(
            "Admin",
            "Authorized CitySeva administrator access",
            "🛡️",
            "Admin login"
        ) {
            onSelect(UserRole.ADMIN)
        }

        Text(
            "Retailer accounts become active only after admin approval.",
            color = Blue,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    emoji: String,
    action: String,
    onClick: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(emoji, fontSize = 35.sp)
            Text(
                title,
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(description, color = Color.Gray, fontSize = 12.sp)
            Text(action + "  →", color = Blue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PasswordAuthScreen(
    role: UserRole,
    onBack: () -> Unit,
    onAuthenticated: (String, String, Boolean) -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val isAdmin = role == UserRole.ADMIN
    var isRegister by remember { mutableStateOf(role != UserRole.ADMIN) }
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun authEmail(phone: String): String = "91${phone}@cityseva.app"

    fun finishLogin(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val savedRole = doc.getString("role")?.uppercase(Locale.ROOT)
                val expectedRole = role.name
                val phone = doc.getString("phoneNumber") ?: mobile
                val savedName = doc.getString("name") ?: ""
                val active = doc.getBoolean("active") ?: false
                if (savedRole != expectedRole) {
                    auth.signOut()
                    busy = false
                    error = "Is mobile number ka account ${savedRole ?: "unknown"} role ka hai. Sahi login type chunein."
                } else if (isAdmin) {
                    auth.currentUser?.getIdToken(true)?.addOnSuccessListener { token ->
                        if (token.claims["admin"] == true) {
                            busy = false
                            onAuthenticated(savedName, phone, false)
                        } else {
                            auth.signOut()
                            busy = false
                            error = "Admin access authorize nahi hai. Admin claim Firebase Admin SDK se set karna hoga."
                        }
                    }?.addOnFailureListener {
                        auth.signOut(); busy = false; error = it.localizedMessage ?: "Admin verification failed."
                    }
                } else if (role == UserRole.RETAILER && !active) {
                    busy = false
                    onAuthenticated(savedName, phone, true)
                } else if (!active) {
                    auth.signOut(); busy = false; error = "Account inactive hai. Admin se contact karein."
                } else {
                    busy = false
                    onAuthenticated(savedName, phone, false)
                }
            }
            .addOnFailureListener {
                auth.signOut()
                busy = false
                error = it.localizedMessage ?: "Profile load nahi ho payi. Firestore rules check karein."
            }
    }

    fun submit() {
        error = ""
        if (!mobile.matches(Regex("[6-9][0-9]{9}"))) {
            error = "Valid 10-digit mobile number enter karein."
            return
        }
        if (password.length < 6) {
            error = "Password kam se kam 6 characters ka hona chahiye."
            return
        }
        if (isRegister && name.isBlank()) {
            error = if (role == UserRole.RETAILER) "Business / owner name enter karein." else "Apna naam enter karein."
            return
        }
        if (isRegister && password != confirmPassword) {
            error = "Password aur confirm password match nahi kar rahe."
            return
        }
        busy = true
        val emailAlias = authEmail(mobile)
        if (isRegister) {
            if (isAdmin) { busy = false; error = "Admin registration app se allowed nahi hai."; return }
            auth.createUserWithEmailAndPassword(emailAlias, password)
                .addOnSuccessListener { result ->
                    val isRetailer = role == UserRole.RETAILER
                    val profile = hashMapOf<String, Any>(
                        "name" to name.trim(),
                        "phoneNumber" to mobile,
                        "role" to role.name.lowercase(Locale.ROOT),
                        "active" to !isRetailer,
                        "status" to if (isRetailer) "pending" else "active",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("users").document(result.user!!.uid).set(profile)
                        .addOnSuccessListener {
                            busy = false
                            onAuthenticated(name.trim(), mobile, isRetailer)
                        }
                        .addOnFailureListener { e ->
                            result.user?.delete()
                            busy = false
                            error = e.localizedMessage ?: "Profile save nahi hui. Firestore rules check karein."
                        }
                }
                .addOnFailureListener { busy = false; error = it.localizedMessage ?: "Registration failed." }
        } else {
            auth.signInWithEmailAndPassword(emailAlias, password)
                .addOnSuccessListener { result -> finishLogin(result.user!!.uid) }
                .addOnFailureListener { busy = false; error = "Mobile number ya password galat hai. ${it.localizedMessage ?: ""}" }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back") }
        Spacer(Modifier.height(12.dp))
        Text(
            when (role) {
                UserRole.CUSTOMER -> "Customer account"
                UserRole.RETAILER -> "Retailer account"
                UserRole.ADMIN -> "Admin login"
            },
            fontSize = 26.sp,
            color = Navy,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            if (isRegister) "Create your account using mobile number and password."
            else "Login with your registered mobile number and password.",
            color = Color.Gray
        )
        if (isRegister) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (role == UserRole.RETAILER) "Business / owner name" else "Full name") },
                singleLine = true, shape = RoundedCornerShape(14.dp)
            )
        }
        OutlinedTextField(
            value = mobile,
            onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) mobile = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("10-digit mobile number") }, prefix = { Text("+91  ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true, shape = RoundedCornerShape(14.dp)
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password (minimum 6 characters)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true, shape = RoundedCornerShape(14.dp)
        )
        if (isRegister) {
            OutlinedTextField(
                value = confirmPassword, onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("Confirm password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true, shape = RoundedCornerShape(14.dp)
            )
        }
        Button(
            onClick = { submit() }, enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            else Text(if (isRegister) "Create account" else "Login")
        }
        if (!isAdmin) {
            TextButton(onClick = { isRegister = !isRegister; error = "" }) {
                Text(if (isRegister) "Already registered? Login" else "New here? Create account")
            }
        }
        if (!isRegister) {
            TextButton(onClick = {
                error = "Password reset ke liye CitySeva Admin se contact karein. OTP/SMS reset enabled nahi hai."
            }) {
                Text("Forgot password?")
            }
        }
        if (role == UserRole.RETAILER) {
            Text("Retailer registration ke baad Admin approval zaroori hai.", color = Blue, fontSize = 12.sp)
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        Text(
            "OTP/SMS login use nahi hota. Password Firebase Authentication mein securely store hota hai. Password bhoolne par admin support se reset karwayein.",
            color = Color.Gray, fontSize = 11.sp
        )
    }
}

@Composable
private fun PendingScreen(onHome: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(26.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⏳", fontSize = 62.sp)
        Text(
            "Application submitted",
            fontSize = 24.sp,
            color = Navy,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Text(
            "Your retailer account is pending admin approval. You can use the retailer dashboard after your business is verified.",
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to home")
        }
    }
}

@Composable
private fun GalleryScreen(
    service: ServiceItem,
    onBack: () -> Unit,
    onBook: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back to services") }

        Image(
            painter = painterResource(service.images.first()),
            contentDescription = service.name,
            modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(22.dp)),
            contentScale = ContentScale.Crop
        )

        Text(
            service.name,
            fontSize = 25.sp,
            color = Navy,
            fontWeight = FontWeight.ExtraBold
        )

        Text(service.subtitle, color = Color.Gray)

        Text(
            "Inspiration & packages",
            color = Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Text("Service photos", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            service.images.drop(1).forEachIndexed { index, imageId ->
                Image(
                    painter = painterResource(imageId),
                    contentDescription = "${service.name} photo ${index + 2}",
                    modifier = Modifier.weight(1f).height(82.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onBook,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Book this service")
        }
    }
}

@Composable
private fun BookingScreen(
    service: ServiceItem,
    name: String,
    onName: (String) -> Unit,
    mobile: String,
    onMobile: (String) -> Unit,
    address: String,
    onAddress: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back") }

        Text(
            "Book ${service.name}",
            fontSize = 24.sp,
            color = Navy,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "Share your details. The service provider can confirm availability.",
            color = Color.Gray,
            fontSize = 12.sp
        )

        OutlinedTextField(
            value = name,
            onValueChange = onName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your name") },
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = mobile,
            onValueChange = {
                if (it.length <= 10 && it.all(Char::isDigit)) {
                    onMobile(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your 10-digit mobile number") },
            prefix = { Text("+91  ") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = address,
            onValueChange = onAddress,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Service address in Dhule") },
            minLines = 3,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onSubmit,
            enabled = name.isNotBlank() &&
                mobile.matches(Regex("[6-9][0-9]{9}")) &&
                address.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Submit booking & open WhatsApp")
        }
    }
}

@Composable
private fun BookingsScreen(bookings: List<Booking>) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "My bookings",
            fontSize = 25.sp,
            color = Navy,
            fontWeight = FontWeight.ExtraBold
        )

        if (bookings.isEmpty()) {
            Text("Your bookings will appear here.", color = Color.Gray)
        }

        bookings.forEach { booking ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        booking.service,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Text("Customer: ${booking.customer}", color = Color.Gray, fontSize = 12.sp)
                    Text("Mobile: +91 ${booking.mobile}", color = Color.Gray, fontSize = 12.sp)
                    Text("Address: ${booking.address}", color = Color.Gray, fontSize = 12.sp)
                    Text(booking.date, color = Color.Gray, fontSize = 11.sp)
                    Text(
                        booking.status,
                        color = Orange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountScreen(
    name: String,
    mobile: String,
    onLogin: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "My account",
            fontSize = 25.sp,
            color = Navy,
            fontWeight = FontWeight.ExtraBold
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("👤", fontSize = 38.sp)
                Column {
                    Text(
                        name.ifBlank { "Welcome to CitySeva" },
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Text(
                        if (mobile.isBlank())
                            "Sign in to manage your bookings"
                        else
                            "+91 $mobile",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        listOf(
            "My bookings",
            "Saved addresses",
            "Help & support",
            "Account settings"
        ).forEach { label ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSettings() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Ink, fontWeight = FontWeight.Medium)
                    Text("›", color = Blue, fontSize = 22.sp)
                }
            }
        }

        OutlinedButton(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Customer / Retailer login")
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back") }

        Text(
            "Account settings",
            fontSize = 25.sp,
            color = Navy,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "Profile details, mobile number, notifications and privacy controls will be managed here.",
            color = Color.Gray
        )
    }
}
