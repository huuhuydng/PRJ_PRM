package com.example.wavesoffoodadmin

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wavesoffoodadmin.databinding.ActivityAddItemBinding
import com.example.wavesoffoodadmin.model.AllMenu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.io.IOException

class AddItemActivity : AppCompatActivity() {

    private lateinit var foodName: String
    private lateinit var foodPrice: String
    private lateinit var foodDescription: String
    private lateinit var foodIngredient: String
    private var foodImageUri: Uri? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val binding: ActivityAddItemBinding by lazy {
        ActivityAddItemBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Chon image
        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Them mon
        binding.addItemButton.setOnClickListener {
            foodName = binding.foodName.text.toString().trim()
            foodPrice = binding.foodPrice.text.toString().trim()
            foodDescription = binding.description.text.toString().trim()
            foodIngredient = binding.ingredient.text.toString().trim()

            if (foodName.isNotBlank() && foodPrice.isNotBlank() &&
                foodDescription.isNotBlank() && foodIngredient.isNotBlank()
            ) {
                uploadDataToCloudinary()
            } else {
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    // Upload image len Cloudinary
    private fun uploadDataToCloudinary() {
        if (foodImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val cloudName = "dt4mgtqce" // Cloud Name
        val uploadPreset = "PRM_WOF" // Upload Preset

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", "food_image.jpg",
                contentResolver.openInputStream(foodImageUri!!)?.let { inputStream ->
                    object : RequestBody() {
                        override fun contentType() = "image/*".toMediaTypeOrNull()
                        override fun writeTo(sink: BufferedSink) {
                            inputStream.use { sink.writeAll(it.source()) }
                        }
                    }
                } ?: return
            )
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(requestBody)
            .build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AddItemActivity, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@AddItemActivity, "Upload error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val json = JSONObject(response.body?.string() ?: "")
                val imageUrl = json.getString("secure_url")

                // Luu vao Firebase Database sau khi co link image
                saveMenuDataToFirebase(imageUrl)
            }
        })
    }

    // Luu du lieu vo Firebase
    private fun saveMenuDataToFirebase(imageUrl: String) {
        val menuRef: DatabaseReference = database.getReference("menu")
        val newKeyItem: String? = menuRef.push().key

        newKeyItem?.let { key ->
            val newItem = AllMenu(
                foodName = foodName,
                foodPrice = foodPrice,
                foodDescription = foodDescription,
                foodIngredient = foodIngredient,
                foodImage = imageUrl,
                key = key  // Save Firebase key for deletion
            )
            
            menuRef.child(key).setValue(newItem).addOnSuccessListener {
                runOnUiThread {
                    Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }.addOnFailureListener {
                runOnUiThread {
                    Toast.makeText(this, "Database upload failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Chon image
    private val pickImage = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            binding.selectedImage.setImageURI(uri)
            foodImageUri = uri
        }
    }
}
