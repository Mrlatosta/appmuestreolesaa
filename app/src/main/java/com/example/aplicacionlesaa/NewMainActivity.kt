package com.example.aplicacionlesaa

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.aplicacionlesaa.databinding.ActivityNewMainBinding


class NewMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNewMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnCrear = binding.btnCrearFolio
        btnCrear.setOnClickListener {
            val intent = Intent(this, SelePdmActivity::class.java)
            startActivity(intent)
        }

    }



}