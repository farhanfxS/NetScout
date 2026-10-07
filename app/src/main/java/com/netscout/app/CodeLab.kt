package com.netscout.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class GeneratedFile(
    val path: String,
    val content: String
)

data class GeneratedProject(
    val name: String,
    val files: List<GeneratedFile>
)

object CodeLabEngine {

    fun generate(
        mode: String,
        request: String
    ): GeneratedProject {

        val safeRequest =
            request.trim().ifEmpty {
                "Describe the project you want to build."
            }

        return when (mode) {
            "APP BUILDER" -> generateAndroidApp(safeRequest)
            "API BUILDER" -> generateApi(safeRequest)
            "CODE REVIEW" -> generateReview(safeRequest)
            "SECURITY TESTS" -> generateSecurityTests(safeRequest)
            "LAB GENERATOR" -> generateLab(safeRequest)
            "FIX / PATCH" -> generatePatch(safeRequest)
            else -> generateCode(safeRequest)
        }
    }

    private fun generateAndroidApp(request: String): GeneratedProject {
        val activity = """
            package com.example.generated

            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.compose.foundation.layout.Arrangement
            import androidx.compose.foundation.layout.Column
            import androidx.compose.foundation.layout.fillMaxSize
            import androidx.compose.material3.MaterialTheme
            import androidx.compose.material3.Text
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Alignment
            import androidx.compose.ui.Modifier

            class MainActivity : ComponentActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)

                    setContent {
                        GeneratedApp()
                    }
                }
            }

            @Composable
            fun GeneratedApp() {
                MaterialTheme {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Generated Android App")
                        Text("$request")
                    }
                }
            }
        """.trimIndent()

        val manifest = """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application
                    android:allowBackup="true"
                    android:label="Generated App"
                    android:supportsRtl="true">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """.trimIndent()

        return GeneratedProject(
            name = "GeneratedAndroidApp",
            files = listOf(
                GeneratedFile("app/src/main/java/com/example/generated/MainActivity.kt", activity),
                GeneratedFile("app/src/main/AndroidManifest.xml", manifest),
                GeneratedFile("README.md", "# Generated Android App\n\nRequest:\n$request")
            )
        )
    }

    private fun generateCode(request: String): GeneratedProject {
        val code = """
            fun generatedFunction(): String {
                // Request:
                // $request

                return "Generated code placeholder"
            }
        """.trimIndent()

        return GeneratedProject(
            name = "GeneratedCode",
            files = listOf(
                GeneratedFile("Generated.kt", code),
                GeneratedFile("README.md", "# Generated Code\n\n$request")
            )
        )
    }

    private fun generateApi(request: String): GeneratedProject {
        val api = """
            package com.example.api

            /**
             * API design generated from:
             * $request
             *
             * This is a local starter contract.
             */
            data class ApiResponse(
                val success: Boolean,
                val message: String
            )

            fun handleRequest(): ApiResponse {
                return ApiResponse(
                    success = true,
                    message = "Implement authorized API logic here."
                )
            }
        """.trimIndent()

        return GeneratedProject(
            name = "GeneratedApi",
            files = listOf(
                GeneratedFile("Api.kt", api),
                GeneratedFile("README.md", "# Generated API\n\n$request")
            )
        )
    }

    private fun generateReview(request: String): GeneratedProject {
        val review = """
            SECURITY CODE REVIEW CHECKLIST

            Request / code under review:
            $request

            Check:
            1. Input validation
            2. Authentication
            3. Authorization
            4. Secret handling
            5. Network/TLS configuration
            6. Error handling
            7. Logging and sensitive data exposure
            8. Dependency/update risk
            9. File and storage permissions
            10. Test coverage

            Result:
            REVIEW PENDING — provide the actual source code to perform
            a concrete review.
        """.trimIndent()

        return GeneratedProject(
            name = "SecurityCodeReview",
            files = listOf(
                GeneratedFile("SECURITY_REVIEW.md", review)
            )
        )
    }

