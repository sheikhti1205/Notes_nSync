package com.notesnync.app.data

import android.net.Uri
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

object GoogleDriveOAuth {
    const val ClientId = "1092049010490-dghvvj5tmu5urr41pjqpufafa1ilga25.apps.googleusercontent.com"
    const val RedirectScheme = "com.googleusercontent.apps.1092049010490-dghvvj5tmu5urr41pjqpufafa1ilga25"
    const val RedirectUri = "$RedirectScheme:/oauth2redirect"
    const val DriveAppDataScope = "https://www.googleapis.com/auth/drive.appdata"
    private const val IdentityScopes = "openid email profile"

    private val serviceConfiguration = AuthorizationServiceConfiguration(
        Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
        Uri.parse("https://oauth2.googleapis.com/token"),
    )

    fun authorizationRequest(): AuthorizationRequest =
        AuthorizationRequest.Builder(
            serviceConfiguration,
            ClientId,
            ResponseTypeValues.CODE,
            Uri.parse(RedirectUri),
        )
            .setScope("$IdentityScopes $DriveAppDataScope")
            .setPrompt("consent")
            .setAdditionalParameters(mapOf("access_type" to "offline"))
            .build()
}
