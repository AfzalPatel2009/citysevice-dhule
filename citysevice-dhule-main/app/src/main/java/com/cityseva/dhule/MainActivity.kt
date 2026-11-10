package com.cityseva.dhule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF14265C)
private val Blue = Color(0xFF2855D9)
private val Orange = Color(0xFFFF8A24)
private val Pale = Color(0xFFF4F6FC)
private val Ink = Color(0xFF18213B)

private data class ServiceItem(val name: String, val subtitle: String, val emoji: String, val price: String)
private data class Booking(val service: String, val customer: String, val date: String, val status: String)
private enum class Page { HOME, ROLE, PHONE, OTP, PENDING, GALLERY, BOOKING, BOOKINGS, ACCOUNT, SETTINGS }
private enum class UserRole { CUSTOMER, RETAILER }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CitySevaApp() }
    }
}

@Composable
private fun CitySevaApp() {
    val services = listOf(
        ServiceItem("Party Decoration", "Birthdays, weddings & events", "🎈", "Packages from ₹999"),
        ServiceItem("House Cleaning", "Deep cleaning & regular cleaning", "🧹", "Packages from ₹499"),
        ServiceItem("Washing Service", "Laundry, ironing & pickup", "👕", "Packages from ₹99"),
        ServiceItem("Painting Contract", "Home and office painting", "🎨", "Get a free estimate"),
        ServiceItem("AC Repair", "Service, repair & installation", "❄️", "Inspection available"),
        ServiceItem("Plumbing & Electrical", "Trusted home maintenance", "🔧", "Book a technician")
    )
    var page by remember { mutableStateOf(Page.HOME) }
    var role by remember { mutableStateOf(UserRole.CUSTOMER) }
    var selected by remember { mutableStateOf(services.first()) }
    var mobile by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var bookings by remember { mutableStateOf(listOf<Booking>()) }
    var search by remember { mutableStateOf("") }

    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, secondary = Orange, background = Pale)) {
        Scaffold(
            containerColor = Pale,
            bottomBar = {
                if (page in listOf(Page.HOME, Page.BOOKINGS, Page.ACCOUNT, Page.SETTINGS)) {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(selected = page == Page.HOME, onClick = { page = Page.HOME }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                        NavigationBarItem(selected = page == Page.BOOKINGS, onClick = { page = Page.BOOKINGS }, icon = { Icon(Icons.Default.CalendarMonth, null) }, label = { Text("Bookings") })
                        NavigationBarItem(selected = page == Page.ACCOUNT, onClick = { page = Page.ACCOUNT }, icon = { Icon(Icons.Default.AccountCircle, null) }, label = { Text("Account") })
                        NavigationBarItem(selected = page == Page.SETTINGS, onClick = { page = Page.SETTINGS }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (page) {
                    Page.HOME -> HomeScreen(services, search, { search = it }, { selected = it; page = Page.GALLERY }, { page = Page.ROLE })
                    Page.ROLE -> RoleScreen(onSelect = { role = it; page = Page.PHONE }, onBack = { page = Page.HOME })
                    Page.PHONE -> PhoneScreen(role, mobile, { mobile = it }, onBack = { page = Page.ROLE }, onContinue = { page = Page.OTP })
                    Page.OTP -> OtpScreen(mobile, otp, { otp = it }, onBack = { page = Page.PHONE }, onVerify = {
                        // Real OTP verification is connected after Firebase project setup.
                        if (role == UserRole.RETAILER) page = Page.PENDING else page = Page.ACCOUNT
                    })
                    Page.PENDING -> PendingScreen(onHome = { page = Page.HOME })
                    Page.GALLERY -> GalleryScreen(selected, onBack = { page = Page.HOME }, onBook = { page = Page.BOOKING })
                    Page.BOOKING -> BookingScreen(selected, customerName, { customerName = it }, address, { address = it }, onBack = { page = Page.GALLERY }, onSubmit = {
                        bookings = bookings + Booking(selected.name, customerName, "Request submitted", "Pending confirmation")
                        page = Page.BOOKINGS
                    })
                    Page.BOOKINGS -> BookingsScreen(bookings)
                    Page.ACCOUNT -> AccountScreen(customerName, mobile, { page = Page.ROLE }, { page = Page.SETTINGS })
                    Page.SETTINGS -> SettingsScreen(onBack = { page = Page.ACCOUNT })
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(services: List<ServiceItem>, search: String, onSearch: (String) -> Unit, onService: (ServiceItem) -> Unit, onAccount: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("CitySeva", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
                    Text("DHULE  •  MAHARASHTRA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Blue, letterSpacing = 1.5.sp)
                }
                Surface(shape = CircleShape, color = Color.White, onClick = onAccount) { Icon(Icons.Default.AccountCircle, "Account", Modifier.padding(9.dp).size(30.dp), tint = Blue) }
            }
        }
        item {
            OutlinedTextField(value = search, onValueChange = onSearch, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search services in Dhule") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White))
        }
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Navy, Blue))).padding(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("YOUR CITY, YOUR SERVICES", color = Color(0xFFFFD29A), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("Make every day\neasier with CitySeva", color = Color.White, fontSize = 25.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Discover trusted local services around Dhule.", color = Color.White.copy(alpha = .88f), fontSize = 13.sp)
                    Surface(color = Orange, shape = RoundedCornerShape(12.dp), onClick = { onService(services.first()) }) { Text("Explore services  →", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
        item { SectionTitle("Explore services", "Choose what you need") }
        items(services.filter { it.name.contains(search, true) || it.subtitle.contains(search, true) }) { service ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onService(service) }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFEFF3FF)), contentAlignment = Alignment.Center) { Text(service.emoji, fontSize = 29.sp) }
                    Column(Modifier.weight(1f)) {
                        Text(service.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(service.subtitle, color = Color.Gray, fontSize = 11.sp)
                        Text(service.price, color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                    Text("›", color = Blue, fontSize = 28.sp)
                }
            }
        }
        item { Text("Local services. Simple bookings. Better days.", Modifier.fillMaxWidth().padding(vertical = 12.dp), color = Color.Gray, textAlign = TextAlign.Center, fontSize = 11.sp) }
    }
}

@Composable
private fun SectionTitle(title: String, sub: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, fontSize = 20.sp, color = Ink, fontWeight = FontWeight.ExtraBold); Text(sub, color = Color.Gray, fontSize = 12.sp) }
}

@Composable
private fun RoleScreen(onSelect: (UserRole) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        Spacer(Modifier.height(18.dp))
        Text("Welcome to CitySeva", fontSize = 27.sp, color = Navy, fontWeight = FontWeight.ExtraBold)
        Text("How would you like to continue?", color = Color.Gray)
        RoleCard("Customer", "Book local services and manage your bookings", "🧑", "I want to book services") { onSelect(UserRole.CUSTOMER) }
        RoleCard("Retailer / Service Partner", "Register your business and receive customer bookings", "🏪", "I provide services") { onSelect(UserRole.RETAILER) }
        Text("Retailer accounts become active only after admin approval.", color = Blue, fontSize = 12.sp)
    }
}

@Composable
private fun RoleCard(title: String, description: String, emoji: String, action: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(emoji, fontSize = 35.sp); Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(description, color = Color.Gray, fontSize = 12.sp)
            Text(action + "  →", color = Blue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PhoneScreen(role: UserRole, mobile: String, onMobile: (String) -> Unit, onBack: () -> Unit, onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        Spacer(Modifier.height(30.dp))
        Text("Enter your mobile number", fontSize = 25.sp, color = Navy, fontWeight = FontWeight.ExtraBold)
        Text(if (role == UserRole.CUSTOMER) "Customer account" else "Retailer registration", color = Blue, fontWeight = FontWeight.Bold)
        Text("We will send a one-time password to verify your number.", color = Color.Gray)
        OutlinedTextField(value = mobile, onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) onMobile(it) }, modifier = Modifier.fillMaxWidth(), label = { Text("10-digit mobile number") }, prefix = { Text("+91  ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, shape = RoundedCornerShape(14.dp))
        Button(onClick = onContinue, enabled = mobile.matches(Regex("[6-9][0-9]{9}")), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Continue to OTP") }
        Text("OTP delivery will be enabled after secure Firebase phone authentication setup.", color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
private fun OtpScreen(mobile: String, otp: String, onOtp: (String) -> Unit, onBack: () -> Unit, onVerify: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        Spacer(Modifier.height(30.dp))
        Text("Verify your number", fontSize = 25.sp, color = Navy, fontWeight = FontWeight.ExtraBold)
        Text("Enter the 6-digit OTP sent to +91 $mobile", color = Color.Gray)
        OutlinedTextField(value = otp, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) onOtp(it) }, modifier = Modifier.fillMaxWidth(), label = { Text("6-digit OTP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(14.dp))
        Button(onClick = onVerify, enabled = false, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Firebase setup required") }
        Text("This is the OTP screen preview. Real SMS verification is not active until Firebase is connected.", color = Orange, fontSize = 11.sp)
    }
}

@Composable
private fun PendingScreen(onHome: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⏳", fontSize = 62.sp)
        Text("Application submitted", fontSize = 24.sp, color = Navy, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text("Your retailer account is pending admin approval. You can use the retailer dashboard after your business is verified.", color = Color.Gray, textAlign = TextAlign.Center)
        Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Back to home") }
    }
}

@Composable
private fun GalleryScreen(service: ServiceItem, onBack: () -> Unit, onBook: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack) { Text("← Back to services") }
        Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFFFFD8C4), Color(0xFF8E69E8)))), contentAlignment = Alignment.Center) { Text(service.emoji, fontSize = 76.sp) }
        Text(service.name, fontSize = 25.sp, color = Navy, fontWeight = FontWeight.ExtraBold)
        Text(service.subtitle, color = Color.Gray)
        Text("Inspiration & packages", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Decoration and service photos will appear here. We will add a separate gallery for every service.", color = Color.Gray, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("✨", "🎉", "🌸").forEach { mark -> Box(Modifier.weight(1f).height(78.dp).clip(RoundedCornerShape(16.dp)).background(Color.White), contentAlignment = Alignment.Center) { Text(mark, fontSize = 32.sp) } }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onBook, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) { Text("Book this service") }
    }
}

@Composable
private fun BookingScreen(service: ServiceItem, name: String, onName: (String) -> Unit, address: String, onAddress: (String) -> Unit, onBack: () -> Unit, onSubmit: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text("Book ${service.name}", fontSize = 24.sp, color = Navy, fontWeight = FontWeight.ExtraBold)
        Text("Share your details. The service provider can confirm availability.", color = Color.Gray, fontSize = 12.sp)
        OutlinedTextField(value = name, onValueChange = onName, modifier = Modifier.fillMaxWidth(), label = { Text("Your name") }, shape = RoundedCornerShape(14.dp))
        OutlinedTextField(value = address, onValueChange = onAddress, modifier = Modifier.fillMaxWidth(), label = { Text("Service address in Dhule") }, minLines = 3, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.weight(1f))
        Button(onClick = onSubmit, enabled = name.isNotBlank() && address.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Submit booking request") }
    }
}

@Composable
private fun BookingsScreen(bookings: List<Booking>) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("My bookings", fontSize = 25.sp, color = Navy, fontWeight = FontWeight.ExtraBold)
        if (bookings.isEmpty()) Text("Your bookings will appear here.", color = Color.Gray)
        bookings.forEach { booking ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(booking.service, fontWeight = FontWeight.Bold, color = Ink)
                    Text(booking.customer, color = Color.Gray, fontSize = 12.sp)
                    Text(booking.status, color = Orange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AccountScreen(name: String, mobile: String, onLogin: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("My account", fontSize = 25.sp, color = Navy, fontWeight = FontWeight.ExtraBold)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("👤", fontSize = 38.sp)
                Column { Text(name.ifBlank { "Welcome to CitySeva" }, fontWeight = FontWeight.Bold, color = Ink); Text(if (mobile.isBlank()) "Sign in to manage your bookings" else "+91 $mobile", color = Color.Gray, fontSize = 12.sp) }
            }
        }
        listOf("My bookings", "Saved addresses", "Help & support", "Account settings").forEach { label ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) { Row(Modifier.fillMaxWidth().clickable { onSettings() }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Ink, fontWeight = FontWeight.Medium); Text("›", color = Blue, fontSize = 22.sp) } }
        }
        OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Customer / Retailer login") }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text("Account settings", fontSize = 25.sp, color = Navy, fontWeight = FontWeight.ExtraBold)
        Text("Profile details, mobile number, notifications and privacy controls will be managed here.", color = Color.Gray)
    }
}
