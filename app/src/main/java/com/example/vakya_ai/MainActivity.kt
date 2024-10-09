package com.example.vakya_ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val apiKey = "AIzaSyBHTecn5UbnS3gV96x3AcJppkpAdRJw-2Q"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val eTPrompt = findViewById<EditText>(R.id.eTPrompt)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val tVResult = findViewById<TextView>(R.id.tVResult)
        val btnCopy = findViewById<Button>(R.id.btnCopy)

        btnSubmit.setOnClickListener {
            val prompt = eTPrompt.text.toString().trim()

            if (prompt.isEmpty()) {
                tVResult.text = "Please enter a question."
                return@setOnClickListener
            }

            // Set loading text while fetching response
            tVResult.text = "Fetching response, please wait..."
            btnCopy.visibility = Button.GONE // Hide the copy button until response is ready

            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        val generativeModel = GenerativeModel(
                            modelName = "gemini-1.5-flash",
                            apiKey = apiKey,
                            generationConfig = generationConfig {
                                temperature = 0.15f
                                topK = 32
                                topP = 1f
                                maxOutputTokens = 4096
                            },
                            safetySettings = listOf(
                                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
                                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
                                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
                                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
                            )
                        )
                        generativeModel.generateContent(prompt)
                    }

                    val plainText = response.text?.let { it1 -> formatResponse(it1) }
                    tVResult.text = plainText
                    eTPrompt.text.clear()

                    // Show copy button when a response is ready
                    btnCopy.visibility = Button.VISIBLE

                } catch (e: Exception) {
                    e.printStackTrace()
                    tVResult.text = "Error: ${e.localizedMessage}"
                    btnCopy.visibility = Button.GONE
                }
            }
        }

        // Copy response to clipboard when copy button is clicked
        btnCopy.setOnClickListener {
            val responseText = tVResult.text.toString()
            if (responseText.isNotEmpty()) {
                val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("AI Response", responseText)
                clipboardManager.setPrimaryClip(clip)

                // Show a toast message
                Toast.makeText(this, "Response copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No response to copy", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatResponse(responseText: String): String? {
        var formattedText = responseText
            ?.replace(Regex("\\*\\*|__"), "")
            ?.replace(Regex("#+ "), "")
        var bulletIndex = 1
        formattedText = formattedText?.replace(Regex("^[*-]\\s+", RegexOption.MULTILINE)) {
            "${bulletIndex++}. "
        }

        return formattedText
    }
}
