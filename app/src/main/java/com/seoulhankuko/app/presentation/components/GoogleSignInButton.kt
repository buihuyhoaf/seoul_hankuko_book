package com.seoulhankuko.app.presentation.components

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.seoulhankuko.app.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.seoulhankuko.app.presentation.viewmodel.GoogleSignInViewModel
import com.seoulhankuko.app.core.Logger

/**
 * Google Sign-In button component with Material 3 design
 */
@Composable
fun GoogleSignInButton(
    onSignInSuccess: (String) -> Unit = {}, // Callback when sign-in is successful
    onSignInError: (String) -> Unit = {},   // Callback when sign-in fails
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    viewModel: GoogleSignInViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Get Google Sign-In client
    val googleSignInClient = remember { viewModel.getGoogleSignInClient(context) }

    // Activity result launcher for Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            
            Logger.GoogleSignIn.tokenReceived()
            viewModel.handleGoogleSignInResult(account)
        } catch (e: ApiException) {
            val errorMsg = "Google Sign-In failed: ${e.statusCode}"
            Logger.GoogleSignIn.signInError(errorMsg)
            onSignInError(errorMsg)
            errorMessage = errorMsg
        }
    }

    // Observe ViewModel state
    LaunchedEffect(viewModel.googleSignInState) {
        viewModel.googleSignInState.collect { result ->
            result?.let {
                when (it) {
                    is com.seoulhankuko.app.data.repository.GoogleSignInResult.Loading -> {
                        isLoading = true
                        errorMessage = null
                    }
                    is com.seoulhankuko.app.data.repository.GoogleSignInResult.Success -> {
                        isLoading = false
                        errorMessage = null
                        onSignInSuccess(it.userInfo.email)
                    }
                    is com.seoulhankuko.app.data.repository.GoogleSignInResult.Error -> {
                        isLoading = false
                        errorMessage = it.message
                        onSignInError(it.message)
                    }
                }
            }
        }
    }


    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Google Sign-In Button
        Button(
            onClick = {
                if (!isLoading && enabled) {
                    Logger.GoogleSignIn.signInAttempt("user@example.com") // Will be updated with actual email
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                }
            },
            enabled = enabled && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.outline
                )
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = if (isLoading) "Đang đăng nhập..." else "Đăng nhập bằng Google",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Error message
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Facebook Sign-In button component (placeholder for future implementation)
 */
@Composable
fun FacebookSignInButton(
    onSignInSuccess: (String) -> Unit = {},
    onSignInError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (!isLoading && enabled) {
                isLoading = true
                // TODO: Implement Facebook Sign-In
                // For now, simulate a failed attempt
                onSignInError("Facebook Sign-In chưa được triển khai")
                isLoading = false
            }
        },
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outline
            )
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Text(
            text = if (isLoading) "Đang đăng nhập..." else "Đăng nhập bằng Facebook",
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Social Sign-In section with both Google and Facebook buttons
 */
@Composable
fun SocialSignInSection(
    onGoogleSignInSuccess: (String) -> Unit = {},
    onGoogleSignInError: (String) -> Unit = {},
    onFacebookSignInSuccess: (String) -> Unit = {},
    onFacebookSignInError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Divider with "Hoặc" text
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = stringResource(R.string.or_text),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Google Sign-In Button
        GoogleSignInButton(
            onSignInSuccess = onGoogleSignInSuccess,
            onSignInError = onGoogleSignInError,
            enabled = enabled
        )

        // Facebook Sign-In Button
        FacebookSignInButton(
            onSignInSuccess = onFacebookSignInSuccess,
            onSignInError = onFacebookSignInError,
            enabled = enabled
        )
    }
}

