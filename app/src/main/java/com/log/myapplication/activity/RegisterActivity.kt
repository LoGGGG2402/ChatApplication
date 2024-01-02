package com.log.myapplication.activity


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.log.myapplication.constant.Constant.Companion.DATABASE_URL
import com.log.myapplication.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        databaseReference = FirebaseDatabase.getInstance(DATABASE_URL).getReference("Users")
        val binding = ActivityRegisterBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            finish()
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val name = binding.etName.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please enter text in email/pw/name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            registerUser(email, password, name)
        }
    }

    private fun registerUser(email: String, password: String, name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val hashMap: HashMap<String, Any> = HashMap()
                    hashMap["uid"] = user!!.uid
                    hashMap["name"] = name
                    hashMap["email"] = email
                    hashMap["avatar"] = 0
                    hashMap["chats"] = hashMapOf<String, String>()
                    databaseReference.child(user.uid).setValue(hashMap).addOnCompleteListener(this) { task1 ->
                        if (task1.isSuccessful) {
                            Toast.makeText(this, "Successfully created user with uid: ${user.uid}", Toast.LENGTH_SHORT).show()
                            // Sign in success, close login activity and start main activity
                            Intent(this, MainActivity::class.java).also {
                                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(it)
                            }
                        } else {
                            Toast.makeText(this, "Failed to update user: ${task1.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Failed to create user: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
