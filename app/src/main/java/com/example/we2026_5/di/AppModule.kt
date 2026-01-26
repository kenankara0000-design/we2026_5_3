package com.example.we2026_5.di

import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.ui.customermanager.CustomerManagerViewModel
import com.example.we2026_5.ui.tourplanner.TourPlannerViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Firebase Firestore
    single { FirebaseFirestore.getInstance() }
    
    // Firebase Storage
    single { FirebaseStorage.getInstance() }
    
    // Firebase Auth
    single { FirebaseAuth.getInstance() }
    
    // Repository
    single { CustomerRepository(get()) }
    
    // ViewModels
    viewModel { CustomerManagerViewModel(get()) }
    viewModel { TourPlannerViewModel(get()) }
}
