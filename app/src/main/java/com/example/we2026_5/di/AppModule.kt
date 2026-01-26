package com.example.we2026_5.di

import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.data.repository.RealtimeDatabaseRepository
import com.example.we2026_5.ui.customermanager.CustomerManagerViewModel
import com.example.we2026_5.ui.tourplanner.TourPlannerViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Firebase Realtime Database (HAUPT-DATENBANK)
    single { FirebaseDatabase.getInstance() }
    
    // Firebase Storage (für Fotos)
    single { FirebaseStorage.getInstance() }
    
    // Firebase Auth
    single { FirebaseAuth.getInstance() }
    
    // Repository (verwenden Realtime Database)
    single { CustomerRepository(get()) }
    single { KundenListeRepository(get()) }
    single { RealtimeDatabaseRepository(get()) }
    
    // ViewModels
    viewModel { CustomerManagerViewModel(get()) }
    viewModel { TourPlannerViewModel(get(), get()) }
}
