package com.aritma.musteritakip.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aritma.musteritakip.data.local.entity.CustomerEntity
import com.aritma.musteritakip.data.local.entity.DistrictEntity
import com.aritma.musteritakip.data.local.entity.NoteEntity
import com.aritma.musteritakip.data.repository.CustomerRepository
import com.aritma.musteritakip.util.DateUtils
import com.aritma.musteritakip.util.MaintenanceStatus
import com.aritma.musteritakip.util.StatusType
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption {
    MAINTENANCE_DATE_ASC,
    NAME_ASC,
    DISTRICT_ASC
}

enum class StatusFilter {
    ALL,
    OVERDUE_ONLY,
    DUE_SOON_ONLY,
    SAFE_ONLY,
    MUTED_ONLY,
    RESERVED_ONLY
}

/**
 * UI katmanı için müşteri, filtreleme, arama ve randevu durumlarını yöneten ViewModel.
 * Reaktif StateFlow akışları aracılığıyla ekranların güncel veriyi dinlemesini sağlar;
 * açılışta eski aşırı uzatılmış tarihleri otomatik dengeleyen bakım fonksiyonunu yürütür.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CustomerViewModel(private val repository: CustomerRepository) : ViewModel() {

    init {
        fixOverExtendedMaintenanceDates()
    }

    private fun fixOverExtendedMaintenanceDates() {
        viewModelScope.launch {
            val allCustomers = repository.getAllActiveCustomersList()
            val today = System.currentTimeMillis()
            val maxAllowedDate = DateUtils.addMonths(today, 12) + (24 * 60 * 60 * 1000L) // Today + 366 days max limit

            allCustomers.forEach { customer ->
                if (customer.maintenanceDate > maxAllowedDate) {
                    val fixedDate = DateUtils.addMonths(today, 12)
                    repository.updateMaintenanceDate(customer.id, fixedDate)
                }
            }
        }
    }

    private val _selectedModule = MutableStateFlow(CustomerEntity.MODULE_BIREYSEL)
    val selectedModule: StateFlow<String> = _selectedModule.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDistrict = MutableStateFlow<String?>(null) // null = Tüm Semtler
    val selectedDistrict: StateFlow<String?> = _selectedDistrict.asStateFlow()

    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    val statusFilter: StateFlow<StatusFilter> = _statusFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.MAINTENANCE_DATE_ASC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    val allDistricts: StateFlow<List<DistrictEntity>> = repository.getAllDistricts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val bireyselCount: StateFlow<Int> = repository.getCustomerCountByModule(CustomerEntity.MODULE_BIREYSEL)
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val lojistikCount: StateFlow<Int> = repository.getCustomerCountByModule(CustomerEntity.MODULE_LOJISTIK)
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val reservationsList: StateFlow<List<CustomerEntity>> = repository.getCustomersWithReservation()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val todayReservationsCount: StateFlow<Int> = reservationsList.map { list ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        list.count { customer ->
            val resDate = customer.reservationDate ?: return@count false
            resDate in startOfDay..endOfDay
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val urgentCustomerCount: StateFlow<Int> = repository.getAllActiveCustomersFlow().map { list ->
        list.count { customer ->
            val status = MaintenanceStatus.calculate(customer.maintenanceDate, customer.isMuted)
            status.type == StatusType.OVERDUE || status.type == StatusType.DUE_SOON
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val customers: StateFlow<List<CustomerEntity>> = _selectedModule.flatMapLatest { module ->
        repository.getCustomersByModule(module)
    }.combine(_searchQuery) { list, query ->
        if (query.isBlank()) list else {
            val q = query.trim().lowercase()
            list.filter {
                it.name.lowercase().contains(q) ||
                        it.surname.lowercase().contains(q) ||
                        (it.companyName?.lowercase()?.contains(q) == true) ||
                        (it.companyPhone?.contains(q) == true) ||
                        it.phone.contains(q) ||
                        it.address.lowercase().contains(q) ||
                        it.district.lowercase().contains(q)
            }
        }
    }.combine(_selectedDistrict) { list, district ->
        if (district.isNullOrBlank() || district == "Tüm Semtler") list else {
            list.filter { it.district.equals(district, ignoreCase = true) }
        }
    }.combine(_statusFilter) { list, filter ->
        if (filter == StatusFilter.ALL) list else {
            list.filter { customer ->
                val status = MaintenanceStatus.calculate(customer.maintenanceDate, customer.isMuted)
                when (filter) {
                    StatusFilter.OVERDUE_ONLY -> status.type == StatusType.OVERDUE
                    StatusFilter.DUE_SOON_ONLY -> status.type == StatusType.DUE_SOON
                    StatusFilter.SAFE_ONLY -> status.type == StatusType.SAFE
                    StatusFilter.MUTED_ONLY -> status.type == StatusType.MUTED
                    StatusFilter.RESERVED_ONLY -> customer.reservationDate != null
                    else -> true
                }
            }
        }
    }.combine(_sortOption) { list, sort ->
        when (sort) {
            SortOption.MAINTENANCE_DATE_ASC -> list.sortedBy { it.maintenanceDate }
            SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            SortOption.DISTRICT_ASC -> list.sortedBy { it.district.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getCustomerByIdFlow(id: Long): Flow<CustomerEntity?> =
        repository.getCustomerByIdFlow(id)

    fun setModule(module: String) {
        _selectedModule.value = module
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDistrictFilter(district: String?) {
        _selectedDistrict.value = district
    }

    fun setStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun addCustomer(
        module: String,
        name: String,
        surname: String,
        phone: String,
        address: String,
        district: String,
        installationDate: Long,
        maintenanceDate: Long,
        companyName: String? = null,
        companyPhone: String? = null,
        initialNote: String? = null,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val customer = CustomerEntity(
                module = module,
                name = name,
                surname = surname,
                phone = phone,
                address = address,
                district = district,
                installationDate = installationDate,
                maintenanceDate = maintenanceDate,
                companyName = companyName,
                companyPhone = companyPhone
            )
            val customerId = repository.insertCustomer(customer)
            if (!initialNote.isNullOrBlank() && customerId > 0) {
                repository.addNote(customerId, initialNote)
            }
            onComplete(customerId)
        }
    }

    fun updateCustomer(customer: CustomerEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            onComplete()
        }
    }

    fun archiveCustomer(customerId: Long) {
        viewModelScope.launch {
            repository.archiveCustomer(customerId)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    fun toggleMuteStatus(customerId: Long, currentIsMuted: Boolean) {
        viewModelScope.launch {
            val newMute = !currentIsMuted
            repository.updateMuteStatus(customerId, newMute)
            val note = if (newMute) "Müşteri susturuldu (Bildirim dışı yapıldı 🔇)" else "Müşteri susturması kaldırıldı (Aktif 🔔)"
            repository.addNote(customerId, note)
        }
    }

    fun setReservation(customerId: Long, dateMillis: Long, note: String?) {
        viewModelScope.launch {
            repository.updateReservation(customerId, dateMillis, note)
            val noteText = "Randevu/Sıraya alma eklendi 🗓️: ${DateUtils.formatDateTime(dateMillis)}${if (!note.isNullOrBlank()) " ($note)" else ""}"
            repository.addNote(customerId, noteText)
        }
    }

    fun clearReservation(customerId: Long) {
        viewModelScope.launch {
            repository.updateReservation(customerId, null, null)
            repository.addNote(customerId, "Randevu iptal edildi ❌")
        }
    }

    fun refreshMaintenanceDate(customerId: Long, monthsToAdd: Int = 12) {
        viewModelScope.launch {
            val today = System.currentTimeMillis()
            val newDate = DateUtils.addMonths(today, monthsToAdd)
            repository.updateMaintenanceDate(customerId, newDate)
            val noteText = "Bakım yapıldı (Bugünden itibaren +$monthsToAdd Ay eklendi). Yeni bakım tarihi: ${DateUtils.formatDate(newDate)}"
            repository.addNote(customerId, noteText)
        }
    }

    fun getNotesForCustomer(customerId: Long): Flow<List<NoteEntity>> =
        repository.getNotesForCustomer(customerId)

    fun addNote(customerId: Long, noteText: String) {
        viewModelScope.launch {
            if (noteText.isNotBlank()) {
                repository.addNote(customerId, noteText.trim())
            }
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun addNewDistrict(name: String) {
        viewModelScope.launch {
            repository.addDistrict(name)
        }
    }
}
