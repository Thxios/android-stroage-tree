package com.thxios.storagetree.di

import com.thxios.storagetree.domain.repository.StorageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [StorageModule::class])
abstract class TestStorageModule {
    @Binds
    @Singleton
    abstract fun bindStorageRepository(impl: FakeStorageRepository): StorageRepository
}
