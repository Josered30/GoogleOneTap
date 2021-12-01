package com.example.googleonetap.di

import android.content.Context
import com.example.googleonetap.auth.AuthManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun providesAuthManager(@ApplicationContext context: Context): AuthManager {
        return AuthManager(context)
    }
}