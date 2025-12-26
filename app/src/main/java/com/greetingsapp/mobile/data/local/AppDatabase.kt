package com.greetingsapp.mobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.greetingsapp.mobile.data.local.dao.FavoriteDao
import com.greetingsapp.mobile.data.local.entities.FavoriteEntity

// se define la configuracion: que tablas(entidades) y que version se incluiran en la bd
@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // se expone la clase DAO, para la persistencia
    abstract fun favoriteDao(): FavoriteDao;

    // la BD se declara como un Singleton
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null;

        fun getDatabase(context: Context): AppDatabase {

            //synchronized(this): Si dos hilos intentan pedir la base de datos al mismo tiempo, uno espera a que el otro termine
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "greetings_database" //nombre de la bd local
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}