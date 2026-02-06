package com.example.we2026_5.di

import android.content.Context
import com.example.we2026_5.data.repository.ArticleRepository
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.ErfassungRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.data.repository.TourPlanRepository
import com.example.we2026_5.ui.main.MainViewModel
import com.example.we2026_5.ui.statistics.StatisticsViewModel
import com.example.we2026_5.ui.liste.ListeErstellenViewModel
import com.example.we2026_5.ui.customermanager.CustomerManagerViewModel
import com.example.we2026_5.ui.addcustomer.AddCustomerViewModel
import com.example.we2026_5.ui.kundenlisten.KundenListenViewModel
import com.example.we2026_5.ui.listebearbeiten.ListeBearbeitenViewModel
import com.example.we2026_5.ui.mapview.MapViewViewModel
import com.example.we2026_5.ui.detail.CustomerDetailViewModel
import com.example.we2026_5.ui.tourplanner.TourPlannerViewModel
import com.example.we2026_5.ui.urlaub.UrlaubViewModel
import com.example.we2026_5.ui.wasch.ArtikelVerwaltungViewModel
import com.example.we2026_5.ui.wasch.WaschenErfassungViewModel
import com.example.we2026_5.ui.sevdesk.SevDeskImportViewModel
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
    single { TourPlanRepository(get()) }
    single { ArticleRepository(get()) }
    single { ErfassungRepository(get()) }
    
    // ViewModels
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { StatisticsViewModel(get()) }
    viewModel { ListeErstellenViewModel(get(), get()) }
    viewModel { CustomerManagerViewModel(get()) }
    viewModel { TourPlannerViewModel(get(), get()) }
    viewModel { AddCustomerViewModel() }
    viewModel { KundenListenViewModel(get<Context>(), get<KundenListeRepository>(), get<CustomerRepository>()) }
    viewModel { ListeBearbeitenViewModel(get(), get<CustomerRepository>()) }
    viewModel { MapViewViewModel(get<CustomerRepository>(), get<KundenListeRepository>()) }
    viewModel { CustomerDetailViewModel(get()) }
    viewModel { (customerId: String) -> UrlaubViewModel(get(), customerId) }
    viewModel { WaschenErfassungViewModel(get(), get(), get()) }
    viewModel { ArtikelVerwaltungViewModel(get()) }
    viewModel { (ctx: Context) -> SevDeskImportViewModel(ctx, get(), get()) }
}
