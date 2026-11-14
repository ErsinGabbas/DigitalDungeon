# R8 keep rules for the release build (isMinifyEnabled = true, isShrinkResources = true).
#
# Only rules this project actually needs live here. Everything that ships its own
# consumer rules is deliberately NOT duplicated below — see the notes per library.


# --- kotlinx.serialization ---------------------------------------------------------
# EventParticipant is the only @Serializable class in the app; its participants list is
# persisted as a JSON string in the `participants` TEXT columns. In R8 full mode the
# generated EventParticipant.$serializer is reachable only through Companion.serializer(),
# which R8 cannot see, so it gets shrunk away and every decode throws SerializationException.
# decodeParticipants() swallows that exception and returns an empty list, so the failure
# shows up as silently empty participant lists in release builds only — keep it explicit.
# Scoped to domain.model because that is the only package holding serializable models.
-keep,includedescriptorclasses class com.example.digitaldungeon.domain.model.**$$serializer { *; }
# Keeps the synthetic Companion field through which serializer() is looked up.
-keepclassmembers class com.example.digitaldungeon.domain.model.** {
    *** Companion;
}
# Keeps the generated serializer() factory itself on the model/companion classes.
-keepclasseswithmembers class com.example.digitaldungeon.domain.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}


# --- Enums persisted as TEXT -------------------------------------------------------
# Room stores SessionEvent.type and the eight quest enums in TEXT columns using the enum
# constant names, the Room migrations write those names as SQL literals, and the backup
# document carries them as plain text. Renaming the constants would make already stored
# rows unreadable, so pin the names.
# (values()/valueOf() are already kept for every enum by proguard-android-optimize.txt;
# these rules add the constant fields, which that default rule does not cover.)
-keepclassmembers enum com.example.digitaldungeon.domain.model.SessionEventType {
    <fields>;
}
-keepclassmembers enum com.example.digitaldungeon.domain.model.QuestCategory {
    <fields>;
}
-keepclassmembers enum com.example.digitaldungeon.domain.model.QuestStatus {
    <fields>;
}
-keepclassmembers enum com.example.digitaldungeon.domain.model.QuestPriority {
    <fields>;
}
-keepclassmembers enum com.example.digitaldungeon.domain.model.QuestObjectiveStatus {
    <fields>;
}
-keepclassmembers enum com.example.digitaldungeon.domain.model.QuestRewardType {
    <fields>;
}
-keepclassmembers enum com.example.digitaldungeon.domain.model.QuestConsequenceType {
    <fields>;
}
-keepclassmembers enum com.example.digitaldungeon.domain.model.QuestRelationType {
    <fields>;
}
-keepclassmembers enum com.example.digitaldungeon.domain.model.QuestHistoryEventType {
    <fields>;
}


# --- Room --------------------------------------------------------------------------
# No rules needed. room-runtime ships a consumer rule that keeps RoomDatabase subclasses
# (which covers the generated AppDatabase_Impl that Room loads via Class.forName), the DAO
# implementations are generated at compile time and referenced directly, and the migrations
# in Migrations.kt are plain objects reached through ALL_MIGRATIONS - nothing reflective.
# SupportSQLiteOpenHelper is instantiated through FrameworkSQLiteOpenHelperFactory by direct
# call, not by name, so it needs no keep either.
# Note: this relies on android.r8.strictFullModeForKeepRules=false (gradle.properties), which
# keeps the default constructor of classes matched by a -keep rule. Flipping that flag to
# true means AppDatabase_Impl's no-arg constructor must be kept here explicitly.


# --- Hilt / Dagger -------------------------------------------------------------------
# No rules needed: hilt-android ships its own consumer rules for the reflective
# EntryPoints casts, and everything else Hilt uses is generated at compile time.
# Do not copy generic Dagger keep rules in here.


# --- Crash reports --------------------------------------------------------------------
# Keep line numbers so obfuscated release stack traces stay usable, and drop the original
# file names (they add nothing once the class names are obfuscated).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
