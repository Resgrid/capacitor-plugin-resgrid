package com.resgrid.plugins.resgrid.data

class PreferencesConfiguration : Cloneable {
    var group: String? = null
    @Throws(CloneNotSupportedException::class)
    public override fun clone(): PreferencesConfiguration {
        return super.clone() as PreferencesConfiguration
    }

    companion object {
        var DEFAULTS: PreferencesConfiguration? = null

        init {
            DEFAULTS = PreferencesConfiguration()
            DEFAULTS!!.group = "CapResgridStorage"
        }
    }
}