    private fun generateSecurityTests(request: String): GeneratedProject {
        val tests = """
            # Authorized Security Test Plan

            Target:
            $request

            ## Test categories

            - Authentication controls
            - Authorization / access control
            - Input validation
            - Session handling
            - TLS configuration
            - Security headers
            - Rate-limit behavior
            - Error handling
            - Sensitive information exposure
            - Logging and audit behavior

            ## Safety boundary

            Run these tests only against systems you own or are explicitly
            authorized to assess. Keep destructive, credential-harvesting,
            persistence, and denial-of-service tests out of this suite.

            ## Expected output

            Each test should record:
            - target
            - test name
            - evidence
            - pass/fail
            - severity
            - remediation
        """.trimIndent()

        return GeneratedProject(
            name = "AuthorizedSecurityTests",
            files = listOf(
                GeneratedFile("SECURITY_TEST_PLAN.md", tests)
            )
        )
    }

    private fun generateLab(request: String): GeneratedProject {
        val lab = """
            # NetScout Attack Lab Specification

            Lab objective:
            $request

            ## Isolation requirements

            - Use a local VM, emulator, container, or dedicated test device.
            - Do not use third-party production systems.
            - Keep the target isolated from unrelated networks.
            - Use fake credentials and synthetic data.

            ## Demonstration structure

            1. Start vulnerable lab service.
            2. Run NetScout reconnaissance.
            3. Show the discovered service.
            4. Demonstrate a controlled vulnerability.
            5. Capture evidence.
            6. Explain impact.
            7. Apply the fix.
            8. Re-run the test.
            9. Show the vulnerability is no longer reproduced.
        """.trimIndent()

        return GeneratedProject(
            name = "NetScoutAttackLab",
            files = listOf(
                GeneratedFile("LAB_PLAN.md", lab)
            )
        )
    }

    private fun generatePatch(request: String): GeneratedProject {
        val patch = """
            # Security Fix / Patch Plan

            Problem:
            $request

            ## Patch workflow

            1. Reproduce the issue on an authorized test target.
            2. Identify the root cause.
            3. Apply the smallest safe fix.
            4. Add a regression test.
            5. Re-run the security assessment.
            6. Confirm the finding is resolved.
        """.trimIndent()

        return GeneratedProject(
            name = "SecurityPatch",
            files = listOf(
                GeneratedFile("PATCH_PLAN.md", patch)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeLabScreen(
    request: String,
    mode: String,
    project: GeneratedProject?,
    onRequestChange: (String) -> Unit,
    onModeChange: (String) -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        "APP BUILDER",
        "CODE GENERATOR",
        "API BUILDER",
        "CODE REVIEW",
        "SECURITY TESTS",
        "LAB GENERATOR",
        "FIX / PATCH"
    )

    var showModes by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {
        item {
            Text(
                text = "🧠 CODE LAB",
                color = Color(0xFF39FF14),
                fontSize = 20.sp
            )

            Text(
                text = "Development, testing and authorized security engineering",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = request,
                onValueChange = onRequestChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),
                label = { Text("What do you want to build or test?") },
                placeholder = {
                    Text("Create an Android network monitoring app...")
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { showModes = !showModes },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF081108)
                )
            ) {
                Text("MODE: $mode")
            }

            if (showModes) {
                Spacer(modifier = Modifier.height(6.dp))

                modes.forEach { item ->
                    Button(
                        onClick = {
                            onModeChange(item)
                            showModes = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                if (item == mode)
                                    Color(0xFF1D661D)
                                else
                                    Color(0xFF123D12)
                        )
                    ) {
                        Text(item)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onGenerate,
                enabled = request.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF123D12)
                )
            ) {
                Text("GENERATE")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (project != null) {
            item {
                Text(
                    text = "GENERATED PROJECT",
                    color = Color(0xFF39FF14),
                    fontSize = 17.sp
                )

                Text(
                    text = project.name,
                    color = Color.White,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            items(project.files) { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF081108)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "📄 ${file.path}",
                            color = Color(0xFF39FF14),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = file.content,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
