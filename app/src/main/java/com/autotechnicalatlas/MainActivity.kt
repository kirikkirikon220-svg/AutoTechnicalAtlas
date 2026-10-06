package com.autotechnicalatlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AutoTechnicalAtlasApp()
        }
    }
}

@Composable
fun AutoTechnicalAtlasApp() {

    MaterialTheme {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {

                Text(
                    text = "AUTO TECHNICAL ATLAS",
                    modifier = Modifier.padding(20.dp),
                    fontSize = 20.sp,
                    color = Color.Black
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "Технический атлас\n\nВыберите автомобиль",
                        fontSize = 18.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}
