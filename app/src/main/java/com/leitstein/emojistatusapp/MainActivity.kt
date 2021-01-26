package com.leitstein.emojistatusapp

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class User (
        val displayName: String = "",
        val emojis: String = ""
        )

class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class MainActivity : AppCompatActivity() {

    private companion object {
        private const val TAG = "MainActivity"
        private val VALID_CHAR_TYPES = listOf(
            Character.NON_SPACING_MARK, // 6
            Character.DECIMAL_DIGIT_NUMBER, // 9
            Character.LETTER_NUMBER, // 10
            Character.OTHER_NUMBER, // 11
            Character.SPACE_SEPARATOR, // 12
            Character.FORMAT, // 16
            Character.SURROGATE, // 19
            Character.DASH_PUNCTUATION, // 20
            Character.START_PUNCTUATION, // 21
            Character.END_PUNCTUATION, // 22
            Character.CONNECTOR_PUNCTUATION, // 23
            Character.OTHER_PUNCTUATION, // 24
            Character.MATH_SYMBOL, // 25
            Character.CURRENCY_SYMBOL, //26
            Character.MODIFIER_SYMBOL, // 27
            Character.OTHER_SYMBOL // 28
        ).map { it.toInt() }.toSet()
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var rvUsers: RecyclerView

    // Access a Cloud Firestore instance from your Activity
    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = Firebase.auth

        val query = db.collection("users")

        val options = FirestoreRecyclerOptions.Builder<User>()
            .setQuery(query, User::class.java)
            .setLifecycleOwner(this)
            .build()

        val adapter = object: FirestoreRecyclerAdapter<User, UserViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
                val view =
                    LayoutInflater.from(this@MainActivity)
                        .inflate(android.R.layout.simple_list_item_2, parent, false)
                return UserViewHolder(view)
            }

            override fun onBindViewHolder(holder: UserViewHolder, position: Int, model: User) {
                val tvName: TextView = holder.itemView.findViewById(android.R.id.text1)
                val tvEmojis: TextView = holder.itemView.findViewById(android.R.id.text2)
                tvName.text = model.displayName
                tvEmojis.text = model.emojis
            }
        }
        rvUsers = findViewById(R.id.rvUsers)
        rvUsers.adapter = adapter
        rvUsers.layoutManager = LinearLayoutManager(this)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.miLogout) {
            Log.i(TAG, "User logged out.")

            // Logout the user
            auth.signOut()
            val logoutIntent = Intent(this, LoginActivity::class.java)
            logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(logoutIntent)
        } else if (item.itemId == R.id.miEdit) {
            Log.i(TAG, "Show alert dialog to edit status.")
            showAlertDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    inner class EmojiFilter : InputFilter {
        override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?,
            dstart: Int, dend: Int): CharSequence {
            // if the added text is valid, return source otherwise return ""
            if (source == null || source.isBlank())             {
                return ""
            }
            Log.i(TAG, "Added tet $source, it has a length ${source.length} characters")
            for (inputChar in source) {
                val type = Character.getType(inputChar)
                Log.i(TAG, "Character Type $type")
                if (!VALID_CHAR_TYPES.contains(type)) {
                    val toast = Toast.makeText(this@MainActivity, "Only emojis are allow!", Toast.LENGTH_SHORT)
                    toast.setGravity(Gravity.TOP, 0, 0)
                    toast.show()
                    return ""
                }
            }
            return source
        }
    }

    private fun showAlertDialog() {
        val editText = EditText(this)
        val emojiFilter = EmojiFilter()
        val lengthFilter = InputFilter.LengthFilter(9)
        editText.filters = arrayOf(lengthFilter, emojiFilter)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Update your emojis")
            .setView(editText)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK", null)
            .show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            Log.i(TAG, "Clicked on Positive button!")
            val emojisEntered = editText.text.toString()
            if (emojisEntered.isBlank()) {
                Toast.makeText(this, "Cannot submit an empty text", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "No signed in user", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update firestore with new emojis
            db.collection("users").document(currentUser.uid)
                .update("emojis", emojisEntered)
            dialog.dismiss()
        }
    }
}