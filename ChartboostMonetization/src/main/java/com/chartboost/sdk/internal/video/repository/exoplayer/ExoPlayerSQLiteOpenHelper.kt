package com.chartboost.sdk.internal.video.repository.exoplayer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteOpenHelper

private const val CHARTBOOST_EXOPLAYER_DATABASE_NAME: String = "chartboost_exoplayer.db"

// Needed to be able to change the default database name.
internal class ExoPlayerSQLiteOpenHelper(
    context: Context,
    name: String = CHARTBOOST_EXOPLAYER_DATABASE_NAME,
    factory: CursorFactory? = null,
    version: Int = 1,
) : SQLiteOpenHelper(
        context,
        name,
        factory,
        version,
    ) {
    override fun onCreate(db: SQLiteDatabase?) {
        // Nothing to do
    }

    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int,
    ) {
        // Nothing to do
    }
}
