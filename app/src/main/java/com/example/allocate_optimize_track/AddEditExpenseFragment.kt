package com.example.allocate_optimize_track

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateFormat // Use Android's DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController // If using Navigation Component
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide // Add Glide dependency: implementation 'com.github.bumptech.glide:glide:4.16.0'
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AddEditExpenseFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddEditExpenseFragment : Fragment() {
    // Use the same ViewModel instance as the list fragment if needed, or a new one
    private val expenseViewModel: ExpenseViewModel by activityViewModels()
    private val args: AddEditExpenseFragmentArgs by navArgs()
    private val gamificationViewModel: GamificationViewModel by viewModels()
    // Views
    private lateinit var amountEditText: TextInputEditText
    private lateinit var dateButton: MaterialButton
    private lateinit var dateTextView: TextView
    private lateinit var categoryAutoComplete: AutoCompleteTextView
    private lateinit var categoryLayout: TextInputLayout
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var photoPreviewImageView: ImageView
    private lateinit var attachPhotoButton: MaterialButton
    private lateinit var saveButton: MaterialButton

    private lateinit var buttonLastUsedCategory: MaterialButton
    private val PREFS_NAME = "expense_app_prefs"
    private val LAST_USED_CATEGORY_ID_KEY = "last_used_category_id"
    private val LAST_USED_CATEGORY_NAME_KEY = "last_used_category_name"



    // Activity Result Launchers
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestGalleryPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var selectImageLauncher: ActivityResultLauncher<String>

    // State variables
    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var selectedCategory: Category? = null
    private var currentPhotoUri: Uri? = null // Uri of a NEWLY selected photo by the user
    private var tempCameraPhotoUri: Uri? = null
    private var existingExpense: Expense? = null
    private var isEditing: Boolean = false

    // Category Dropdown
    private lateinit var categoryAdapterSpinner: ArrayAdapter<String>
    private var categoryList: List<Category> = emptyList()
    private val categoryNameMap = mutableMapOf<String, String>() // Map display name to ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActivityResultLaunchers()
        // Check if editing based on passed argument ID. Firebase IDs are not -1L.
        isEditing = !args.expenseId.isNullOrBlank()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_edit_expense, container, false)

        amountEditText = view.findViewById(R.id.editTextExpenseAmount)
        dateButton = view.findViewById(R.id.buttonSelectDate)
        dateTextView = view.findViewById(R.id.textViewSelectedDate)
        categoryLayout = view.findViewById(R.id.layoutExpenseCategory)
        categoryAutoComplete = view.findViewById(R.id.autoCompleteCategory)
        descriptionEditText = view.findViewById(R.id.editTextExpenseDescription)
        photoPreviewImageView = view.findViewById(R.id.imageViewReceiptPreview)
        attachPhotoButton = view.findViewById(R.id.buttonAttachPhoto)
        saveButton = view.findViewById(R.id.buttonSaveExpense)
        buttonLastUsedCategory = view.findViewById(R.id.buttonLastUsedCategory)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("AddEditExpense", "onViewCreated. isEditing: $isEditing, expenseId: ${args.expenseId}")

        setupCategoryDropdown() // Must be called before observeCategories if populating from existing
        setupDateSelection()
        setupPhotoAttachment()
        setupSaveButton()
        observeCategoriesForDropdown() // Start observing categories for the dropdown
        loadAndDisplayLastUsedCategoryButton()
        setupLastUsedCategoryButtonClickListener()

        if (isEditing) {
            saveButton.text = getString(R.string.update_expense_button_text) // Use string resource
            // Fetch and populate existing expense data
            viewLifecycleOwner.lifecycleScope.launch {
                val fetchedExpense =
                    args.expenseId?.let { expenseViewModel.getExpenseById(it) } // Suspend fun
                if (fetchedExpense != null) {
                    existingExpense = fetchedExpense
                    populateFields(existingExpense!!)
                } else {
                    Toast.makeText(context, "Error: Could not load expense for editing.", Toast.LENGTH_LONG).show()
                    Log.e("AddEditExpense", "Failed to fetch expense with ID: ${args.expenseId}")
                    findNavController().navigateUp()
                }
            }
        } else {
            saveButton.text = getString(R.string.save_expense_button_text) // Use string resource
            updateDateDisplay() // Set initial date for new expense
        }

        expenseViewModel.saveExpenseStatus.observe(viewLifecycleOwner) { result ->
            Log.d("AddEditExpense", "saveExpenseStatus observed: $result") // Log every emission
            if (result == null) { // If it's null (cleared or initial), do nothing further
                Log.d("AddEditExpense", "saveExpenseStatus is null, returning from observer.")
                saveButton.isEnabled = true // Ensure button is enabled if status is cleared
                return@observe
            }

            // Only process non-null results once
            when (result) {
                is FirebaseResult.Loading -> {
                    saveButton.isEnabled = false
                    Log.d("AddEditExpense", "Status: Loading...")
                }
                is FirebaseResult.Success -> {
                    saveButton.isEnabled = true // Re-enable button
                    Toast.makeText(context, if (isEditing) "Expense Updated" else "Expense Saved", Toast.LENGTH_SHORT).show()
                    Log.d("AddEditExpense", "Status: Success. Navigating up.")
                    findNavController().navigateUp()
                    gamificationViewModel.recordUserActivity()
                    expenseViewModel.clearSaveExpenseStatus() // Clear after handling
                }
                is FirebaseResult.Failure -> {
                    saveButton.isEnabled = true // Re-enable button
                    Toast.makeText(context, "Operation Failed: ${result.exception.message}", Toast.LENGTH_LONG).show()
                    Log.e("AddEditExpense", "Status: Failure", result.exception)
                    expenseViewModel.clearSaveExpenseStatus() // Clear after handling
                }
            }
        }
    }



    private fun setupActivityResultLaunchers() {
        // Camera Permission
        requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) launchCamera()
            else Toast.makeText(requireContext(), "Camera permission denied.", Toast.LENGTH_SHORT).show()
        }
        // Gallery Permission
        requestGalleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) launchGallery()
            else Toast.makeText(requireContext(), "Gallery permission denied.", Toast.LENGTH_SHORT).show()
        }
        // Take Picture
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoUri = tempCameraPhotoUri // This is the Uri passed to camera app
                loadPhotoPreview(currentPhotoUri)
            } else { Log.e("AddEditExpense", "Camera capture failed/cancelled.") }
            tempCameraPhotoUri = null // Reset
        }
        // Select Image from Gallery
        selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                currentPhotoUri = it
                loadPhotoPreview(currentPhotoUri)
            }
        }
    }


    private fun setupCategoryDropdown() {
        categoryAdapterSpinner = ArrayAdapter(requireContext(), R.layout.list_item_dropdown, mutableListOf<String>())
        categoryAutoComplete.setAdapter(categoryAdapterSpinner)
        categoryAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedName = categoryAdapterSpinner.getItem(position)
            val categoryId = categoryNameMap[selectedName] // Get ID from map
            selectedCategory = categoryList.find { it.id == categoryId }
            categoryLayout.error = null // Clear error on selection
            Log.d("AddEditExpense", "Category selected: ${selectedCategory?.name}, ID: ${selectedCategory?.id}")
        }
        // Optional: Clear selection if text is manually cleared
        categoryAutoComplete.addTextChangedListener { text ->
            if (text.isNullOrEmpty() || !categoryNameMap.containsKey(text.toString())) {
                if (selectedCategory != null) { // Only log if it was previously selected
                    Log.d("AddEditExpense", "Category selection cleared or invalid input.")
                }
                selectedCategory = null
            }
        }
    }

    private fun observeCategoriesForDropdown() {
        Log.d("AddEditExpense", "Observing user categories for dropdown.")
        expenseViewModel.userCategories.observe(viewLifecycleOwner) { categories ->
            Log.d("AddEditExpense", "Received categories for dropdown: ${categories?.size ?: 0}")
            val wasCategoryListEmpty = categoryList.isEmpty() // Check before updating
            categoryList = categories ?: emptyList()
            categoryNameMap.clear()
            val names = categoryList.map { category ->
                categoryNameMap[category.name] = category.id // Map name to Firebase ID
                category.name
            }
            categoryAdapterSpinner.clear()
            categoryAdapterSpinner.addAll(names)
            // categoryAdapterSpinner.notifyDataSetChanged() // Not always needed with addAll

            // If editing and categories just loaded, and we have an existing expense, try to select its category
            if (isEditing && wasCategoryListEmpty && categoryList.isNotEmpty() && existingExpense != null) {
                Log.d("AddEditExpense", "Attempting to pre-select category for existing expense.")
                findAndSelectCategoryInDropdown(existingExpense!!.categoryId)
            }
        }
    }

    private fun findAndSelectCategoryInDropdown(categoryIdToSelect: String) {
        Log.d("AddEditExpense", "Attempting to find and select category ID: $categoryIdToSelect")
        val category = categoryList.find { it.id == categoryIdToSelect }
        if (category != null) {
            selectedCategory = category
            // Set the text in the AutoCompleteTextView. This must be one of the strings
            // that the adapter currently holds for the dropdown to show it.
            categoryAutoComplete.setText(category.name, false) // false = don't filter
            categoryLayout.error = null
            Log.d("AddEditExpense", "Successfully pre-selected category: ${category.name}")
        } else {
            Log.w("AddEditExpense", "Could not find category ID '$categoryIdToSelect' in current categoryList.")
            if (isEditing) categoryLayout.error = "Original category not found"
        }
    }

    private fun setupDateSelection() {
        dateButton.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun updateDateDisplay() {
        // Format the milliseconds timestamp into a user-readable date string
        val dateFormat = DateFormat.getDateFormat(requireContext()) // Locale-aware format
        dateTextView.text = "Selected: ${dateFormat.format(Date(selectedDateMillis))}"
        dateTextView.visibility = View.VISIBLE
        // Clear potential error when date is selected
        // (Need a way to track if date was explicitly selected, maybe a boolean flag)
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDateMillis // Start picker at current selection

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
            selectedDateMillis = selectedCalendar.timeInMillis // Store as milliseconds
            updateDateDisplay()
        }, year, month, day).show()
    }


    private fun setupPhotoAttachment() {
        attachPhotoButton.setOnClickListener {
            showPhotoSourceDialog()
        }
        photoPreviewImageView.setOnClickListener {
            // Optional: Allow clicking preview to change photo again
            showPhotoSourceDialog()
        }
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel") // Add "Take Photo" back
        AlertDialog.Builder(requireContext())
            .setTitle("Attach Receipt")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch() // Take Photo
                    1 -> checkGalleryPermissionAndLaunch() // Choose from Gallery
                    2 -> dialog.dismiss() // Cancel
                }
            }
            .show()
    }



    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted, proceed to launch camera
                Log.d("AddEditExpense", "Camera permission already granted.")
                launchCamera() // Your existing function to start camera intent
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Explain to the user why you need the permission (optional but recommended)
                // Show a dialog or SnackBar here
                Log.d("AddEditExpense", "Showing rationale for camera permission.")
                showPermissionRationaleDialog(
                    "Camera access is needed to take photos of receipts.",
                    Manifest.permission.CAMERA,
                    requestCameraPermissionLauncher
                )
            }
            else -> {
                // Directly request the permission
                Log.d("AddEditExpense", "Requesting camera permission.")
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // Helper function to show explanation (optional)
    private fun showPermissionRationaleDialog(
        message: String,
        permission: String,
        launcher: ActivityResultLauncher<String>
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Needed")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                // Request permission again after explanation
                launcher.launch(permission)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchCamera() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Log.e("AddEditExpense", "Error creating image file", ex)
            Toast.makeText(requireContext(), "Error preparing camera", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.let { file -> // Use 'let' to scope operations on the non-null file
            val providerUri: Uri? = try { // Add try-catch for FileProvider itself
                FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider", // Authority matches AndroidManifest
                    file
                )
            } catch (e: IllegalArgumentException) {
                Log.e("AddEditExpense", "FileProvider configuration error.", e)
                Toast.makeText(requireContext(), "Error accessing photo storage.", Toast.LENGTH_SHORT).show()
                null
            }

            providerUri?.let { uri -> // Use 'let' again for the non-null Uri
                // Store the URI we are passing to the camera, BEFORE launching
                tempCameraPhotoUri = uri

                try {
                    // Launch with the non-null 'uri' from the let block
                    takePictureLauncher.launch(uri)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show()
                    tempCameraPhotoUri = null // Reset if launch fails immediately
                } catch (e: SecurityException) {
                    Log.e("AddEditExpense", "Security exception launching camera.", e)
                    Toast.makeText(requireContext(), "Permission error launching camera.", Toast.LENGTH_SHORT).show()
                    tempCameraPhotoUri = null // Reset on error
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES) // App-specific external storage
        // Or use cache dir: requireContext().cacheDir path defined in file_paths.xml
        // val imagePath = File(requireContext().cacheDir, "images")
        // imagePath.mkdirs()

        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
            // imagePath /* directory if using cache */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents (optional)
            // currentPhotoPath = absolutePath
        }
    }


    private fun checkGalleryPermissionAndLaunch() {
        // Determine the correct permission based on Android version
        val permissionToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES // API 33+
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE // Below API 33
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permissionToRequest // Use the determined permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted
                Log.d("AddEditExpense", "Gallery permission already granted ($permissionToRequest).")
                launchGallery() // Separate function to launch gallery intent
            }
            shouldShowRequestPermissionRationale(permissionToRequest) -> {
                // Explain why you need it
                Log.d("AddEditExpense", "Showing rationale for gallery permission ($permissionToRequest).")
                showPermissionRationaleDialog(
                    "Storage access is needed to select receipt images from your gallery.",
                    permissionToRequest,
                    requestGalleryPermissionLauncher // Use the correct launcher
                )
            }
            else -> {
                // Directly request the permission
                Log.d("AddEditExpense", "Requesting gallery permission ($permissionToRequest).")
                // Make sure the gallery launcher requests the correct permission
                requestGalleryPermissionLauncher.launch(permissionToRequest)
            }
        }
    }

    private fun launchGallery() {
        // Use GetContent or PickVisualMedia
        selectImageLauncher.launch("image/*")
        // For modern photo picker (recommended):
        // pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        // You'd need to define pickMediaLauncher using ActivityResultContracts.PickVisualMedia()
    }

    private fun loadPhotoPreview(uri: Uri?) { // This loads the NEWLY selected local URI
        if (uri != null) {
            Log.d("AddEditExpense", "Loading NEWLY selected photo preview from URI: $uri")
            Glide.with(this)
                .load(uri) // Local URI
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .into(photoPreviewImageView)
            photoPreviewImageView.visibility = View.VISIBLE
        } else {
            // If uri is null, it means no NEW image is selected.
            // If editing, the existing image (if any) would have been loaded in populateFields from Supabase URL.
            // If adding and no image selected, keep it hidden.
            if (!isEditing || existingExpense?.photoStoragePath.isNullOrEmpty()) {
                photoPreviewImageView.visibility = View.GONE
            }
            Log.d("AddEditExpense", "No new photo URI to preview. isEditing: $isEditing")
        }
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            saveExpense()
        }
    }

    private fun saveExpense() {
        val amountString = amountEditText.text.toString()
        val description = descriptionEditText.text.toString().trim()
        var isValid = true

        val amount = amountString.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            amountEditText.error = "Valid amount required"; isValid = false
        } else amountEditText.error = null

        if (selectedCategory == null) {
            categoryLayout.error = "Category required"; isValid = false
        } else categoryLayout.error = null

        if (description.isEmpty()) {
            descriptionEditText.error = "Description required"; isValid = false
        } else descriptionEditText.error = null

        if (!isValid) {
            Log.w("AddEditExpense", "Validation failed.")
            return
        }

        val currentFbUserId = Firebase.auth.currentUser?.uid
        if (currentFbUserId == null) {
            Toast.makeText(context, "Error: User not logged in.", Toast.LENGTH_LONG).show()
            return
        }

        val expenseToSave = Expense(
            id = if (isEditing) existingExpense!!.id else "", // ID is empty for new, service generates
            userId = currentFbUserId,
            categoryId = selectedCategory!!.id, // Firebase ID of category
            amount = amount!!, // Safe due to validation
            date = selectedDateMillis,
            description = description,
            // If editing AND no new image was picked (currentPhotoUri is null), keep existing path.
            // Otherwise, new image (currentPhotoUri) will be uploaded, or path becomes null if no image.
            photoStoragePath = if (isEditing && currentPhotoUri == null) existingExpense?.photoStoragePath else null
        )

        Log.d("AddEditExpense", "Attempting to save/update expense: $expenseToSave, newPhotoUri: $currentPhotoUri")
        if (selectedCategory != null) {
            val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            prefs.putString(LAST_USED_CATEGORY_ID_KEY, selectedCategory!!.id)
            prefs.putString(LAST_USED_CATEGORY_NAME_KEY, selectedCategory!!.name)
            prefs.apply()
            Log.d("AddEditExpense", "Saved last used category: ${selectedCategory!!.name}")
        }
        val contentResolver: ContentResolver = requireActivity().contentResolver

        if (isEditing) {
            expenseViewModel.updateExpense(expenseToSave, currentPhotoUri, contentResolver)
        } else {
            expenseViewModel.insertExpense(expenseToSave, currentPhotoUri, contentResolver)
        }

    }


    private fun populateFields(expense: Expense) {
        Log.d("AddEditExpense", "Populating fields for expense: ${expense.description}")
        amountEditText.setText(expense.amount.toString())
        descriptionEditText.setText(expense.description)

        selectedDateMillis = expense.date
        updateDateDisplay()

        // Category selection will be handled by observeCategoriesForDropdown
        // when the category list is available. We just need to ensure
        // findAndSelectCategoryInDropdown is called if editing.
        if (categoryList.isNotEmpty()) { // If categories already loaded
            findAndSelectCategoryInDropdown(expense.categoryId)
        } // Else, observeCategoriesForDropdown will handle it when list arrives

        // Photo
        if (!expense.photoStoragePath.isNullOrEmpty()) {
            val imageUrl = SupabaseImageService.getImageUrl(expense.photoStoragePath!!)
            Log.d("AddEditExpense", "Loading existing image from URL: $imageUrl")
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder) // Ensure these drawables exist
                .error(R.drawable.ic_image_error)
                .centerCrop()
                .into(photoPreviewImageView)
            photoPreviewImageView.visibility = View.VISIBLE
        } else {
            photoPreviewImageView.visibility = View.GONE
        }
        currentPhotoUri = null // Reset any newly picked photo URI when populating existing data
    }

    private fun loadAndDisplayLastUsedCategoryButton() {
        val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastUsedId = prefs.getString(LAST_USED_CATEGORY_ID_KEY, null)
        val lastName = prefs.getString(LAST_USED_CATEGORY_NAME_KEY, null)

        if (lastUsedId != null && lastName != null) {
            buttonLastUsedCategory.text = "Use Last: $lastName"
            buttonLastUsedCategory.visibility = View.VISIBLE
        } else {
            buttonLastUsedCategory.visibility = View.GONE
        }
    }

    private fun setupLastUsedCategoryButtonClickListener() {
        buttonLastUsedCategory.setOnClickListener {
            val prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastUsedId = prefs.getString(LAST_USED_CATEGORY_ID_KEY, null)
            if (lastUsedId != null && categoryList.isNotEmpty()) {
                // Try to find and select this category in the current dropdown list
                val categoryToSelect = categoryList.find { it.id == lastUsedId }
                if (categoryToSelect != null) {
                    selectedCategory = categoryToSelect
                    categoryAutoComplete.setText(categoryToSelect.name, false) // Update dropdown
                    categoryLayout.error = null
                    Log.d("AddEditExpense", "Selected last used category: ${categoryToSelect.name}")
                    Toast.makeText(context, "'${categoryToSelect.name}' selected", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Last used category not available", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}