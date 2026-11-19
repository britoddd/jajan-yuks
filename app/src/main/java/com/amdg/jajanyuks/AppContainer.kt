package com.amdg.jajanyuks

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

// Manual dependency injection: one place that creates shared objects.
// Repositories will be added here as features are built.
class AppContainer {
    val supabase = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}
