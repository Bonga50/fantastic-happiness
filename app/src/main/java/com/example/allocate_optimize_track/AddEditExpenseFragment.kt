package com.example.allocate_optimize_track

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
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
    private val expenseViewModel: ExpenseViewModel by viewModels() // Or activityViewModels()
    // Use Safe Args delegate to get arguments
    private val args: AddEditExpenseFragmentArgs by navArgs()
    // View Binding (optional but recommended)
    // private var _binding: FragmentAddEditExpenseBinding? = null
    // private val binding get() = _binding!!

    // Views (example without View Binding)
    private lateinit var amountEditText: TextInputEditText
    private lateinit var dateButton: MaterialButton
    private lateinit var dateTextView: TextView
    private lateinit var categoryAutoComplete: AutoCompleteTextView
    private lateinit var categoryLayout: TextInputLayout
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var photoPreviewImageView: ImageView
    private lateinit var attachPhotoButton: MaterialButton
    private lateinit var saveButton: MaterialButton



    // Activity Result Launchers
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestGalleryPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var selectImageLauncher: ActivityResultLauncher<String>

    private var selectedDateMillis: Long = System.currentTimeMillis() // Default to now
    private var selectedCategory: Category? = null
    private var currentPhotoUri: Uri? = null
    private var tempCameraPhotoUri: Uri? = null // URI provided to camera app
    private var existingExpense: Expense? = null // Store fetched expense for editing
    private var isEditing: Boolean = false

    private lateinit var categoryAdapter: ArrayAdapter<String>
    private var categoryList: List<Category> = emptyList()
    private val categoryNameMap = mutableMapOf<String, Long>() // Map display name to ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActivityResultLaunchers()
        isEditing = args.expenseId != -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        val view = inflater.inflate(R.layout.fragment_add_edit_expense, container, false)
        // If using View Binding:
        // _binding = FragmentAddEditExpenseBinding.inflate(inflater, container, false)
        // return binding.root

        // Find views (example without View Binding)
        amountEditText = view.findViewById(R.id.editTextExpenseAmount)
        dateButton = view.findViewById(R.id.buttonSelectDate)
        dateTextView = view.findViewById(R.id.textViewSelectedDate)
        categoryLayout = view.findViewById(R.id.layoutExpenseCategory)
        categoryAutoComplete = view.findViewById(R.id.autoCompleteCategory)
        descriptionEditText = view.findViewById(R.id.editTextExpenseDescription)
        photoPreviewImageView = view.findViewById(R.id.imageViewReceiptPreview)
        attachPhotoButton = view.findViewById(R.id.buttonAttachPhoto)
        saveButton = view.findViewById(R.id.buttonSaveExpense)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("AddEditExpense", "onViewCreated started") // Add log
        setupCategoryDropdown()
        setupDateSelection() // <--- Make sure this is called
        setupPhotoAttachment()
        setupSaveButton()
        updateDateDisplay()
        observeCategories()

        Log.d("AddEditExpense", "onViewCreated finished setup") // Add log

        if (isEditing) {
            Log.d("AddEditExpense", "Editing mode. Expense ID: ${args.expenseId}")
            // Change UI elements for editing mode
            // (e.g., binding.buttonSaveExpense.text = "Update Expense", findNavController().currentDestination?.label = "Edit Expense")
            saveButton.text = "Update Expense" // Example

            // Fetch the existing expense data
            fetchAndPopulateExpenseData()
        } else {
            Log.d("AddEditExpense", "Adding new expense.")
            // Set default date display for new expense
            updateDateDisplay()
            saveButton.text = "Save Expense" // Example
        }
    }



    private fun setupActivityResultLaunchers() {
        // Camera Permission
        requestCameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        // Gallery Permission
        requestGalleryPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                selectImageLauncher.launch("image/*")
            } else {
                Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        // Take Picture
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success: Boolean ->
            if (success) {
                currentPhotoUri = tempCameraPhotoUri // The URI we passed to the camera
                loadPhotoPreview(currentPhotoUri)
            } else {
                // Handle failure or cancellation
                Log.e("AddEditExpense", "Camera capture failed or cancelled")
                // Optionally delete the temporary file if created
            }
            tempCameraPhotoUri = null // Reset temp uri
        }

        // Select Image from Gallery
        selectImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent() // Or ActivityResultContracts.PickVisualMedia() for modern picker
        ) { uri: Uri? ->
            uri?.let {
                // Persist permission for gallery URI if needed across restarts
                try {
                    //requireContext().contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    currentPhotoUri = it
                    loadPhotoPreview(currentPhotoUri)
                } catch (e: SecurityException) {
                    Log.e("AddEditExpense", "Failed to persist URI permission", e)
                    Toast.makeText(requireContext(), "Could not load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun setupCategoryDropdown() {
        categoryAdapter = ArrayAdapter(requireContext(), R.layout.list_item_dropdown, mutableListOf<String>()) // Use a simple list item layout
        categoryAutoComplete.setAdapter(categoryAdapter)

        categoryAutoComplete.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as String
            val selectedId = categoryNameMap[selectedName]
            selectedCategory = categoryList.find { it.id == selectedId }
            // Clear error if category is selected
            categoryLayout.error = null
            Log.d("AddEditExpense", "Selected category: ${selectedCategory?.name} (ID: ${selectedCategory?.id})")
        }
        // Clear selection if text is manually cleared (optional)
        categoryAutoComplete.addTextChangedListener { text ->
            if (text.isNullOrEmpty() || !categoryNameMap.containsKey(text.toString())) {
                selectedCategory = null
            }
        }
    }

    private fun observeCategories() {
        Log.d("AddEditExpense", "Setting up categories observer")
        expenseViewModel.userCategories.observe(viewLifecycleOwner, Observer { categories ->
            Log.d("AddEditExpense", "Categories Observer received data. Count: ${categories?.size}")
            val previousCategoryListEmpty = categoryList.isEmpty() // Check if list was previously empty
            categoryList = categories ?: emptyList()
            categoryNameMap.clear()
            val categoryNames = categories?.map { categoryNameMap[it.name] = it.id; it.name } ?: emptyList()

            categoryAdapter.clear()
            categoryAdapter.addAll(categoryNames)
            // categoryAdapter.notifyDataSetChanged() // Usually not needed with addAll

            // If editing and categories just loaded, try to select the category again
            if (isEditing && previousCategoryListEmpty && categoryList.isNotEmpty()) {
                existingExpense?.let { findAndSelectCategory(it.categoryId) }
            }
        })
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
        val options = arrayOf( "Choose from Gallery", "Cancel")
        AlertDialog.Builder(requireContext())
            .setTitle("Attach Receipt")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkGalleryPermissionAndLaunch() // Take Photo
                    1 -> dialog.dismiss() // Choose from Gallery
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

    private fun loadPhotoPreview(uri: Uri?) {
        if (uri != null) {
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder) // Add a placeholder drawable
                .error(R.drawable.ic_image_error) // Add an error drawable
                .into(photoPreviewImageView)
            photoPreviewImageView.visibility = View.VISIBLE
        } else {
            photoPreviewImageView.visibility = View.GONE
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

        // --- Validation ---
        var isValid = true
        val amount = amountString.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            amountEditText.error = "Please enter a valid positive amount"
            isValid = false
        } else {
            amountEditText.error = null
        }

        if (selectedCategory == null) {
            categoryLayout.error = "Please select a category" // Set error on the layout
            // categoryAutoComplete.error = "Please select a category" // Alternative
            isValid = false
        } else {
            categoryLayout.error = null
        }

        if (description.isEmpty()) {
            descriptionEditText.error = "Description cannot be empty"
            isValid = false
        } else {
            descriptionEditText.error = null
        }

        // Optional: Check if date was explicitly selected if required beyond default
        // if (!dateWasSelected) { ... }

        if (!isValid) return

        val userId = Firebase.auth.currentUser?.email
        if (userId == null) { /* ... handle error ... */ return }

        // Create or Update the Expense object
        val expenseData = amount?.let {
            Expense(
                id = if (isEditing) existingExpense!!.id else 0, // Use existing ID if editing
                userId = userId,
                categoryId = selectedCategory!!.id, // Safe due to validation
                amount = it, // Safe due to validation
                date = selectedDateMillis,
                description = description, // Safe due to validation
                photoUri = currentPhotoUri?.toString()
            )
        }

        // --- Call ViewModel ---
        if (isEditing) {
            Log.d("AddEditExpense", "Updating expense: $expenseData")
            if (expenseData != null) {
                expenseViewModel.update(expenseData)
            }
            Toast.makeText(requireContext(), "Expense Updated", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("AddEditExpense", "Inserting new expense: $expenseData")
            if (expenseData != null) {
                expenseViewModel.insert(expenseData)
            }
            Toast.makeText(requireContext(), "Expense Saved", Toast.LENGTH_SHORT).show()
        }

        // --- Navigate Back ---
        findNavController().navigateUp()
    }

    // --- Lifecycle ---
    // override fun onDestroyView() {
    //     super.onDestroyView()
    //     _binding = null // Clean up View Binding
    // }

    // --- Fetch and Populate Logic ---
    private fun fetchAndPopulateExpenseData() {
        // Use coroutine scoped to the fragment's view lifecycle
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                existingExpense = expenseViewModel.getExpenseById(args.expenseId)
                Log.d("AddEditExpense", "Fetched expense: $existingExpense")
                if (existingExpense != null) {
                    // Populate fields on the main thread
                    populateFields(existingExpense!!)
                } else {
                    // Handle case where expense is not found (e.g., deleted elsewhere)
                    Log.e("AddEditExpense", "Expense with ID ${args.expenseId} not found or doesn't belong to user.")
                    Toast.makeText(requireContext(), "Error loading expense data.", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp() // Go back
                }
            } catch (e: Exception) {
                Log.e("AddEditExpense", "Error fetching expense", e)
                Toast.makeText(requireContext(), "Error loading expense data.", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp() // Go back
            }
        }
    }

    private fun populateFields(expense: Expense) {
        amountEditText.setText(expense.amount.toString())
        descriptionEditText.setText(expense.description)

        // Set date
        selectedDateMillis = expense.date
        updateDateDisplay() // Update the TextView

        // Set category (requires categories to be loaded first)
        // We might need to wait for the category list observer
        val categoryIdToSelect = expense.categoryId
        findAndSelectCategory(categoryIdToSelect) // Call helper to handle selection

        // Set photo
        if (!expense.photoUri.isNullOrEmpty()) {
            currentPhotoUri = Uri.parse(expense.photoUri)
            loadPhotoPreview(currentPhotoUri)
        } else {
            currentPhotoUri = null
            loadPhotoPreview(null)
        }
    }

    // Helper to select category once list is loaded
    private fun findAndSelectCategory(categoryIdToSelect: Long) {
        if (categoryList.isNotEmpty()) {
            val category = categoryList.find { it.id == categoryIdToSelect }
            if (category != null) {
                selectedCategory = category
                // Set the text in the AutoCompleteTextView. Must match an item in the adapter.
                categoryAutoComplete.setText(category.name, false) // false = don't filter
                categoryLayout.error = null // Clear potential error
                Log.d("AddEditExpense", "Pre-selected category: ${category.name}")
            } else {
                Log.w("AddEditExpense", "Category ID $categoryIdToSelect not found in loaded list.")
                categoryLayout.error = "Original category not found" // Show error
                selectedCategory = null // Clear selection
            }
        } else {
            // Categories not loaded yet, do nothing or schedule check?
            // The observer will call this again when categories arrive if needed.
            Log.d("AddEditExpense", "Categories not loaded yet, cannot pre-select category.")
        }
    }
}