plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(plugin(libs.plugins.errorprone))
    implementation(plugin(libs.plugins.kover))

    compileOnly(plugin(libs.plugins.kotlin.jvm))
}

fun plugin(plugin: Provider<PluginDependency>): Provider<String> =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
