package com.st10321779.rootedwealth.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.st10321779.rootedwealth.data.local.entity.Category
import com.st10321779.rootedwealth.data.local.entity.Expense
import com.st10321779.rootedwealth.data.local.entity.Income
import java.util.Calendar
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val listeners = mutableMapOf<DatabaseReference, ValueEventListener>()

    // Gets the current user's unique ID
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Gets a reference to the root node for the current user
    private fun getUserDataRef(): DatabaseReference? {
        return getCurrentUserId()?.let { uid ->
            database.getReference("users").child(uid)
        }
    }

    /*fun addExpense(expense: Expense) {
        getUserDataRef()?.child("expenses")?.push()?.setValue(expense)
    }

    fun addIncome(income: Income) {
        getUserDataRef()?.child("income")?.push()?.setValue(income)
    }

    fun deleteExpense(id: String) {
        getUserDataRef()?.child("expenses")?.child(id)?.removeValue()
    }

    fun deleteIncome(id: String) {
        getUserDataRef()?.child("income")?.child(id)?.removeValue()
    }*/
    fun addExpense(expense: Expense) {
        val ref = getUserDataRef()?.child("expenses")?.push()
        expense.id = ref?.key ?: ""
        ref?.setValue(expense)
    }

    fun addIncome(income: Income) {
        val ref = getUserDataRef()?.child("income")?.push()
        income.id = ref?.key ?: ""
        ref?.setValue(income)
    }

    fun addCategory(category: Category) {
        val ref = getUserDataRef()?.child("categories")?.push()
        category.id = ref?.key ?: ""
        ref?.setValue(category)
    }

    fun updateExpense(expense: Expense) {
        getUserDataRef()?.child("expenses")?.child(expense.id)?.setValue(expense)
    }

    fun updateCategory(category: Category) {
        // An update is just setting the value at a known ID
        getUserDataRef()?.child("categories")?.child(category.id)?.setValue(category)
    }
    fun deleteExpense(expenseId: String) {
        getUserDataRef()?.child("expenses")?.child(expenseId)?.removeValue()
    }

    fun deleteIncome(incomeId: String) {
        getUserDataRef()?.child("income")?.child(incomeId)?.removeValue()
    }

    fun deleteCategory(categoryId: String) {
        getUserDataRef()?.child("categories")?.child(categoryId)?.removeValue()
    }

    /*fun getExpensesLiveData(): LiveData<List<Expense>> {
        val liveData = MutableLiveData<List<Expense>>()
        val expensesRef = getUserDataRef()?.child("expenses")

        expensesRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val expenses = snapshot.children.mapNotNull { it.getValue(Expense::class.java) }
                liveData.postValue(expenses)
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
        return liveData
    }

    fun getIncomeLiveData(): LiveData<List<Income>> {
        // Similar implementation to getExpensesLiveData
        val liveData = MutableLiveData<List<Income>>()
        getUserDataRef()?.child("income")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val incomeList = snapshot.children.mapNotNull { it.getValue(Income::class.java) }
                liveData.postValue(incomeList)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        return liveData
    }*/

    fun getExpensesLiveData(): LiveData<List<Expense>> = createGenericListener("expenses")
    fun getIncomeLiveData(): LiveData<List<Income>> = createGenericListener("income")
    fun getCategoriesLiveData(): LiveData<List<Category>> = createGenericListener("categories")
    private inline fun <reified T> createGenericListener(path: String): LiveData<List<T>> {
        val liveData = MutableLiveData<List<T>>()
        val dataRef = getUserDataRef()?.child(path)

        if (dataRef != null) {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = snapshot.children.mapNotNull { it.getValue(T::class.java) }
                    liveData.postValue(items)
                }
                override fun onCancelled(error: DatabaseError) { /* Handle error */ }
            }
            dataRef.addValueEventListener(listener)
            listeners[dataRef] = listener // Store listener for cleanup
        }
        return liveData
    }
    // One-time fetch for default categories
    suspend fun hasDefaultCategories(): Boolean = suspendCoroutine { continuation ->
        val categoriesRef = getUserDataRef()?.child("categories")
        categoriesRef?.orderByChild("default")?.equalTo(true)?.limitToFirst(1)
            ?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    continuation.resume(snapshot.exists())
                }
                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(false)
                }
            }) ?: continuation.resume(false)
    }

    fun seedDefaultCategories() {
        val defaults = listOf(
            Category(name = "Groceries", color = "#4CAF50", isDefault = true),
            Category(name = "Transport", color = "#2196F3", isDefault = true),
            Category(name = "Entertainment", color = "#9C27B0", isDefault = true),
            Category(name = "Rent", color = "#FF9800", isDefault = true),
            Category(name = "Utilities", color = "#FFC107", isDefault = true),
            Category(name = "Takeout", color = "#E91E63", isDefault = true),
            Category(name = "Health", color = "#F44336", isDefault = true),
            Category(name = "Education", color = "#009688", isDefault = true)
        )
        defaults.forEach { addCategory(it) }
    }

    // when the user logs out to prevent data leaks
    fun removeAllListeners() {
        listeners.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        listeners.clear()
    }

    suspend fun getExpenseCount(): Int = suspendCoroutine { continuation ->
        val expensesRef = getUserDataRef()?.child("expenses")
        expensesRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                continuation.resume(snapshot.childrenCount.toInt())
            }
            override fun onCancelled(error: DatabaseError) {
                continuation.resume(0)
            }
        }) ?: continuation.resume(0)
    }
    fun getPurchasedThemesLiveData(): LiveData<List<String>> {
        val liveData = MutableLiveData<List<String>>()
        val themesRef = getUserDataRef()?.child("purchasedThemes")

        themesRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //Firebase stores sets as maps (e.g., {"spiderman": true, "rosy": true})
                //we just need the keys.
                val themeIds = snapshot.children.mapNotNull { it.key }
                liveData.postValue(themeIds)
            }
            override fun onCancelled(error: DatabaseError) {
                liveData.postValue(emptyList()) // Post empty list on error
            }
        })
        return liveData
    }

    fun purchaseTheme(themeId: String) {
        //we store the theme ID as a key with a value of 'true'
        getUserDataRef()?.child("purchasedThemes")?.child(themeId)?.setValue(true)
    }

    //This seeds the default owned theme for a new user
    fun seedDefaultTheme() {
        getUserDataRef()?.child("purchasedThemes")?.child("default")?.setValue(true)
    }


    suspend fun seedBankLinkData() {
        //Fetch the user's categories directly from Firebase, one time.
        val categories = getCategoriesOneTime()
        if (categories.isEmpty()) {
            // If there are no categories, we can't proceed.
            //seed them just in case.
            seedDefaultCategories()
            // And fetch them again.
            val finalCategories = getCategoriesOneTime()
            if (finalCategories.isEmpty()) return // If still empty, exit.
            performSeeding(finalCategories)
        } else {
            performSeeding(categories)
        }
    }
    // helper contain the actual seeding logic
    private fun performSeeding(categories: List<Category>) {
        addIncome(Income(
            amount = 25000.0, date = getDate(25), source = "Salary (Simulated)", isLinked = true
        ))

        val rentCat = categories.find { it.name == "Rent (Simulated)" }
        rentCat?.let {
            addExpense(Expense(
                amount = 7500.0, date = getDate(1), categoryId = it.id,
                notes = "Monthly Rent (Simulated)", isLinked = true
            ))
        }

        val utilCat = categories.find { it.name == "Utilities (Simulated)" }
        utilCat?.let {
            addExpense(Expense(
                amount = 850.0, date = getDate(3), categoryId = it.id,
                notes = "Electricity (Simulated)", isLinked = true
            ))
        }

        val groceriesCat = categories.find { it.name == "Groceries (Simulated)" }
        for (i in 1..5) {
            groceriesCat?.let {
                addExpense(Expense(
                    amount = Random.nextDouble(150.0, 600.0),
                    date = getDate(Random.nextInt(2, 28)),
                    categoryId = it.id, notes = "Grocery Store (Simulated)", isLinked = true
                ))
            }
        }
    }

    private suspend fun getCategoriesOneTime(): List<Category> = suspendCoroutine { continuation ->
        val categoriesRef = getUserDataRef()?.child("categories")
        categoriesRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(Category::class.java) }
                continuation.resume(items)
            }
            override fun onCancelled(error: DatabaseError) {
                continuation.resume(emptyList())
            }
        }) ?: continuation.resume(emptyList())
    }

    private fun getDate(dayOfMonth: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        return calendar.time
    }
}