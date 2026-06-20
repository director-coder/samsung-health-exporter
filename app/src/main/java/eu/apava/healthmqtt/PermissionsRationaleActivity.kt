package eu.apava.healthmqtt

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class PermissionsRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = TextView(this).apply {
            textSize = 18f
            setPadding(48, 48, 48, 48)
            text = getString(R.string.privacy_policy)
        }
        setContentView(view)
    }
}
