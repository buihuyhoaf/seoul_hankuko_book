package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.seoulhankuko.app.presentation.ui.theme.WhiteBackground
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.R
import com.seoulhankuko.app.presentation.components.SocialSignInSection
import com.seoulhankuko.app.presentation.viewmodel.AuthViewModel
import com.seoulhankuko.app.core.Logger
import com.seoulhankuko.app.domain.model.AuthState.SignedIn
import com.seoulhankuko.app.domain.model.AuthState.SignedOut

@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLearn: (courseId: Int) -> Unit,
    initialEmail: String = "",
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val loginErrorText = stringResource(R.string.login_error)
    val validationEmptyFieldsText = stringResource(R.string.validation_empty_fields)
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is SignedIn -> {
                Logger.LoginScreen.signInSuccess()
                onNavigateToLearn(1)
            }
            else -> {}
        }
        
        // Reset loading state when auth state changes
        isLoading = false
        if (authState is SignedOut && email.isNotEmpty()) {
            errorMessage = loginErrorText
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = WhiteBackground // Use pure white background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.login_screen_padding)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.login_screen_large_spacing))
        ) {
            // Top spacing for better visual balance
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.login_screen_large_spacing)))
            
            // Header section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
            ) {
                Text(
                    text = stringResource(R.string.welcome_back),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1B5E20) // Dark green for title on white background
                )
                
                Text(
                    text = stringResource(R.string.sign_in_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2E7D32), // Medium green for subtitle
                    textAlign = TextAlign.Center
                )
            }
            
            // Form section
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.login_field_spacing)),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50), // Green border when focused
                        unfocusedBorderColor = Color(0xFFE0E0E0), // Light gray border when unfocused
                        focusedTextColor = Color(0xFF1B5E20), // Dark green text
                        unfocusedTextColor = Color(0xFF2E7D32) // Medium green text
                    )
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50), // Green border when focused
                        unfocusedBorderColor = Color(0xFFE0E0E0), // Light gray border when unfocused
                        focusedTextColor = Color(0xFF1B5E20), // Dark green text
                        unfocusedTextColor = Color(0xFF2E7D32) // Medium green text
                    )
                )
                
                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFD32F2F), // Red color for error message on white background
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_medium))
                    )
                }
                
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                
                // Sign In button
                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            Logger.LoginScreen.signInButtonClicked(email)
                            isLoading = true
                            errorMessage = null
                            viewModel.signIn(email, password)
                        } else {
                            Logger.LoginScreen.validationFailed("Empty fields")
                            errorMessage = validationEmptyFieldsText
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.login_button_height)),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50), // Green button background
                        contentColor = Color.White, // White text on green button
                        disabledContainerColor = Color(0xFFE0E0E0), // Light gray when disabled
                        disabledContentColor = Color(0xFF9E9E9E) // Gray text when disabled
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(dimensionResource(R.dimen.login_progress_indicator_size)),
                            color = Color.White, // White progress indicator on green background
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.sign_in_button),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White // White text on green button
                        )
                    }
                }
            }
            
            // Social Sign-In Section
            SocialSignInSection(
                onGoogleSignInSuccess = { email ->
                    Logger.LoginScreen.signInSuccess()
                    onNavigateToLearn(1)
                },
                onGoogleSignInError = { error ->
                    errorMessage = error
                    Logger.LoginScreen.signInFailed(error)
                },
                onFacebookSignInSuccess = { email ->
                    // TODO: Handle Facebook sign-in success
                    onNavigateToLearn(1)
                },
                onFacebookSignInError = { error ->
                    errorMessage = error
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.login_screen_spacing))
            ) {
                // Back button
                TextButton(
                    onClick = {
                        Logger.LoginScreen.backButtonClicked()
                        onNavigateBack()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.back_to_home),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50) // Green for clickable text
                    )
                }
                
                // Sign up link
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dont_have_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32) // Medium green for regular text
                    )
                    TextButton(
                        onClick = { /* TODO: Navigate to sign up */ }
                    ) {
                        Text(
                            text = stringResource(R.string.sign_up),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4CAF50) // Green for clickable text
                        )
                    }
                }
                
                // Demo credentials hint
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F9FA) // Light gray background for card
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(dimensionResource(R.dimen.login_card_padding))
                    ) {
                        Text(
                            text = stringResource(R.string.demo_credentials),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20) // Dark green for card title
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                        Text(
                            text = stringResource(R.string.demo_credentials_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32) // Medium green for card content
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            onNavigateBack = {},
            onNavigateToLearn = {}
        )
    }
}
