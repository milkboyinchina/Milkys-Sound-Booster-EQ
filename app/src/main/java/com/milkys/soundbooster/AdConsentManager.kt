package com.milkys.soundbooster

import android.app.Activity
import android.content.Context
import android.util.Log

object AdConsentManager {
    private const val TAG = "AdConsentManager"

    fun isUmpAvailable(): Boolean {
        return try {
            Class.forName("com.google.android.ump.UserMessagingPlatform")
            // verify ConsentInformation also exists (correct package is ump, not omp)
            Class.forName("com.google.android.ump.ConsentInformation")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun requestConsentInfoUpdate(activity: Activity, onComplete: () -> Unit = {}) {
        if (!isUmpAvailable()) {
            onComplete()
            return
        }

        try {
            val umpClass = Class.forName("com.google.android.ump.UserMessagingPlatform")
            val consentInfoClass = Class.forName("com.google.android.ump.ConsentInformation")
            val getConsentInfoMethod = umpClass.getMethod("getConsentInformation", Context::class.java)
            val consentInformation = getConsentInfoMethod.invoke(null, activity)

            val paramsBuilderClass = Class.forName("com.google.android.ump.ConsentRequestParameters\$Builder")
            val paramsBuilder = paramsBuilderClass.getDeclaredConstructor().newInstance()
            val buildMethod = paramsBuilderClass.getMethod("build")
            val params = buildMethod.invoke(paramsBuilder)

            val successListenerClass = Class.forName("com.google.android.ump.ConsentInformation\$OnConsentInfoUpdateSuccessListener")
            val failureListenerClass = try {
                Class.forName("com.google.android.ump.ConsentInformation\$OnConsentInfoUpdateFailureListener")
            } catch (e: Exception) {
                null
            }

            val listenerClass = Class.forName("com.google.android.ump.UserMessagingPlatform\$OnConsentFormDismissedListener")
            val proxyListener = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass)
            ) { _, _, _ ->
                onComplete()
                null
            }

            val loadAndShowMethod = umpClass.getMethod(
                "loadAndShowConsentFormIfRequired",
                Activity::class.java,
                listenerClass
            )

            val successProxy = java.lang.reflect.Proxy.newProxyInstance(
                successListenerClass.classLoader,
                arrayOf(successListenerClass)
            ) { _, _, _ ->
                try {
                    // UMP requires loadAndShow on main thread
                    activity.runOnUiThread {
                        try {
                            loadAndShowMethod.invoke(null, activity, proxyListener)
                        } catch (e: Exception) {
                            Log.d(TAG, "loadAndShow failed: ${e.message}")
                            onComplete()
                        }
                    }
                } catch (e: Exception) {
                    onComplete()
                }
                null
            }

            if (failureListenerClass != null) {
                val failureProxy = java.lang.reflect.Proxy.newProxyInstance(
                    failureListenerClass.classLoader,
                    arrayOf(failureListenerClass)
                ) { _, _, _ ->
                    onComplete()
                    null
                }

                val requestParamsClass = Class.forName("com.google.android.ump.ConsentRequestParameters")
                val requestMethod = try {
                    consentInfoClass.getMethod(
                        "requestConsentInfoUpdate",
                        requestParamsClass,
                        successListenerClass,
                        failureListenerClass
                    )
                } catch (e: NoSuchMethodException) {
                    // Fallback with Activity param for older lib variant
                    consentInfoClass.getMethod(
                        "requestConsentInfoUpdate",
                        Activity::class.java,
                        requestParamsClass,
                        successListenerClass,
                        failureListenerClass
                    )
                }
                try {
                    requestMethod.invoke(consentInformation, params, successProxy, failureProxy)
                } catch (e: Exception) {
                    // Try Activity-overload
                    val alt = consentInfoClass.getMethod(
                        "requestConsentInfoUpdate",
                        Activity::class.java,
                        requestParamsClass,
                        successListenerClass,
                        failureListenerClass
                    )
                    alt.invoke(consentInformation, activity, params, successProxy, failureProxy)
                }
            } else {
                onComplete()
            }

        } catch (e: Exception) {
            Log.d(TAG, "UMP consent update skipped or reflection failed: ${e.message}")
            onComplete()
        }
    }

    fun resetConsent(activity: Activity) {
        if (!isUmpAvailable()) return
        try {
            val umpClass = Class.forName("com.google.android.ump.UserMessagingPlatform")
            val consentInfoClass = Class.forName("com.google.android.ump.ConsentInformation")
            val getConsentInfoMethod = umpClass.getMethod("getConsentInformation", Context::class.java)
            val consentInformation = getConsentInfoMethod.invoke(null, activity)
            consentInfoClass.getMethod("reset").invoke(consentInformation)
        } catch (e: Exception) {
            Log.d(TAG, "UMP consent reset failed: ${e.message}")
        }
    }
}
